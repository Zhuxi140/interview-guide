package interview.code.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.code.mapper.CodeQuestionMapper;
import interview.code.mapper.CodeTestCaseMapper;
import interview.code.model.entity.CodeQuestion;
import interview.code.model.entity.CodeTestCase;
import interview.code.model.enums.CodeVisibility;
import interview.code.model.req.CodeTestCaseCreateReq;
import interview.code.model.req.CodeTestCaseUpdateReq;
import interview.code.model.vo.CodeTestCaseCreateVO;
import interview.code.model.vo.CodeTestCaseUpdateVO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 编程题测试用例服务实现。
 */
@Service
@RequiredArgsConstructor
public class CodeTestCaseServiceImpl extends ServiceImpl<CodeTestCaseMapper, CodeTestCase>
        implements CodeTestCaseService {

    private final CodeQuestionMapper codeQuestionMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeTestCaseCreateVO createPlatformTestCase(Long questionId, CodeTestCaseCreateReq req) {
        // 平台侧仅允许为全局题维护用例。
        requirePlatformQuestion(questionId);
        return insertTestCase(questionId, req);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeTestCaseUpdateVO updatePlatformTestCase(
            Long questionId, Long caseId, CodeTestCaseUpdateReq req) {
        // 平台侧仅允许为全局题维护用例。
        requirePlatformQuestion(questionId);
        requireTestCase(questionId, caseId);
        return updateTestCase(caseId, req);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deletePlatformTestCase(Long questionId, Long caseId, Integer expectedVersion) {
        // 平台侧仅允许为全局题维护用例。
        requirePlatformQuestion(questionId);
        requireTestCase(questionId, caseId);
        deleteTestCase(questionId, caseId, expectedVersion);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeTestCaseCreateVO createEnterpriseTestCase(
            Long enterpriseId, Long questionId, CodeTestCaseCreateReq req) {
        // 企业侧仅允许为本企业私有题维护用例。
        requireEnterpriseQuestion(enterpriseId, questionId);
        return insertTestCase(questionId, req);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public CodeTestCaseUpdateVO updateEnterpriseTestCase(
            Long enterpriseId, Long questionId, Long caseId, CodeTestCaseUpdateReq req) {
        // 企业侧仅允许为本企业私有题维护用例。
        requireEnterpriseQuestion(enterpriseId, questionId);
        requireTestCase(questionId, caseId);
        return updateTestCase(caseId, req);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteEnterpriseTestCase(
            Long enterpriseId, Long questionId, Long caseId, Integer expectedVersion) {
        // 企业侧仅允许为本企业私有题维护用例。
        requireEnterpriseQuestion(enterpriseId, questionId);
        requireTestCase(questionId, caseId);
        deleteTestCase(questionId, caseId, expectedVersion);
    }

    private CodeTestCaseCreateVO insertTestCase(Long questionId, CodeTestCaseCreateReq req) {
        // 新增用例：isSecret 未传时默认公开用例。
        CodeTestCase testCase = CodeTestCase.builder()
                .questionId(questionId)
                .inputCase(req.getInputCase())
                .expectedOutput(req.getExpectedOutput())
                .isSecret(Boolean.TRUE.equals(req.getIsSecret()))
                .version(0)
                .build();
        save(testCase);
        return CodeTestCaseCreateVO.builder()
                .id(testCase.getId())
                .questionId(questionId)
                .isSecret(testCase.getIsSecret())
                .version(testCase.getVersion())
                .build();
    }

    private CodeTestCaseUpdateVO updateTestCase(Long caseId, CodeTestCaseUpdateReq req) {
        // 半量更新：仅覆盖请求中出现的字段，乐观锁由 version 条件保证。
        CodeTestCase update = new CodeTestCase();
        update.setId(caseId);
        if (StrUtil.isNotBlank(req.getInputCase())) {
            update.setInputCase(req.getInputCase());
        }
        if (StrUtil.isNotBlank(req.getExpectedOutput())) {
            update.setExpectedOutput(req.getExpectedOutput());
        }
        if (req.getIsSecret() != null) {
            update.setIsSecret(req.getIsSecret());
        }
        update.setVersion(req.getExpectedVersion());
        int affected = baseMapper.update(update,
                Wrappers.<CodeTestCase>lambdaUpdate().eq(CodeTestCase::getId, caseId));
        if (affected == 0) {
            throw versionConflict();
        }
        return new CodeTestCaseUpdateVO(caseId, req.getExpectedVersion() + 1,
                OffsetDateTime.now());
    }

    private void deleteTestCase(Long questionId, Long caseId, Integer expectedVersion) {
        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        CodeTestCase update = new CodeTestCase();
        update.setId(caseId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<CodeTestCase>lambdaUpdate()
                        .eq(CodeTestCase::getId, caseId)
                        .eq(CodeTestCase::getQuestionId, questionId)
                        .set(CodeTestCase::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }
    }

    private void requirePlatformQuestion(Long questionId) {
        // 校验题目存在且为全局题。
        CodeQuestion question = getRequiredQuestion(questionId);
        if (question.getVisibility() != CodeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
    }

    private void requireEnterpriseQuestion(Long enterpriseId, Long questionId) {
        // 校验当前用户属于该企业，且题目为本企业私有题。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        CodeQuestion question = getRequiredQuestion(questionId);
        if (question.getVisibility() != CodeVisibility.PRIVATE
                || !enterpriseId.equals(question.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
    }

    private CodeQuestion getRequiredQuestion(Long questionId) {
        CodeQuestion question = codeQuestionMapper.selectOne(
                Wrappers.<CodeQuestion>lambdaQuery()
                        .select(CodeQuestion::getId, CodeQuestion::getVisibility,
                                CodeQuestion::getEnterpriseId)
                        .eq(CodeQuestion::getId, questionId));
        if (question == null) {
            throw new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND);
        }
        return question;
    }

    private CodeTestCase requireTestCase(Long questionId, Long caseId) {
        // 校验用例存在且归属指定题目。
        CodeTestCase testCase = lambdaQuery()
                .eq(CodeTestCase::getId, caseId)
                .eq(CodeTestCase::getQuestionId, questionId)
                .one();
        if (testCase == null) {
            throw new BusinessException(ErrorCode.CODE_TEST_CASE_NOT_FOUND);
        }
        return testCase;
    }

    private BusinessException versionConflict() {
        // 80xxx 暂无测试用例专用版本冲突错误码，使用参数校验错误并携带提示信息。
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "测试用例已被其他请求修改，请刷新后重试");
    }
}
