package interview.resume.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.resume.model.entity.Resumes;
import interview.resume.model.req.ResumeListQuery;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeVO;
import org.springframework.web.multipart.MultipartFile;

public interface ResumesService extends IService<Resumes> {

    ResumeVO uploadResume(Long enterpriseId, MultipartFile file, ResumeUploadReq metadata);

    IPage<ResumeListItemVO> pageResumes(Long enterpriseId, ResumeListQuery query);

    ResumeVO getResumeDetail(Long enterpriseId, Long resumeId);

    void deleteResume(Long enterpriseId, Long resumeId);

    ResumeVO analyzeResume(Long enterpriseId, Long resumeId);

    ResumeAnalysisVO getAnalysis(Long enterpriseId, Long resumeId);
}
