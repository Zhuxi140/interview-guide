package interview.candidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.candidate.model.entity.EnterpriseCandidate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EnterpriseCandidateMapper extends BaseMapper<EnterpriseCandidate> {
}
