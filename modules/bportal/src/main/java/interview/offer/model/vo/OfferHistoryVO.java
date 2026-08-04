package interview.offer.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "投递对应的 Offer 历史")
public record OfferHistoryVO(
        @Schema(description = "Offer 记录，按创建时间倒序")
        List<OfferListItemVO> records
) {
}
