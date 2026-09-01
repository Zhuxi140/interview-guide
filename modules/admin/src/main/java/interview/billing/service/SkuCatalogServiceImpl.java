package interview.billing.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.billing.mapper.SkuCatalogMapper;
import interview.billing.model.entity.SkuCatalog;
import interview.billing.model.req.SkuCreateReq;
import interview.billing.model.req.SkuStatusReq;
import interview.billing.model.req.SkuUpdateReq;
import interview.billing.model.vo.*;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SkuCatalogServiceImpl
        extends ServiceImpl<SkuCatalogMapper, SkuCatalog>
        implements SkuCatalogService {

    @Override
    public IPage<SkuVO> pagePublicSkus(Integer page, Integer size) {
        // 公开列表只展示已上架、未逻辑删除的套餐。
        validatePage(page, size);
        IPage<SkuCatalog> skuPage = lambdaQuery()
                .select(SkuCatalog::getId, SkuCatalog::getPackageName,
                        SkuCatalog::getDescription, SkuCatalog::getPrice,
                        SkuCatalog::getCurrency, SkuCatalog::getTokensIncluded)
                .eq(SkuCatalog::getIsActive, true)
                .orderByDesc(SkuCatalog::getCreatedAt)
                .orderByDesc(SkuCatalog::getId)
                .page(new Page<>(page, size));

        // 转换为不包含后台状态和审计字段的公开响应。
        return skuPage.convert(this::toPublicVO);
    }

    @Override
    public SkuVO getPublicSku(Long skuId) {
        // 下架、删除和不存在的套餐对公开接口统一表现为不存在。
        SkuCatalog sku = lambdaQuery()
                .select(SkuCatalog::getId, SkuCatalog::getPackageName,
                        SkuCatalog::getDescription, SkuCatalog::getPrice,
                        SkuCatalog::getCurrency, SkuCatalog::getTokensIncluded)
                .eq(SkuCatalog::getId, skuId)
                .eq(SkuCatalog::getIsActive, true)
                .one();
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        return toPublicVO(sku);
    }

    @Override
    public IPage<SkuListItemVO> pageAdminSkus(Integer page, Integer size, Boolean isActive) {
        // 管理端可查看上架和下架套餐，但仍自动排除逻辑删除数据。
        validatePage(page, size);
        IPage<SkuCatalog> skuPage = lambdaQuery()
                .select(SkuCatalog::getId, SkuCatalog::getPackageName,
                        SkuCatalog::getDescription, SkuCatalog::getPrice,
                        SkuCatalog::getCurrency, SkuCatalog::getTokensIncluded,
                        SkuCatalog::getIsActive, SkuCatalog::getVersion,
                        SkuCatalog::getCreatedAt)
                .eq(isActive != null, SkuCatalog::getIsActive, isActive)
                .orderByDesc(SkuCatalog::getCreatedAt)
                .orderByDesc(SkuCatalog::getId)
                .page(new Page<>(page, size));

        // 映射管理端列表所需的状态和版本字段。
        return skuPage.convert(sku -> new SkuListItemVO(
                sku.getId(),
                sku.getPackageName(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getCurrency(),
                sku.getTokensIncluded(),
                sku.getIsActive(),
                sku.getVersion(),
                sku.getCreatedAt()
        ));
    }

    @Override
    public SkuDetailVO getAdminSku(Long skuId) {
        // 主键查询自动附加逻辑删除条件。
        SkuCatalog sku = getById(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        return new SkuDetailVO(
                sku.getId(),
                sku.getPackageName(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getCurrency(),
                sku.getTokensIncluded(),
                sku.getIsActive(),
                sku.getVersion(),
                sku.getCreatedAt(),
                sku.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SkuCreateVO createSku(SkuCreateReq req) {
        String packageName = normalizePackageName(req.getPackageName());
        String currency = normalizeCurrency(req.getCurrency());
        if (lambdaQuery()
                .eq(SkuCatalog::getPackageName, packageName)
                .eq(SkuCatalog::getCurrency, currency)
                .exists()) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }

        // 创建时一次写入全部套餐字段，审计字段交由 MyBatis 自动填充。
        SkuCatalog sku = SkuCatalog.builder()
                .packageName(packageName)
                .description(req.getDescription())
                .price(req.getPrice())
                .currency(currency)
                .tokensIncluded(req.getTokensIncluded())
                .isActive(req.getIsActive() == null || req.getIsActive())
                .version(0)
                .build();
        try {
            if (!save(sku)) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "套餐创建失败");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }
        return new SkuCreateVO(
                sku.getId(), sku.getPackageName(), sku.getPrice(), sku.getCurrency(),
                sku.getTokensIncluded(), sku.getIsActive(), sku.getVersion());
    }

    @Override
    @Transactional
    public SkuUpdateVO updateSku(Long skuId, SkuUpdateReq req) {
        SkuCatalog current = getById(skuId);
        if (current == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        if (req.getIsActive() != null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "请使用套餐状态接口上架或下架");
        }
        if (req.getPackageName() == null && req.getDescription() == null
                && req.getPrice() == null && req.getCurrency() == null
                && req.getTokensIncluded() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "至少提交一个可更新字段");
        }

        String packageName = req.getPackageName() == null
                ? current.getPackageName() : normalizePackageName(req.getPackageName());
        String currency = req.getCurrency() == null
                ? current.getCurrency() : normalizeCurrency(req.getCurrency());
        if ((!Objects.equals(packageName, current.getPackageName())
                || !Objects.equals(currency, current.getCurrency()))
                && lambdaQuery()
                .eq(SkuCatalog::getPackageName, packageName)
                .eq(SkuCatalog::getCurrency, currency)
                .ne(SkuCatalog::getId, skuId)
                .exists()) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }

        // 多字段半量更新使用实体，并由 @Version 完成 CAS。
        SkuCatalog update = new SkuCatalog();
        update.setId(skuId);
        update.setPackageName(req.getPackageName() == null ? null : packageName);
        update.setDescription(req.getDescription());
        update.setPrice(req.getPrice());
        update.setCurrency(req.getCurrency() == null ? null : currency);
        update.setTokensIncluded(req.getTokensIncluded());
        update.setVersion(req.getExpectedVersion());
        try {
            if (!updateById(update)) {
                if (getById(skuId) == null) {
                    throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
                }
                throw new BusinessException(ErrorCode.SKU_VERSION_CONFLICT);
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }
        SkuCatalog latest = lambdaQuery()
                .select(SkuCatalog::getId, SkuCatalog::getVersion, SkuCatalog::getUpdatedAt)
                .eq(SkuCatalog::getId, skuId)
                .one();
        return new SkuUpdateVO(latest.getId(), latest.getVersion(), latest.getUpdatedAt());
    }

    @Override
    @Transactional
    public SkuStatusVO updateSkuStatus(Long skuId, SkuStatusReq req) {
        SkuCatalog current = lambdaQuery()
                .select(SkuCatalog::getId, SkuCatalog::getIsActive,
                        SkuCatalog::getVersion, SkuCatalog::getUpdatedAt)
                .eq(SkuCatalog::getId, skuId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        if (!Objects.equals(current.getVersion(), req.getExpectedVersion())) {
            throw new BusinessException(ErrorCode.SKU_VERSION_CONFLICT);
        }
        if (Objects.equals(current.getIsActive(), req.getIsActive())) {
            return new SkuStatusVO(
                    current.getId(), current.getIsActive(),
                    current.getVersion(), current.getUpdatedAt());
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(SkuCatalog::getId, skuId)
                .eq(SkuCatalog::getVersion, req.getExpectedVersion())
                .set(SkuCatalog::getIsActive, req.getIsActive())
                .setSql("version = version + 1")
                .set(SkuCatalog::getUpdatedAt, now)
                .set(SkuCatalog::getUpdatedBy, AuthContext.getRequiredUserId())
                .set(SkuCatalog::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            if (getById(skuId) == null) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.SKU_VERSION_CONFLICT);
        }
        return new SkuStatusVO(
                skuId, req.getIsActive(), req.getExpectedVersion() + 1, now);
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private SkuVO toPublicVO(SkuCatalog sku) {
        return new SkuVO(
                sku.getId(),
                sku.getPackageName(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getCurrency(),
                sku.getTokensIncluded()
        );
    }

    private String normalizePackageName(String packageName) {
        if (StrUtil.isBlank(packageName)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "套餐名称不能为空");
        }
        return packageName.trim();
    }

    private String normalizeCurrency(String currency) {
        if (StrUtil.isBlank(currency)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "币种不能为空");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!"CNY".equals(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "当前仅支持 CNY");
        }
        return normalized;
    }
}
