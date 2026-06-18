package interview.common.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 用户风控等级枚举类
 */

@AllArgsConstructor
@Getter
public enum RiskLevel {

    NO_RISK(0,"无风险"),
    LOW_RISK(1,"低风险"),
    MID_RISK(2,"中风险"),
    HIGH_RISK(3,"高风险");



    @EnumValue
    private final Integer code;
    private final String message;

    /**
     * 根据code获取枚举
     * @param code code
     * @return RiskLevel
     */
    public static RiskLevel codeToRiskLevel(Integer code){
        for(RiskLevel value : values()){
            if (value.code.equals(code)){
                return value;
            }
        }
        throw new IllegalArgumentException("无效的风控等级");
    }
}
