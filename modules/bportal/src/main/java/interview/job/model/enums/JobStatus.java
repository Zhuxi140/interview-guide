package interview.job.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 */

@Getter
@AllArgsConstructor
public enum JobStatus {

    DRAFT(2,"草稿"),
    OPEN(1,"招聘中"),
    CLOSED(0,"暂停招聘/已满");

    @EnumValue
    private final Integer code;
    private final String msg;


    /**
     * 根据code转换为枚举类
     * @param code 状态码
     * @return 枚举类
     */
    public static JobStatus getJobStatus(Integer code) {
        for (JobStatus jobStatus : JobStatus.values()) {
            if (jobStatus.getCode().equals(code)) {
                return jobStatus;
            }
        }
        throw new IllegalArgumentException("无法识别的岗位状态: " + code);
    }
}
