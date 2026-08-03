package interview.textinterview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.textinterview.model.entity.CandidateInterviewAvailability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 候选人可面试时间 Mapper。
 */
@Mapper
public interface CandidateInterviewAvailabilityMapper
        extends BaseMapper<CandidateInterviewAvailability> {

    /**
     * 按版本号原子更新候选人可面试时间
     * @param availability 新配置及审计信息
     * @param expectedVersion 期望版本号
     * @return 更新行数
     */
    int updateByExpectedVersion(
            @Param("availability") CandidateInterviewAvailability availability,
            @Param("expectedVersion") Integer expectedVersion);
}
