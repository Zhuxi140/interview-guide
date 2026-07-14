package interview.resume.service.impl;

import interview.resume.model.entity.CandidateProfile;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.service.CandidateProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 候选人画像聚合表（供雷达图读取） 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
@Service
public class CandidateProfileServiceImpl extends ServiceImpl<CandidateProfileMapper, CandidateProfile> implements CandidateProfileService {

}
