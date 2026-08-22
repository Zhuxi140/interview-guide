package interview.kyc.model.enums;

/**
 * @author zhuxi
 * @apiNote 个人实名认证材料类型（身份证正反面 + 活体人脸）
 */
public enum KycMaterialType {

    /**
     * 身份证人像面
     */
    ID_CARD_FRONT,

    /**
     * 身份证国徽面
     */
    ID_CARD_BACK,

    /**
     * 活体人脸核验材料（受限图片或短视频）
     */
    FACE_LIVENESS
}
