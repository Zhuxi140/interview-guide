package interview.matching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.matching.model.entity.JobScreeningConfig;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * 岗位 AI 初筛配置 Mapper。
 */
public interface JobScreeningConfigMapper extends BaseMapper<JobScreeningConfig> {

    /**
     * 按版本号原子更新岗位初筛配置。
     *
     * @param config 新配置
     * @param expectedVersion 期望版本
     * @param now 当前时间
     * @return 更新行数
     */
    int updateByExpectedVersion(@Param("config") JobScreeningConfig config,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("now") OffsetDateTime now);
}
