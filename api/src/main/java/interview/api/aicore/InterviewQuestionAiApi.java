package interview.api.aicore;

import interview.api.aicore.dto.InterviewQuestionGeneratedResultDTO;
import interview.api.aicore.dto.InterviewQuestionGenerationReqDTO;

/**
 * 面试 AI 出题与题目生成跨模块接口
 */
public interface InterviewQuestionAiApi {

    /**
     * 根据考点配置、岗位要求及候选人背景生成一道结构化试题
     *
     * @param req 出题请求参数
     * @return 生成的试题结果
     */
    InterviewQuestionGeneratedResultDTO generateQuestion(InterviewQuestionGenerationReqDTO req);
}
