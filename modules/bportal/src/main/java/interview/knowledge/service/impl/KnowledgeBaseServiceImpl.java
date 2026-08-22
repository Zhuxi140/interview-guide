package interview.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.knowledge.mapper.KnowledgeBaseMapper;
import interview.knowledge.model.entity.KnowledgeBase;
import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.req.AdminKnowledgeBaseSearchReq;
import interview.knowledge.model.req.EnterpriseKnowledgeBaseSearchReq;
import interview.knowledge.model.vo.KnowledgeBaseDetailVO;
import interview.knowledge.model.vo.KnowledgeBaseListItemVO;
import interview.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库文档服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public IPage<KnowledgeBaseListItemVO> pageEnterpriseKnowledgeBases(
            Long enterpriseId, EnterpriseKnowledgeBaseSearchReq req) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 企业可见集合 = 本企业私有文档 + 平台全局文档；visibility 为空时两者都返回。
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .select(KnowledgeBase::getId, KnowledgeBase::getName,
                        KnowledgeBase::getFileName, KnowledgeBase::getFileSize,
                        KnowledgeBase::getVisibility, KnowledgeBase::getVectorStatus,
                        KnowledgeBase::getVectorError, KnowledgeBase::getChunkCount,
                        KnowledgeBase::getUploadedAt)
                .and(w -> w.eq(KnowledgeBase::getVisibility, KnowledgeVisibility.GLOBAL)
                        .or(sub -> sub.eq(KnowledgeBase::getVisibility, KnowledgeVisibility.PRIVATE)
                                .eq(KnowledgeBase::getEnterpriseId, enterpriseId)))
                .eq(req.getVisibility() != null,
                        KnowledgeBase::getVisibility, req.getVisibility())
                .eq(req.getVectorStatus() != null,
                        KnowledgeBase::getVectorStatus, req.getVectorStatus())
                .orderByDesc(KnowledgeBase::getId);
        Page<KnowledgeBase> kbPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        return toListItemPage(kbPage);
    }

    @Override
    public KnowledgeBaseDetailVO getEnterpriseKnowledgeBaseDetail(Long enterpriseId, Long kbId) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 校验文档存在且企业有权读取（全局文档或本企业私有文档）。
        KnowledgeBase knowledgeBase = getRequiredKnowledgeBase(kbId);
        if (!isReadableByEnterprise(knowledgeBase, enterpriseId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return toDetailVO(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteEnterpriseKnowledgeBase(Long enterpriseId, Long kbId, Integer expectedVersion) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 全局文档只能由平台管理端维护，企业只能删除本企业私有文档。
        KnowledgeBase existing = getRequiredKnowledgeBase(kbId);
        if (!isOwnedByEnterprise(existing, enterpriseId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // TODO [Phase7] pgvector 扩展启用后：同事务逻辑删除 document_chunks 切片记录。
        // TODO [Phase7] RustFS 源文件清理走本地消息表补偿，文件清理 Handler 接入后补。

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        KnowledgeBase update = new KnowledgeBase();
        update.setId(kbId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<KnowledgeBase>lambdaUpdate()
                        .eq(KnowledgeBase::getId, kbId)
                        .eq(KnowledgeBase::getEnterpriseId, enterpriseId)
                        .set(KnowledgeBase::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }
    }

    @Override
    public IPage<KnowledgeBaseListItemVO> pageGlobalKnowledgeBases(AdminKnowledgeBaseSearchReq req) {
        // 平台全局知识库只返回 visibility=GLOBAL 的文档，按上传时间倒序。
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .select(KnowledgeBase::getId, KnowledgeBase::getName,
                        KnowledgeBase::getFileName, KnowledgeBase::getFileSize,
                        KnowledgeBase::getVisibility, KnowledgeBase::getVectorStatus,
                        KnowledgeBase::getVectorError, KnowledgeBase::getChunkCount,
                        KnowledgeBase::getVersion, KnowledgeBase::getUploadedAt)
                .eq(KnowledgeBase::getVisibility, KnowledgeVisibility.GLOBAL)
                .eq(req.getVectorStatus() != null,
                        KnowledgeBase::getVectorStatus, req.getVectorStatus())
                .orderByDesc(KnowledgeBase::getId);
        Page<KnowledgeBase> kbPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        return toListItemPage(kbPage);
    }

    @Override
    public KnowledgeBaseDetailVO getGlobalKnowledgeBaseDetail(Long kbId) {
        // 校验文档存在且为全局文档。
        KnowledgeBase knowledgeBase = getRequiredKnowledgeBase(kbId);
        if (knowledgeBase.getVisibility() != KnowledgeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return toDetailVO(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteGlobalKnowledgeBase(Long kbId, Integer expectedVersion) {
        // 校验文档存在且为全局文档。
        KnowledgeBase existing = getRequiredKnowledgeBase(kbId);
        if (existing.getVisibility() != KnowledgeVisibility.GLOBAL) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // TODO [Phase7] pgvector 扩展启用后：同事务逻辑删除 document_chunks 切片记录。
        // TODO [Phase7] RustFS 源文件清理走本地消息表补偿，文件清理 Handler 接入后补。

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        KnowledgeBase update = new KnowledgeBase();
        update.setId(kbId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<KnowledgeBase>lambdaUpdate()
                        .eq(KnowledgeBase::getId, kbId)
                        .set(KnowledgeBase::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }
    }

    private KnowledgeBase getRequiredKnowledgeBase(Long kbId) {
        KnowledgeBase knowledgeBase = lambdaQuery()
                .eq(KnowledgeBase::getId, kbId)
                .one();
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return knowledgeBase;
    }

    private boolean isReadableByEnterprise(KnowledgeBase knowledgeBase, Long enterpriseId) {
        return knowledgeBase.getVisibility() == KnowledgeVisibility.GLOBAL
                || isOwnedByEnterprise(knowledgeBase, enterpriseId);
    }

    private boolean isOwnedByEnterprise(KnowledgeBase knowledgeBase, Long enterpriseId) {
        return knowledgeBase.getVisibility() == KnowledgeVisibility.PRIVATE
                && enterpriseId.equals(knowledgeBase.getEnterpriseId());
    }

    private IPage<KnowledgeBaseListItemVO> toListItemPage(Page<KnowledgeBase> kbPage) {
        Page<KnowledgeBaseListItemVO> voPage = new Page<>(
                kbPage.getCurrent(), kbPage.getSize(), kbPage.getTotal());
        voPage.setRecords(kbPage.getRecords().stream()
                .map(kb -> KnowledgeBaseListItemVO.builder()
                        .id(kb.getId())
                        .name(kb.getName())
                        .fileName(kb.getFileName())
                        .fileSize(kb.getFileSize())
                        .visibility(kb.getVisibility())
                        .vectorStatus(kb.getVectorStatus())
                        .failureReason(kb.getVectorError())
                        .chunkCount(kb.getChunkCount())
                        .uploadedAt(kb.getUploadedAt())
                        .build())
                .toList());
        return voPage;
    }

    private KnowledgeBaseDetailVO toDetailVO(KnowledgeBase kb) {
        // 切片数量直接取 chunk_count 冗余列，不回表 document_chunks。
        return KnowledgeBaseDetailVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .fileName(kb.getFileName())
                .fileSize(kb.getFileSize())
                .visibility(kb.getVisibility())
                .vectorStatus(kb.getVectorStatus())
                .failureReason(kb.getVectorError())
                .chunkCount(kb.getChunkCount())
                .version(kb.getVersion())
                .uploadedAt(kb.getUploadedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }

    private BusinessException versionConflict() {
        // 90xxx 暂无知识库专用版本冲突错误码，使用参数校验错误并携带提示信息。
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "知识库文档已被其他请求修改，请刷新后重试");
    }
}
