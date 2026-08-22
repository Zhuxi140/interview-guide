package interview.code.model.enums;

/**
 * 编程题可见性。
 *
 * <p>GLOBAL 为平台公共题库，企业只读；PRIVATE 为企业私有题，
 * 仅创建企业和平台管理端可维护。</p>
 */
public enum CodeVisibility {

    /** 平台公共题库 */
    GLOBAL,

    /** 企业私有题 */
    PRIVATE
}
