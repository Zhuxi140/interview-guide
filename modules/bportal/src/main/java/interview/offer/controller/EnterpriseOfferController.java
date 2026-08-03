package interview.offer.controller;

import interview.common.annonate.RequireActiveEnterprise;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.offer.model.req.OfferCreateReq;
import interview.offer.model.req.OfferSendReq;
import interview.offer.model.req.OfferUpdateReq;
import interview.offer.model.req.OfferWithdrawReq;
import interview.offer.model.vo.OfferCreateVO;
import interview.offer.model.vo.OfferSendVO;
import interview.offer.model.vo.OfferUpdateVO;
import interview.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}")
@Tag(name = "Offer 管理（B端）")
@RequiredArgsConstructor
@Validated
public class EnterpriseOfferController {

    private final OfferService offerService;

    @Operation(summary = "为投递创建Offer草稿")
    @PostMapping("/applications/{applicationId}/offers")
    public Result<OfferCreateVO> createOffer(
            @PathVariable Long enterpriseId,
            @PathVariable Long applicationId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody OfferCreateReq req) {
        return Result.success(offerService.createOffer(
                enterpriseId, applicationId, idempotencyKey, req));
    }

    @Operation(summary = "修改尚未发送的Offer")
    @PatchMapping("/offers/{offerId}")
    public Result<OfferUpdateVO> updateOffer(
            @PathVariable Long enterpriseId,
            @PathVariable Long offerId,
            @Valid @RequestBody OfferUpdateReq req) {
        return Result.success(offerService.updateOffer(enterpriseId, offerId, req));
    }

    @Operation(summary = "发送Offer")
    @PostMapping("/offers/{offerId}/send")
    public Result<OfferSendVO> sendOffer(
            @PathVariable Long enterpriseId,
            @PathVariable Long offerId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody OfferSendReq req) {
        return Result.success(offerService.sendOffer(
                enterpriseId, offerId, idempotencyKey, req));
    }

    @Operation(summary = "撤回尚未被接受的Offer")
    @PostMapping("/offers/{offerId}/withdraw")
    public Result<OfferUpdateVO> withdrawOffer(
            @PathVariable Long enterpriseId,
            @PathVariable Long offerId,
            @Valid @RequestBody OfferWithdrawReq req) {
        return Result.success(offerService.withdrawOffer(enterpriseId, offerId, req));
    }
}
