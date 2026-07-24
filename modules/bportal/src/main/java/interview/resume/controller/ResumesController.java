package interview.resume.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import interview.common.annonate.MaxRiskLevel;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeDeleteVO;
import interview.resume.model.vo.ResumeDownloadVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeUploadVO;
import interview.resume.model.vo.ResumeVO;
import interview.resume.service.ResumesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/resumes")
@Tag(name = "简历管理（候选人侧）")
@RequiredArgsConstructor
@Validated
public class ResumesController {

    private final ResumesService resumesService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "上传简历（文件上传，支持 pdf/doc/docx）")
    @PostMapping
    public Result<ResumeUploadVO> uploadResume(@RequestParam(value = "file") MultipartFile file,
                                                @Valid ResumeUploadReq metadata) {
        ResumeUploadVO vo = resumesService.uploadResume(file, metadata);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询我的简历列表（分页）")
    @GetMapping
    public Result<IPage<ResumeListItemVO>> listResumes(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
                                                         @RequestParam(required = false) String fileName,
                                                         @RequestParam(required = false) AnalyzeStatus analyzeStatus,
                                                         @RequestParam(defaultValue = "createdAt")
                                                         @Pattern(regexp = "^createdAt$") String sort,
                                                         @RequestParam(defaultValue = "desc")
                                                         @Pattern(regexp = "^(?i)(asc|desc)$") String order) {
        IPage<ResumeListItemVO> result =
                resumesService.pageResumes(page, size, fileName, analyzeStatus, order);
        return Result.success(result);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "获取本人简历短期下载地址")
    @GetMapping("/{resumeId}/download")
    public Result<ResumeDownloadVO> getDownloadUrl(
            @PathVariable("resumeId") Long resumeId) {
        return Result.success(resumesService.getDownloadUrl(resumeId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询简历详情")
    @GetMapping("/{resumeId}")
    public Result<ResumeVO> getResumeDetail(@PathVariable("resumeId") Long resumeId) {
        ResumeVO vo = resumesService.getResumeDetail(resumeId);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.MID_RISK)
    @Operation(summary = "删除简历（逻辑删除）")
    @DeleteMapping("/{resumeId}")
    public Result<ResumeDeleteVO> deleteResume(@PathVariable("resumeId") Long resumeId) {
        resumesService.deleteResume(resumeId);
        return Result.success(new ResumeDeleteVO(resumeId));
    }

    @MaxRiskLevel(RiskLevel.LOW_RISK)
    @Operation(summary = "触发 AI 简历解析（异步，按需付费）")
    @PostMapping("/{resumeId}/analyze")
    public Result<ResumeAnalyzeTriggerVO> analyzeResume(@PathVariable("resumeId") Long resumeId) {
        ResumeAnalyzeTriggerVO vo = resumesService.analyzeResume(resumeId);
        return Result.success(vo);
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询简历 AI 分析结果（含技能评分 + 候选人画像）")
    @GetMapping("/{resumeId}/analysis")
    public Result<ResumeAnalysisVO> getAnalysis(@PathVariable("resumeId") Long resumeId) {
        ResumeAnalysisVO vo = resumesService.getAnalysis(resumeId);
        return Result.success(vo);
    }
}
