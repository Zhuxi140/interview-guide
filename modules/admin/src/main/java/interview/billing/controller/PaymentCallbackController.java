package interview.billing.controller;

import interview.common.constant.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/payments/callbacks/{provider}")
@Tag(name = "支付回调")
@RequiredArgsConstructor
public class PaymentCallbackController {

    @Operation(summary = "第三方支付异步回调（不包装 Result，返回渠道要求的 ACK）")
    @PostMapping
    public String handleCallback(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader java.util.Map<String, String> headers) {
        // TODO ① 按 provider 查找对应支付适配器
        // TODO ② 渠道验签：校验签名、时间戳
        // TODO ③ 按 externalTransactionId 防重放
        // TODO ④ WHERE status = PENDING AND expire_time > now() 原子推进 PAID
        // TODO ⑤ 同一事务增加钱包余额、累计充值额并写入唯一 RECHARGE 账本流水
        // TODO ⑥ 返回渠道要求的 ACK 字符串（不包装 Result）
        return "SUCCESS";
    }
}
