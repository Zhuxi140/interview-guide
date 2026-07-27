package interview.billing.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.billing.mapper.SkuCatalogMapper;
import interview.billing.model.entity.SkuCatalog;
import interview.billing.model.req.SkuCreateReq;
import interview.billing.model.req.SkuStatusReq;
import interview.billing.model.req.SkuUpdateReq;
import interview.billing.model.vo.*;
import interview.billing.service.SkuCatalogService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // TODO ① 去除套餐名称首尾空白并将 currency 规范为大写，校验当前阶段只允许 CNY。
        // TODO ② 再次校验 price>0、tokensIncluded>0，isActive 为空时使用 true。
        // TODO ③ 使用 LambdaQuery 检查未删除记录中 packageName + currency 是否已存在，提前返回套餐重复异常。
        // TODO ④ 构建完整 SkuCatalog 实体，version=0；通过 save 写入并触发主键及审计字段自动填充。
        // TODO ⑤ 数据库唯一索引仍作为并发兜底，将重复键异常转换为套餐重复业务异常。
        // TODO ⑥ 将落库后的实体映射为 SkuCreateVO 返回。
        return null;
    }

    @Override
    @Transactional
    public SkuUpdateVO updateSku(Long skuId, SkuUpdateReq req) {
        // TODO ① 使用 getById 查询未逻辑删除 SKU，不存在时抛出 SKU 不存在业务异常。
        // TODO ② 校验至少提交一个可更新字段；isActive 不应由该接口修改，上架/下架必须调用独立状态接口。
        // TODO ③ 规范化 packageName、currency 并限制当前阶段 currency=CNY；价格或额度非空时校验大于 0。
        // TODO ④ 套餐名称或币种变化时检查 packageName + currency 唯一性。
        // TODO ⑤ 这是多字段半量更新，构建仅含 id、expectedVersion 和非空变更字段的 SkuCatalog 实体。
        // TODO ⑥ 使用 updateById 触发 @Version 和审计自动填充；更新零行时区分记录不存在与版本冲突。
        // TODO ⑦ 回查最新 version、updatedAt，映射为 SkuUpdateVO；不得使用请求中的旧版本拼装响应。
        return null;
    }

    @Override
    @Transactional
    public SkuStatusVO updateSkuStatus(Long skuId, SkuStatusReq req) {
        // TODO ① 使用 LambdaQuery 查询 SKU 当前状态与 version，不存在时抛出 SKU 不存在业务异常。
        // TODO ② 当前 isActive 已等于目标值且 expectedVersion 匹配时幂等返回，不产生无意义版本递增。
        // TODO ③ 仅更新少量字段，使用 LambdaUpdate 按 id + version=expectedVersion 原子设置 isActive。
        // TODO ④ 显式设置 version=version+1、updatedAt、updatedBy、traceId，避免非实体更新遗漏自动填充字段。
        // TODO ⑤ 更新零行时回查：记录不存在返回不存在，其余情况返回乐观锁版本冲突并提示刷新。
        // TODO ⑥ 返回数据库中的最终 isActive、version、updatedAt，映射为 SkuStatusVO。
        return null;
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
}
