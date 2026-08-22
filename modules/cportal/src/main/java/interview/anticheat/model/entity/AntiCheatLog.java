package interview.anticheat.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.anticheat.model.enums.AntiCheatEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 智能防作弊行为检测日志表（anti_cheat_logs）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "anti_cheat_logs", autoResultMap = true)
public class AntiCheatLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属企业租户 ID */
    private Long enterpriseId;

    /** 关联的统一运行时会话 ID（多态：文本/语音会话） */
    private Long sessionId;

    /** 涉嫌异常的候选人用户 ID */
    private Long userId;

    /** 客户端事件幂等 ID */
    private String clientEventId;

    /** 违规类型：PAGE_BLUR / NO_FACE / MULTI_FACE */
    private AntiCheatEventType eventType;

    /** 异常行为持续时间（毫秒） */
    private Long durationMs;

    /** 抓拍的现场暗灰度图像存储路径 */
    private String snapshotOssUrl;

    /** 异常触发的调用链 ID */
    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;

    /** 违规发生时间 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
