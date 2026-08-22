package interview.compliance.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 用户 API 策略表（user_api_policies，限流/黑白名单）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "user_api_policies", autoResultMap = true)
public class UserApiPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 被管控用户 ID */
    private Long userId;

    /** API 路径模板（受限 Ant 风格），空表示匹配全部 */
    private String apiPathRegex;

    /** 策略类型：RATE_LIMIT / BLACKLIST / WHITELIST */
    private PolicyType policyType;

    /** 时间窗口内允许次数（限流时使用） */
    private Integer limitCount;

    /** 时间窗口秒数（限流时使用） */
    private Integer limitSeconds;

    /** 动作：1 强制放行 / -1 强制拦截 */
    private PolicyAction actionType;

    /** 策略过期时间（为空则永久） */
    private OffsetDateTime expireTime;

    /** 风控原因备注 */
    private String reason;

    /** 乐观锁版本号，管理端编辑/删除防并发覆盖 */
    @Version
    private Integer version;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
