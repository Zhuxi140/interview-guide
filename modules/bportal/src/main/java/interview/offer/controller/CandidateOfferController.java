package interview.offer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.offer.model.req.OfferDecisionReq;
import interview.offer.model.enums.OfferStatus;
import interview.offer.model.vo.CandidateOfferDetailVO;
import interview.offer.model.vo.CandidateOfferListItemVO;
import interview.offer.model.vo.OfferDecisionVO;
import interview.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/candidate/offers")
@Tag(name = "候选人 Offer（C端）")
@RequiredArgsConstructor
@Validated
public class CandidateOfferController {

    private final OfferService offerService;

    @Operation(summary = "接受或拒绝本人Offer")
    @PostMapping("/{offerId}/decision")
    public Result<OfferDecisionVO> decideOffer(
            @PathVariable Long offerId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody OfferDecisionReq req) {
        return Result.success(offerService.decideOffer(offerId, idempotencyKey, req));
    }

    @Operation(summary = "候选人分页查询自己的 Offer")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping
    public Result<IPage<CandidateOfferListItemVO>> pageMyOffers(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) OfferStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        return Result.success(offerService.pageCandidateOffers(page, size, status, sort, order));
    }

    @Operation(summary = "查询本人 Offer 详情")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @GetMapping("/{offerId}")
    public Result<CandidateOfferDetailVO> getMyOffer(@PathVariable Long offerId) {
        return Result.success(offerService.getCandidateOfferDetail(offerId));
    }
}
