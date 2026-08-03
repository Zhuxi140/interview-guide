package interview.offer.controller;

import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.offer.model.req.OfferDecisionReq;
import interview.offer.model.vo.OfferDecisionVO;
import interview.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
