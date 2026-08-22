package interview.compliance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.compliance.mapper.SensitiveWordMapper;
import interview.compliance.model.entity.SensitiveWord;
import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.req.SensitiveWordCreateReq;
import interview.compliance.model.req.SensitiveWordSearchReq;
import interview.compliance.model.req.SensitiveWordUpdateReq;
import interview.compliance.model.vo.SensitiveWordCreateVO;
import interview.compliance.model.vo.SensitiveWordListItemVO;
import interview.compliance.model.vo.SensitiveWordUpdateVO;
import interview.compliance.service.SensitiveWordService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuxi
 * @apiNote 平台端敏感词库管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord>
        implements SensitiveWordService {

    @Override
    public IPage<SensitiveWordListItemVO> pageSensitiveWords(SensitiveWordSearchReq req) {
        // 单表 LambdaQuery：按类别筛选、创建时间倒序。
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<SensitiveWord>()
                .eq(req.getCategory() != null, SensitiveWord::getCategory, req.getCategory())
                .orderByDesc(SensitiveWord::getId);
        Page<SensitiveWord> wordPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<SensitiveWordListItemVO> voPage = new Page<>(
                wordPage.getCurrent(), wordPage.getSize(), wordPage.getTotal());
        voPage.setRecords(wordPage.getRecords().stream()
                .map(word -> SensitiveWordListItemVO.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .category(word.getCategory())
                        .actionType(word.getActionType())
                        .version(word.getVersion())
                        .createdAt(word.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public SensitiveWordCreateVO createSensitiveWord(SensitiveWordCreateReq req) {
        // 词本体唯一：先查后插快速失败，唯一索引兜底并发插入。
        boolean exists = lambdaQuery()
                .eq(SensitiveWord::getWord, req.getWord())
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "敏感词已存在");
        }

        // 触发动作缺省 BLOCK；updatedBy/traceId 由自动填充维护。
        SensitiveWord word = SensitiveWord.builder()
                .word(req.getWord())
                .category(req.getCategory())
                .actionType(req.getActionType() != null ? req.getActionType() : SensitiveAction.BLOCK)
                .version(0)
                .build();
        save(word);

        // TODO [Phase8] 敏感词过滤引擎（DFA/AC 自动机缓存）接入后，此处需发布词库变更事件刷新缓存。
        return SensitiveWordCreateVO.builder()
                .id(word.getId())
                .word(word.getWord())
                .category(word.getCategory())
                .actionType(word.getActionType())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public SensitiveWordUpdateVO updateSensitiveWord(Long id, SensitiveWordUpdateReq req) {
        // 校验词存在，并基于旧值合并半量更新字段。
        SensitiveWord existing = getRequiredWord(id);
        String newWord = StrUtil.isNotBlank(req.getWord()) ? req.getWord() : existing.getWord();
        if (!newWord.equals(existing.getWord())) {
            boolean duplicated = lambdaQuery()
                    .eq(SensitiveWord::getWord, newWord)
                    .ne(SensitiveWord::getId, id)
                    .exists();
            if (duplicated) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "敏感词已存在");
            }
        }

        // 半量更新：仅覆盖请求中出现的字段，乐观锁由 version 条件保证。
        SensitiveWord update = new SensitiveWord();
        update.setId(id);
        if (StrUtil.isNotBlank(req.getWord())) {
            update.setWord(req.getWord());
        }
        if (req.getCategory() != null) {
            update.setCategory(req.getCategory());
        }
        if (req.getActionType() != null) {
            update.setActionType(req.getActionType());
        }
        update.setVersion(req.getExpectedVersion());
        int affected = baseMapper.update(update,
                Wrappers.<SensitiveWord>lambdaUpdate().eq(SensitiveWord::getId, id));
        if (affected == 0) {
            throw versionConflict();
        }
        return SensitiveWordUpdateVO.builder()
                .id(id)
                .word(newWord)
                .category(req.getCategory() != null ? req.getCategory() : existing.getCategory())
                .actionType(req.getActionType() != null
                        ? req.getActionType() : existing.getActionType())
                .version(req.getExpectedVersion() + 1)
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteSensitiveWord(Long id, Integer expectedVersion) {
        // 校验词存在。
        getRequiredWord(id);

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        SensitiveWord update = new SensitiveWord();
        update.setId(id);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<SensitiveWord>lambdaUpdate()
                        .eq(SensitiveWord::getId, id)
                        .set(SensitiveWord::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }

        // TODO [Phase8] 敏感词过滤引擎接入后，删除同样需发布词库变更事件刷新缓存。
    }

    /**
     * 加载必须存在的敏感词
     * @param id 敏感词 ID
     * @return 敏感词记录
     */
    private SensitiveWord getRequiredWord(Long id) {
        SensitiveWord word = lambdaQuery()
                .eq(SensitiveWord::getId, id)
                .one();
        if (word == null) {
            // 100xxx 暂无敏感词不存在专用错误码，按既有先例使用参数校验错误并携带提示信息。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "敏感词不存在");
        }
        return word;
    }

    /**
     * 100xxx 暂无敏感词版本冲突专用错误码，按既有先例使用参数校验错误并携带提示信息
     * @return 版本冲突异常
     */
    private BusinessException versionConflict() {
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "敏感词已被其他请求修改，请刷新后重试");
    }
}
