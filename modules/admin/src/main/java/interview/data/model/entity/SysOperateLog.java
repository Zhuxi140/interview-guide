package interview.data.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 业务操作审计日志实体。
 */
@Data
@TableName(value = "sys_operate_logs", autoResultMap = true)
public class SysOperateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String traceId;

    private Long userId;

    private String module;

    /**
     * INSERT / UPDATE / DELETE / GRANT。
     */
    private String operateType;

    private String targetTable;

    private Long targetId;

    /**
     * 修改前数据快照 JSON。
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String oldValueJson;

    /**
     * 修改后数据快照 JSON。
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String newValueJson;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;
}
