package interview.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CandidateDimensionCode {
    ACADEMIC_FOUNDATION("专业教育、课程、研究经历体现的知识基础"),
    TECHNICAL_DEPTH("技术掌握深度"),
    TECHNICAL_BREADTH("技术栈覆盖范围"),
    PROJECT_EXPERIENCE("项目经验:项目复杂度与参与程度"),
    PROFESSIONAL_EXPERIENCE("专业经验:工作经验、实习经历、科研经历等"),
    ENGINEERING_PRACTICE("工程实践: 测试、部署、性能、安全等工程能力"),
    BUSINESS_IMPACT("业务影响:业务成果、量化产出和外部认可"),
    LEARNING_GROWTH("学习成长: 持续学习和能力成长证据");
    private final String msg;
}