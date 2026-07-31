package interview.api.aicore;

import interview.api.aicore.dto.AiResumeAnalysisResult;

public interface ResumeAiApi {

    /**
     *  AI 简历分析
     * @param resumeText 简历文本
     * @return 分析结果
     */
    AiResumeAnalysisResult analysisResume(String resumeText);
}
