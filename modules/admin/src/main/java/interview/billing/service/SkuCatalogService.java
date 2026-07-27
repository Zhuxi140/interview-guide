package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.billing.model.entity.SkuCatalog;
import interview.billing.model.req.SkuCreateReq;
import interview.billing.model.req.SkuStatusReq;
import interview.billing.model.req.SkuUpdateReq;
import interview.billing.model.vo.*;

public interface SkuCatalogService extends IService<SkuCatalog> {

    /**
     * 分页查询已上架套餐
     * @param page 当前页
     * @param size 每页数量
     * @return 公开套餐分页
     */
    IPage<SkuVO> pagePublicSkus(Integer page, Integer size);

    /**
     * 查询已上架套餐详情
     * @param skuId 套餐 ID
     * @return 公开套餐详情
     */
    SkuVO getPublicSku(Long skuId);

    /**
     * 分页查询平台套餐
     * @param page 当前页
     * @param size 每页数量
     * @param isActive 上架状态
     * @return 平台套餐分页
     */
    IPage<SkuListItemVO> pageAdminSkus(Integer page, Integer size, Boolean isActive);

    /**
     * 查询平台套餐详情
     * @param skuId 套餐 ID
     * @return 平台套餐详情
     */
    SkuDetailVO getAdminSku(Long skuId);

    /**
     * 创建算力套餐
     * @param req 创建套餐请求
     * @return 创建的套餐
     */
    SkuCreateVO createSku(SkuCreateReq req);

    /**
     * 更新算力套餐
     * @param skuId 套餐 ID
     * @param req 更新套餐请求
     * @return 套餐更新结果
     */
    SkuUpdateVO updateSku(Long skuId, SkuUpdateReq req);

    /**
     * 更新套餐上架状态
     * @param skuId 套餐 ID
     * @param req 状态更新请求
     * @return 套餐状态更新结果
     */
    SkuStatusVO updateSkuStatus(Long skuId, SkuStatusReq req);
}
