package interview.kyc.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 个人实名认证状态枚举（sys_user_kyc.auth_status，SMALLINT）
 */
@Getter
@AllArgsConstructor
public enum KycStatus {

    /**
     * 未提交：仅作为无记录时的返回语义，不落库
     */
    NOT_SUBMITTED(-1, "未提交"),

    /**
     * 待审核
     */
    PENDING(0, "待审核"),

    /**
     * 已通过
     */
    APPROVED(1, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(2, "已拒绝");

    @EnumValue
    private final int code;
    private final String message;

    /**
     * 判断该状态是否为可持久化的真实审核状态
     * @return true 表示可落库（PENDING/APPROVED/REJECTED），false 表示仅为返回语义
     */
    public boolean isPersisted() {
        return this != NOT_SUBMITTED;
    }
}
