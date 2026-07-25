package interview.interviewcfg.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.annonate.MaxRiskLevel;
import interview.common.annonate.RequirePermission;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.req.InterviewTemplateUpdateReq;
import interview.interviewcfg.model.req.PhaseConfigUpsertReq;
import interview.interviewcfg.model.vo.*;
import interview.interviewcfg.service.InterviewStageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/interview-templates")
@Tag(name = "面试模板与组卷（B端）")
@RequiredArgsConstructor
@Validated
public class InterviewTemplateController {

    private final InterviewStageTemplateService interviewStageTemplateService;

    @Operation(summary = "创建面试阶段模板")
    @PostMapping
    public Result<InterviewTemplateCreateVO> createTemplate(
            @PathVariable Long enterpriseId,
            @Valid @RequestBody InterviewTemplateCreateReq req) {
        return Result.success(interviewStageTemplateService.createTemplate(enterpriseId, req));
    }

    @Operation(summary = "查询面试阶段模板列表（分页）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewTemplate.LIST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping
    public Result<IPage<InterviewTemplateListItemVO>> listTemplates(
            @PathVariable Long enterpriseId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "^(?i)(asc|desc)$") String order) {
        return Result.success(
                interviewStageTemplateService.pageTemplates(enterpriseId, page, size, sort, order));
    }

    @Operation(summary = "查询模板详情（含阶段序列与组卷策略）")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewTemplate.DETAIL,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/{templateId}")
    public Result<InterviewTemplateDetailVO> getTemplateDetail(
            @PathVariable Long enterpriseId,
            @PathVariable Long templateId) {
        return Result.success(interviewStageTemplateService.getTemplateDetail(enterpriseId, templateId));
    }

    @Operation(summary = "更新面试阶段模板")
    @PatchMapping("/{templateId}")
    public Result<InterviewTemplateUpdateVO> updateTemplate(
            @PathVariable Long enterpriseId,
            @PathVariable Long templateId,
            @Valid @RequestBody InterviewTemplateUpdateReq req) {
        return Result.success(interviewStageTemplateService.updateTemplate(enterpriseId, templateId, req));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{templateId}")
    public Result<InterviewTemplateDeleteVO> deleteTemplate(
            @PathVariable Long enterpriseId,
            @PathVariable Long templateId,
            @RequestHeader("If-Match") Integer expectedVersion) {
        return Result.success(interviewStageTemplateService.deleteTemplate(enterpriseId, templateId, expectedVersion));
    }

    @Operation(summary = "创建或完整替换某阶段组卷策略")
    @PutMapping("/{templateId}/phase-configs/{phaseCode}")
    public Result<PhaseConfigVO> upsertPhaseConfig(
            @PathVariable Long enterpriseId,
            @PathVariable Long templateId,
            @PathVariable String phaseCode,
            @Valid @RequestBody PhaseConfigUpsertReq req) {
        return Result.success(
                interviewStageTemplateService.upsertPhaseConfig(enterpriseId, templateId, phaseCode, req));
    }

    @Operation(summary = "查询阶段组卷策略列表")
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @RequirePermission(
            permissions = Perm.InterviewTemplate.PHASE_CONFIG_LIST,
            scope = PermissionScope.ENTERPRISE
    )
    @GetMapping("/{templateId}/phase-configs")
    public Result<List<PhaseConfigVO>> listPhaseConfigs(
            @PathVariable Long enterpriseId,
            @PathVariable Long templateId) {
        return Result.success(interviewStageTemplateService.listPhaseConfigs(enterpriseId, templateId));
    }
}
