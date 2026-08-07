package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiSceneCode {
    RESUME_ANALYSIS("/prompts/resume-analysis/"),
    CANDIDATE_PROFILE_GENERATION("/prompts/candidate-profile/"),
    HR_APPLICATION_SCREENING("/prompts/hr-application-screening/"),
    CANDIDATE_JOB_MATCHING("/prompts/candidate-job-matching/"),
    INTERVIEW_PLAN_GENERATION("/prompts/interview-plan-generation/");
    // 对应阶段真正实现时再增加：
    // TEXT_INTERVIEW_EVALUATION,
    // VOICE_INTERVIEW_EVALUATION,
    // CODE_REVIEW,
    // RAG_CHAT

    private final String promptPath;


    public static String getPromptPath(AiSceneCode code) {
        for (AiSceneCode sceneCode : AiSceneCode.values()) {
            if (sceneCode == code) {
                return sceneCode.getPromptPath();
            }
        }
        throw new IllegalArgumentException("Unsupported AI scene code: " + code);
    }

}
