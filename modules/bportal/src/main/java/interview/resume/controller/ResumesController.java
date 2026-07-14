package interview.resume.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Perm;
import interview.common.constant.Result;
import interview.common.enums.PermissionScope;
import interview.common.enums.RiskLevel;
import interview.framework.annonate.MaxRiskLevel;
import interview.framework.annonate.RequirePermission;
import interview.resume.model.req.ResumeListQuery;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeVO;
import interview.resume.service.ResumesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/resumes")
@Tag(name = "简历管理")
@RequiredArgsConstructor
public class ResumesController {

    private final ResumesService resumesService;

    @RequirePermission(permissions = Perm.Resume.UPLOAD, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "上传简历（文件上传，支持 pdf/doc/docx）")
    @PostMapping
    public Result<ResumeVO> uploadResume(@PathVariable("enterpriseId") Long enterpriseId,
                                         @RequestParam("file") MultipartFile file,
                                         ResumeUploadReq metadata) {
        ResumeVO vo = resumesService.uploadResume(enterpriseId, file, metadata);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Resume.LIST, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询简历列表（分页）")
    @GetMapping
    public Result<IPage<ResumeListItemVO>> listResumes(@PathVariable("enterpriseId") Long enterpriseId,
                                                        @Valid ResumeListQuery query) {
        IPage<ResumeListItemVO> page = resumesService.pageResumes(enterpriseId, query);
        return Result.success(page);
    }

    @RequirePermission(permissions = Perm.Resume.DETAIL, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询简历详情")
    @GetMapping("/{resumeId}")
    public Result<ResumeVO> getResumeDetail(@PathVariable("enterpriseId") Long enterpriseId,
                                            @PathVariable("resumeId") Long resumeId) {
        ResumeVO vo = resumesService.getResumeDetail(enterpriseId, resumeId);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Resume.DELETE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "删除简历（逻辑删除）")
    @DeleteMapping("/{resumeId}")
    public Result<Void> deleteResume(@PathVariable("enterpriseId") Long enterpriseId,
                                              @PathVariable("resumeId") Long resumeId) {
        resumesService.deleteResume(enterpriseId, resumeId);
        return Result.success();
    }

    @RequirePermission(permissions = Perm.Resume.ANALYZE, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "触发 AI 简历解析（异步）")
    @PostMapping("/{resumeId}/analyze")
    public Result<ResumeVO> analyzeResume(@PathVariable("enterpriseId") Long enterpriseId,
                                          @PathVariable("resumeId") Long resumeId) {
        ResumeVO vo = resumesService.analyzeResume(enterpriseId, resumeId);
        return Result.success(vo);
    }

    @RequirePermission(permissions = Perm.Resume.DETAIL, scope = PermissionScope.ENTERPRISE)
    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询简历 AI 分析结果（含技能评分 + 候选人画像）")
    @GetMapping("/{resumeId}/analysis")
    public Result<ResumeAnalysisVO> getAnalysis(@PathVariable("enterpriseId") Long enterpriseId,
                                                @PathVariable("resumeId") Long resumeId) {
        ResumeAnalysisVO vo = resumesService.getAnalysis(enterpriseId, resumeId);
        return Result.success(vo);
    }
}
