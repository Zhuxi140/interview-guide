package interview.infra.localMessage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.annonate.RequirePermission;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.LocalMessageConverter;
import interview.infra.localMessage.model.req.BatchRetryReq;
import interview.infra.localMessage.model.req.LocalMessagePageReq;
import interview.infra.localMessage.model.vo.*;
import interview.infra.localMessage.service.LocalMessageService;
import interview.infra.localMessage.service.LocalMessageService.BatchRetryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiVersion.V1 + "/admin/local-messages")
@Tag(name = "本地消息管理")
@RequiredArgsConstructor
public class LocalMessageAdminController {

    private final LocalMessageService localMessageService;
    private final LocalMessageConverter converter;

    @Operation(summary = "分页查询本地消息")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_PAGE, scope = PermissionScope.PLATFORM)
    @GetMapping
    public Result<IPage<LocalMessagePageVO>> page(
            @Valid @ParameterObject LocalMessagePageReq req) {
        return Result.success(converter.toPageVO(localMessageService.pageQuery(req)));
    }

    @Operation(summary = "查看单条消息详情")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_DETAIL, scope = PermissionScope.PLATFORM)
    @GetMapping("/{id}")
    public Result<LocalMessageDetailVO> getDetail(@PathVariable Long id) {
        LocalMessage detail = localMessageService.getDetail(id);
        return Result.success(converter.toDetailVO(detail));
    }

    @Operation(summary = "手动重试失败消息（仅允许 FAILED）")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_RETRY, scope = PermissionScope.PLATFORM)
    @PostMapping("/{id}/retry")
    public Result<LocalMessageRetryVO> retry(@PathVariable Long id) {
        LocalMessage message = localMessageService.manualRetry(id);
        return Result.success(converter.toRetryVO(message));
    }

    @Operation(summary = "批量重试多条消息")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_BATCH_RETRY, scope = PermissionScope.PLATFORM)
    @PostMapping("/batch-retry")
    public Result<BatchRetryVO> batchRetry(@RequestBody @Valid BatchRetryReq req) {
        BatchRetryResult result = localMessageService.batchRetry(req.getIds());
        List<RetryItemVO> items = result.results().stream()
                .map(item -> new RetryItemVO(item.id(), item.success(), item.error()))
                .toList();
        return Result.success(new BatchRetryVO(
                result.successCount(), result.failCount(), items));
    }
}
