package interview.resume.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeUploadVO;
import interview.resume.model.vo.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zhuxi
 */
public interface ResumesService extends IService<Resumes> {

    ResumeUploadVO uploadResume(MultipartFile file, ResumeUploadReq metadata);

    IPage<ResumeListItemVO> pageResumes(Integer page, Integer size, String fileName, AnalyzeStatus analyzeStatus);

    ResumeVO getResumeDetail(Long resumeId);

    void deleteResume(Long resumeId);

    ResumeVO analyzeResume(Long resumeId);

    ResumeAnalysisVO getAnalysis(Long resumeId);
}
