package interview.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.data.model.entity.SysOperateLog;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * 业务操作审计日志 Mapper。
 *
 * @author zhuxi
 */
public interface SysOperateLogMapper extends BaseMapper<SysOperateLog> {

    /**
     * 分页查询企业域内操作审计日志（限定企业成员产生的记录）。
     *
     * @param page        分页参数
     * @param enterpriseId 企业 ID
     * @param module      业务模块筛选，可为空
     * @param operateType 操作类型筛选，可为空
     * @param targetTable 目标表筛选，可为空
     * @param targetId    目标记录 ID 筛选，可为空
     * @param startTime   开始时间筛选，可为空
     * @param endTime     结束时间筛选，可为空
     * @return 企业域操作日志分页
     */
    IPage<SysOperateLog> pageByEnterprise(IPage<SysOperateLog> page,
                                          @Param("enterpriseId") Long enterpriseId,
                                          @Param("module") String module,
                                          @Param("operateType") String operateType,
                                          @Param("targetTable") String targetTable,
                                          @Param("targetId") Long targetId,
                                          @Param("startTime") OffsetDateTime startTime,
                                          @Param("endTime") OffsetDateTime endTime);
}
