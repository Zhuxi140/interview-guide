package interview.candidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.candidate.model.entity.CandidateBasicProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CandidateBasicProfileMapper extends BaseMapper<CandidateBasicProfile> {
}
