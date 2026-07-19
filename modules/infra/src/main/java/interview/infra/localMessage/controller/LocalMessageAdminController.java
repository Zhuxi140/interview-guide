package interview.infra.localMessage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.annonate.RequirePermission;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.service.LocalMessageService;
import interview.infra.localMessage.service.LocalMessageService.BatchRetryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "分页查询本地消息")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_PAGE, scope = PermissionScope.PLATFORM)
    @GetMapping("/page")
    public Result<IPage<LocalMessage>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {
        IPage<LocalMessage> result = localMessageService.pageQuery(page, size, status, topic, priority, startTime, endTime);
        return Result.success(result);
    }

    @Operation(summary = "查看单条消息详情")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_DETAIL, scope = PermissionScope.PLATFORM)
    @GetMapping("/{id}")
    public Result<LocalMessage> getDetail(@PathVariable Long id) {
        LocalMessage detail = localMessageService.getDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "手动重试单条消息（允许 PENDING/FAILED，拒绝 SUCCESS/IGNORED）")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_RETRY, scope = PermissionScope.PLATFORM)
    @PostMapping("/{id}/retry")
    public Result<LocalMessage> retry(@PathVariable Long id) {
        LocalMessage message = localMessageService.manualRetry(id);
        return Result.success(message);
    }

    @Operation(summary = "批量重试多条消息")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_BATCH_RETRY, scope = PermissionScope.PLATFORM)
    @PostMapping("/batch-retry")
    public Result<BatchRetryResult> batchRetry(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        BatchRetryResult result = localMessageService.batchRetry(ids);
        return Result.success(result);
    }

    @Operation(summary = "手动设置消息状态")
    @RequirePermission(permissions = Perm.Ops.LOCAL_MESSAGE_STATUS, scope = PermissionScope.PLATFORM)
    @PutMapping("/{id}/status")
    public Result<LocalMessage> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        LocalMessage message = localMessageService.updateStatus(id, status);
        return Result.success(message);
    }
}
