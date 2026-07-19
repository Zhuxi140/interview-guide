package interview.common.enums;

import lombok.Getter;

/**
 * @author zhuxi
 */

@Getter
public enum MsgStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    IGNORED;

}
