package interview.anticheat.model.enums;

/**
 * @author zhuxi
 * @apiNote 防作弊违规事件类型（anti_cheat_logs.event_type，VARCHAR）
 */
public enum AntiCheatEventType {

    /**
     * 切屏（页面失焦）
     */
    PAGE_BLUR,

    /**
     * 画面中无人脸
     */
    NO_FACE,

    /**
     * 画面中出现多人脸
     */
    MULTI_FACE
}
