package interview.offer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.offer.model.entity.Offer;
import interview.offer.model.bo.CandidateOfferQueryBO;
import interview.offer.model.enums.OfferStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Offer 持久化 Mapper。
 */
@Mapper
public interface OfferMapper extends BaseMapper<Offer> {

    /**
     * 分页查询候选人可见的 Offer
     * @param page 分页参数
     * @param candidateUserId 候选人用户ID
     * @param status 状态筛选
     * @param ascending 是否按创建时间升序
     * @return Offer 分页结果
     */
    IPage<CandidateOfferQueryBO> pageCandidateOffers(
            IPage<?> page,
            @Param("candidateUserId") Long candidateUserId,
            @Param("status") OfferStatus status,
            @Param("ascending") boolean ascending);

    /**
     * 查询候选人可见的 Offer 详情
     * @param offerId Offer ID
     * @param candidateUserId 候选人用户ID
     * @return Offer 详情，不存在或不可见时返回 null
     */
    CandidateOfferQueryBO getCandidateOfferDetail(
            @Param("offerId") Long offerId,
            @Param("candidateUserId") Long candidateUserId);
}
