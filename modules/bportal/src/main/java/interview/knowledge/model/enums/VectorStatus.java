package interview.knowledge.model.enums;

/**
 * 知识库向量化状态。
 *
 * <p>文档上传后异步向量化，仅 COMPLETED 状态的知识库可被绑定与检索。</p>
 */
public enum VectorStatus {

    /** 待处理（已入库排队） */
    PENDING,

    /** 向量化执行中 */
    PROCESSING,

    /** 向量化完成，可绑定检索 */
    COMPLETED,

    /** 向量化失败（vector_error 记录原因） */
    FAILED
}
