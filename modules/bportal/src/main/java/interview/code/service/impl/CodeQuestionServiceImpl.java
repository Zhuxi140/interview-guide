package interview.code.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.api.cportal.InterviewSessionParticipationApi;
import interview.api.cportal.dto.InterviewSessionParticipantDTO;
import interview.code.mapper.CodeQuestionMapper;
import interview.code.mapper.CodeTestCaseMapper;
import interview.code.model.entity.CodeQuestion;
import interview.code.model.entity.CodeTestCase;
import interview.code.model.enums.CodeVisibility;
import interview.code.model.req.AdminCodeQuestionSearchReq;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.req.EnterpriseCodeQuestionSearchReq;
import interview.code.model.vo.*;
import interview.code.service.CodeQuestionService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 编程题库服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeQuestionServiceImpl extends ServiceImpl<CodeQuestionMapper, CodeQuestion>
        implements CodeQuestionService {

    private final CodeTestCaseMapper codeTestCaseMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final InterviewSessionParticipationApi sessionParticipationApi;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeQuestionCreateVO createPlatformQuestion(CodeQuestionCreateReq req) {
        // 平台全局题：可见性固定 GLOBAL，不归属任何企业租户。
        CodeQuestion question = buildQuestion(CodeVisibility.GLOBAL, null, req);
        save(question);
        return toCreateVO(question);
    }

    @Override
    public IPage<CodeQuestionListItemVO> pagePlatformQuestions(AdminCodeQuestionSearchReq req) {
        // 平台题库只返回 GLOBAL 题，按创建时间倒序。
        LambdaQueryWrapper<CodeQuestion> wrapper = new LambdaQueryWrapper<CodeQuestion>()
                .select(CodeQuestion::getId, CodeQuestion::getTitle,
                        CodeQuestion::getVisibility, CodeQuestion::getTimeLimitMs,
                        CodeQuestion::getMemoryLimitMb, CodeQuestion::getVersion,
                        CodeQuestion::getCreatedAt)
                .eq(CodeQuestion::getVisibility, CodeVisibility.GLOBAL)
                .like(StrUtil.isNotBlank(req.getKeyword()),
                        CodeQuestion::getTitle, req.getKeyword())
                .orderByDesc(CodeQuestion::getId);
        Page<CodeQuestion> questionPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        return toListItemPage(questionPage);
    }

    @Override
    public CodeQuestionDetailVO getPlatformQuestionDetail(Long questionId) {
        // 校验题目存在且为全局题。
        CodeQuestion question = getRequiredQuestion(questionId);
        if (question.getVisibility() != CodeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
        return toDetailVO(question);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeQuestionUpdateVO updatePlatformQuestion(Long questionId, CodeQuestionUpdateReq req) {
        // 校验题目存在且为全局题。
        CodeQuestion existing = getRequiredQuestion(questionId);
        if (existing.getVisibility() != CodeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }

        // 半量更新：仅覆盖请求中出现的字段，乐观锁由 version 条件保证。
        CodeQuestion update = buildQuestionUpdate(questionId, req);
        int affected = baseMapper.update(update,
                Wrappers.<CodeQuestion>lambdaUpdate().eq(CodeQuestion::getId, questionId));
        if (affected == 0) {
            throw versionConflict();
        }
        return new CodeQuestionUpdateVO(questionId, req.getExpectedVersion() + 1,
                OffsetDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deletePlatformQuestion(Long questionId, Integer expectedVersion) {
        // 校验题目存在且为全局题。
        CodeQuestion existing = getRequiredQuestion(questionId);
        if (existing.getVisibility() != CodeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        CodeQuestion update = new CodeQuestion();
        update.setId(questionId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<CodeQuestion>lambdaUpdate()
                        .eq(CodeQuestion::getId, questionId)
                        .set(CodeQuestion::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeQuestionCreateVO createEnterpriseQuestion(Long enterpriseId, CodeQuestionCreateReq req) {
        // 企业私有题：校验当前用户属于该企业，可见性与租户由服务端确定。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        CodeQuestion question = buildQuestion(CodeVisibility.PRIVATE, enterpriseId, req);
        save(question);
        return toCreateVO(question);
    }

    @Override
    public IPage<CodeQuestionListItemVO> pageEnterpriseQuestions(
            Long enterpriseId, EnterpriseCodeQuestionSearchReq req) {
        // 企业可见集合 = 本企业私有题 + 平台全局题；visibility 为空时两者都返回。
        LambdaQueryWrapper<CodeQuestion> wrapper = new LambdaQueryWrapper<CodeQuestion>()
                .select(CodeQuestion::getId, CodeQuestion::getTitle,
                        CodeQuestion::getVisibility, CodeQuestion::getTimeLimitMs,
                        CodeQuestion::getMemoryLimitMb, CodeQuestion::getVersion,
                        CodeQuestion::getCreatedAt)
                .and(w -> w.eq(CodeQuestion::getVisibility, CodeVisibility.GLOBAL)
                        .or(sub -> sub.eq(CodeQuestion::getVisibility, CodeVisibility.PRIVATE)
                                .eq(CodeQuestion::getEnterpriseId, enterpriseId)))
                .eq(req.getVisibility() != null, CodeQuestion::getVisibility, req.getVisibility())
                .like(StrUtil.isNotBlank(req.getKeyword()),
                        CodeQuestion::getTitle, req.getKeyword())
                .orderByDesc(CodeQuestion::getId);
        Page<CodeQuestion> questionPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        return toListItemPage(questionPage);
    }

    @Override
    public CodeQuestionDetailVO getEnterpriseQuestionDetail(Long enterpriseId, Long questionId) {
        // 校验题目存在且企业有权读取（全局题或本企业私有题）。
        CodeQuestion question = getRequiredQuestion(questionId);
        if (!isReadableByEnterprise(question, enterpriseId)) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
        return toDetailVO(question);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeQuestionUpdateVO updateEnterpriseQuestion(
            Long enterpriseId, Long questionId, CodeQuestionUpdateReq req) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 全局题只能由平台管理端维护，企业只能更新本企业私有题。
        CodeQuestion existing = getRequiredQuestion(questionId);
        if (!isOwnedByEnterprise(existing, enterpriseId)) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }

        // 半量更新：仅覆盖请求中出现的字段，乐观锁由 version 条件保证。
        CodeQuestion update = buildQuestionUpdate(questionId, req);
        update.setEnterpriseId(enterpriseId);
        int affected = baseMapper.update(update,
                Wrappers.<CodeQuestion>lambdaUpdate()
                        .eq(CodeQuestion::getId, questionId)
                        .eq(CodeQuestion::getEnterpriseId, enterpriseId));
        if (affected == 0) {
            throw versionConflict();
        }
        return new CodeQuestionUpdateVO(questionId, req.getExpectedVersion() + 1,
                OffsetDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteEnterpriseQuestion(Long enterpriseId, Long questionId, Integer expectedVersion) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 全局题只能由平台管理端维护，企业只能删除本企业私有题。
        CodeQuestion existing = getRequiredQuestion(questionId);
        if (!isOwnedByEnterprise(existing, enterpriseId)) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        CodeQuestion update = new CodeQuestion();
        update.setId(questionId);
        update.setEnterpriseId(enterpriseId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<CodeQuestion>lambdaUpdate()
                        .eq(CodeQuestion::getId, questionId)
                        .eq(CodeQuestion::getEnterpriseId, enterpriseId)
                        .set(CodeQuestion::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }
    }

    @Override
    public SessionCodeQuestionVO getSessionQuestion(Long sessionId, Long questionId) {
        // 通过跨模块只读 API 校验会话归属：仅候选人本人可读取。
        InterviewSessionParticipantDTO participant =
                sessionParticipationApi.getSessionParticipant(sessionId);
        if (participant == null
                || !participant.candidateUserId().equals(AuthContext.getRequiredUserId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }

        // TODO [Phase6] 题目分配器未实现（无会话-题目分配表）：
        //  当前按“会话租户可读题库”最简放行（GLOBAL 或会话所属企业的 PRIVATE 题），
        //  分配器落地后应改为校验题目确实分配给该会话。
        CodeQuestion question = getRequiredQuestion(questionId);
        if (participant.enterpriseId() != null
                && !isReadableByEnterprise(question, participant.enterpriseId())) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }

        // 只返回公开示例用例；隐藏黑盒用例的输入与预期输出不得下发。
        List<CodeTestCase> publicCases = codeTestCaseMapper.selectList(
                Wrappers.<CodeTestCase>lambdaQuery()
                        .select(CodeTestCase::getInputCase, CodeTestCase::getExpectedOutput)
                        .eq(CodeTestCase::getQuestionId, questionId)
                        .eq(CodeTestCase::getIsSecret, false)
                        .orderByAsc(CodeTestCase::getId));
        List<CodePublicExampleVO> publicExamples = publicCases.stream()
                .map(testCase -> new CodePublicExampleVO(
                        testCase.getInputCase(), testCase.getExpectedOutput()))
                .toList();

        // TODO [Phase6] 提交次数上限（attemptLimit）配置未定义，分配器接入前返回 null。
        return SessionCodeQuestionVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .timeLimitMs(question.getTimeLimitMs())
                .memoryLimitMb(question.getMemoryLimitMb())
                .supportedLanguages(readLanguages(question.getSupportedLanguagesJson()))
                .publicExamples(publicExamples)
                .attemptLimit(null)
                .build();
    }

    @Override
    public Map<Long, String> getQuestionTitlesByIds(List<Long> questionIds) {
        // 单表 IN 查询补齐标题；questionIds 为空时直接返回，避免不必要的查询。
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return lambdaQuery()
                .select(CodeQuestion::getId, CodeQuestion::getTitle)
                .in(CodeQuestion::getId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        CodeQuestion::getId,
                        CodeQuestion::getTitle,
                        (left, right) -> left));
    }

    private CodeQuestion buildQuestion(CodeVisibility visibility, Long enterpriseId,
                                       CodeQuestionCreateReq req) {
        return CodeQuestion.builder()
                .visibility(visibility)
                .enterpriseId(enterpriseId)
                .title(req.getTitle())
                .description(req.getDescription())
                .timeLimitMs(req.getTimeLimitMs())
                .memoryLimitMb(req.getMemoryLimitMb())
                .supportedLanguagesJson(writeLanguages(req.getSupportedLanguages()))
                .version(0)
                .build();
    }

    private CodeQuestion buildQuestionUpdate(Long questionId, CodeQuestionUpdateReq req) {
        // 仅覆盖请求中出现的字段；updatedBy/updatedAt/traceId 由自动填充维护。
        CodeQuestion update = new CodeQuestion();
        update.setId(questionId);
        if (StrUtil.isNotBlank(req.getTitle())) {
            update.setTitle(req.getTitle());
        }
        if (StrUtil.isNotBlank(req.getDescription())) {
            update.setDescription(req.getDescription());
        }
        if (req.getTimeLimitMs() != null) {
            update.setTimeLimitMs(req.getTimeLimitMs());
        }
        if (req.getMemoryLimitMb() != null) {
            update.setMemoryLimitMb(req.getMemoryLimitMb());
        }
        if (req.getSupportedLanguages() != null) {
            update.setSupportedLanguagesJson(writeLanguages(req.getSupportedLanguages()));
        }
        update.setVersion(req.getExpectedVersion());
        return update;
    }

    private CodeQuestion getRequiredQuestion(Long questionId) {
        CodeQuestion question = lambdaQuery()
                .eq(CodeQuestion::getId, questionId)
                .one();
        if (question == null) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
        return question;
    }

    private boolean isReadableByEnterprise(CodeQuestion question, Long enterpriseId) {
        return question.getVisibility() == CodeVisibility.GLOBAL
                || isOwnedByEnterprise(question, enterpriseId);
    }

    private boolean isOwnedByEnterprise(CodeQuestion question, Long enterpriseId) {
        return question.getVisibility() == CodeVisibility.PRIVATE
                && enterpriseId.equals(question.getEnterpriseId());
    }

    private IPage<CodeQuestionListItemVO> toListItemPage(Page<CodeQuestion> questionPage) {
        Page<CodeQuestionListItemVO> voPage = new Page<>(
                questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        voPage.setRecords(questionPage.getRecords().stream()
                .map(question -> CodeQuestionListItemVO.builder()
                        .id(question.getId())
                        .title(question.getTitle())
                        .visibility(question.getVisibility())
                        .timeLimitMs(question.getTimeLimitMs())
                        .memoryLimitMb(question.getMemoryLimitMb())
                        .version(question.getVersion())
                        .createdAt(question.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    private CodeQuestionCreateVO toCreateVO(CodeQuestion question) {
        return CodeQuestionCreateVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .visibility(question.getVisibility())
                .version(question.getVersion())
                .createdAt(question.getCreatedAt())
                .build();
    }

    private CodeQuestionDetailVO toDetailVO(CodeQuestion question) {
        // 管理端详情返回全部测试用例（含隐藏用例）。
        List<CodeTestCase> testCases = codeTestCaseMapper.selectList(
                Wrappers.<CodeTestCase>lambdaQuery()
                        .eq(CodeTestCase::getQuestionId, question.getId())
                        .orderByAsc(CodeTestCase::getId));
        return CodeQuestionDetailVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .visibility(question.getVisibility())
                .timeLimitMs(question.getTimeLimitMs())
                .memoryLimitMb(question.getMemoryLimitMb())
                .supportedLanguages(readLanguages(question.getSupportedLanguagesJson()))
                .version(question.getVersion())
                .testCases(testCases.stream()
                        .map(testCase -> CodeTestCaseItemVO.builder()
                                .id(testCase.getId())
                                .inputCase(testCase.getInputCase())
                                .expectedOutput(testCase.getExpectedOutput())
                                .isSecret(testCase.getIsSecret())
                                .version(testCase.getVersion())
                                .build())
                        .toList())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private String writeLanguages(List<String> languages) {
        if (languages == null) {
            return null;
        }
        return objectMapper.writeValueAsString(languages);
    }

    private List<String> readLanguages(String languagesJson) {
        if (StrUtil.isBlank(languagesJson)) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(languagesJson, String[].class));
        } catch (Exception e) {
            log.error("编程题语言 JSON 解析失败: {}", languagesJson, e);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private BusinessException versionConflict() {
        // 80xxx 暂无编程题专用版本冲突错误码，使用参数校验错误并携带提示信息。
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "题目已被其他请求修改，请刷新后重试");
    }
}
