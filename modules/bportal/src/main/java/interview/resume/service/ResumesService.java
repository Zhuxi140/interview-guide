package interview.resume.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeUploadVO;
import interview.resume.model.vo.ResumeVO;
import interview.resume.model.vo.ResumeDownloadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zhuxi
 */
public interface ResumesService extends IService<Resumes> {

    /**
     * 上传简历
     * @param file 简历文件
     * @param metadata 文件展示信息
     * @return 上传结果
     */
    ResumeUploadVO uploadResume(MultipartFile file, ResumeUploadReq metadata);

    /**
     * 查询当前用户简历
     * @param page 页码
     * @param size 每页条数
     * @param fileName 文件名
     * @param analyzeStatus 分析状态
     * @param order 排序方向
     * @return 简历分页
     */
    IPage<ResumeListItemVO> pageResumes(
            Integer page, Integer size, String fileName,
            AnalyzeStatus analyzeStatus, String order);

    /**
     * 查询简历详情
     * @param resumeId 简历 ID
     * @return 简历详情
     */
    ResumeVO getResumeDetail(Long resumeId);

    /**
     * 获取简历短期下载地址
     * @param resumeId 简历 ID
     * @return 下载地址
     */
    ResumeDownloadVO getDownloadUrl(Long resumeId);

    /**
     * 逻辑删除简历
     * @param resumeId 简历 ID
     */
    void deleteResume(Long resumeId);

    /**
     * 触发简历 AI 解析（异步，按需付费）
     * @param resumeId 简历 ID
     * @param idempotencyKey 当前用户与简历范围内的客户端幂等键
     * @return 任务受理信息
     */
    ResumeAnalyzeTriggerVO analyzeResume(Long resumeId, String idempotencyKey);

    /**
     * 查询简历分析结果
     * @param resumeId 简历 ID
     * @return 分析结果
     */
    ResumeAnalysisVO getAnalysis(Long resumeId);
}
