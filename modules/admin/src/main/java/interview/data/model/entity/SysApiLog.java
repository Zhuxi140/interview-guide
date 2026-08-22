package interview.data.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * API 访问日志热表实体。
 */
@Data
@TableName(value = "sys_api_logs")
public class SysApiLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String traceId;

    private Long userId;

    private String apiUrl;

    private String requestMethod;

    private String clientIp;

    private Long executionTime;

    private Integer responseStatus;

    private String errorMsg;

    private Integer llmInputTokens;

    private Integer llmOutputTokens;

    private String llmModel;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;
}
