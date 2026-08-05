package interview.interviewcfg.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequireActiveEnterprise;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftCreateVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftDetailVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftListItemVO;
import interview.interviewcfg.service.InterviewPlanDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireActiveEnterprise
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/applications/{applicationId}/interview-plan-drafts")
@Tag(name = "Agent 面试编排草案（B端）")
@RequiredArgsConstructor
@Validated
public class InterviewPlanDraftController {

    private final InterviewPlanDraftService interviewPlanDraftService;

    @Operation(summary = "异步创建 Agent 面试编排草案")
    @RequirePermission(
            permissions = Perm.InterviewSchedule.AI_SUGGEST,
            scope = PermissionScope.ENTERPRISE
    )
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<InterviewPlanDraftCreateVO> createDraft(
            @PathVariable Long enterpriseId,
            @PathVariable Long applicationId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody InterviewPlanDraftCreateReq req) {
        return Result.success(interviewPlanDraftService.createDraft(
                enterpriseId, applicationId, idempotencyKey, req));
    }

    @Operation(summary = "应用 Agent 面试编排草案")
    @RequirePermission(
            permissions = Perm.InterviewSchedule.CREATE,
            scope = PermissionScope.ENTERPRISE
    )
    @PostMapping("/{draftId}/apply")
    public Result<InterviewPlanDraftApplyVO> applyDraft(
            @PathVariable Long enterpriseId,
            @PathVariable Long applicationId,
            @PathVariable Long draftId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody InterviewPlanDraftApplyReq req) {
        return Result.success(interviewPlanDraftService.applyDraft(
                enterpriseId, applicationId, draftId, idempotencyKey, req));
    }

    @Operation(summary = "分页查询该投递的 Agent 编排草案")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewSchedule.AI_SUGGEST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping
    public Result<IPage<InterviewPlanDraftListItemVO>> pageDrafts(
            @PathVariable Long enterpriseId,
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) InterviewPlanDraftStatus status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        return Result.success(interviewPlanDraftService.pageDrafts(
                enterpriseId, applicationId, page, size, status, sort, order));
    }

    @Operation(summary = "查询 Agent 编排草案详情")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewSchedule.AI_SUGGEST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/{draftId}")
    public Result<InterviewPlanDraftDetailVO> getDraftDetail(
            @PathVariable Long enterpriseId,
            @PathVariable Long applicationId,
            @PathVariable Long draftId) {
        return Result.success(interviewPlanDraftService.getDraftDetail(enterpriseId, applicationId, draftId));
    }
}
