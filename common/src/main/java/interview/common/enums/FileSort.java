package interview.common.enums;

/**
 * @author zhuxi
 * @apiNote 文件分类（用于 RustFS 存储路径隔离）
 */
public enum FileSort {
    /**
     * 简历文件（pdf/doc/docx）
     */
    RESUME,
    /**
     * 知识库文档
     */
    KNOWLEDGE_DOC,
    /**
     * 用户头像
     */
    AVATAR,
    /**
     * 企业 Logo
     */
    LOGO,
    /**
     * 实名认证身份证件
     */
    KYC_ID_CARD,
    /**
     * 企业资质认证文件（营业执照等）
     */
    ENTERPRISE_CERT,
    /**
     * 面试评估报告 PDF
     */
    INTERVIEW_REPORT,
    /**
     * 通用附件兜底
     */
    ATTACHMENT,
}
