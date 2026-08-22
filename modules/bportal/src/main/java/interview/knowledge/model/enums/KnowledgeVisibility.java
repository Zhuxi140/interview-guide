package interview.knowledge.model.enums;

/**
 * 知识库可见性。
 *
 * <p>GLOBAL 为平台公共底座（enterprise_id=0），企业只读；
 * PRIVATE 为企业私有文档，仅归属企业可维护。</p>
 */
public enum KnowledgeVisibility {

    /** 平台公共底座 */
    GLOBAL,

    /** 企业私有文档 */
    PRIVATE
}
