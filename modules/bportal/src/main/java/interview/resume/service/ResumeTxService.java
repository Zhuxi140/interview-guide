package interview.resume.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.common.enums.MsgStatus;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuxi
 */

@Service
@RequiredArgsConstructor
public class ResumeTxService {

    private final ResumesMapper resumesMapper;

    /**
     * 专门用来：即使外层事务回滚，也要把状态写成 UPLOAD_FAILED
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUploadFailed(Long resumeId, AnalyzeStatus status) {
        resumesMapper.markUploadFailed(resumeId,status);
    }
}
