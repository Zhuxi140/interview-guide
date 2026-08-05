package interview.interviewcfg.model.enums;

import lombok.Getter;

/**
 * Agent 面试编排草案状态。
 */
@Getter
public enum InterviewPlanDraftStatus {

    /**
     * 待生成：草案刚创建，等待 Agent 开始生成。
     */
    PENDING("待生成"),

    /**
     * 生成中：Agent 正在生成面试编排草案。
     */
    PROCESSING("生成中"),

    /**
     * 已就绪：草案生成完成，可供预览或应用。
     */
    READY("已就绪"),

    /**
     * 已应用：草案已应用为正式面试计划。
     */
    APPLIED("已应用"),

    /**
     * 生成失败：Agent 生成草案过程中出错。
     */
    FAILED("生成失败"),

    /**
     * 已过期：草案超过有效期未被应用，自动失效。
     */
    EXPIRED("已过期");

    private final String msa;

    InterviewPlanDraftStatus(String msa) {
        this.msa = msa;
    }
}
