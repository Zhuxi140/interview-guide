package interview.ai.planDraft.tool;


import cn.hutool.json.JSONUtil;
import interview.api.aicore.dto.AiInterviewPlanInput;
import interview.api.system.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class InterviewPlanAgentTools {

    private final UserApi userApi;
    private final AiInterviewPlanInput input;

    @Tool(description = "获取面试模板的阶段配置，包含每个阶段的编码、名称、序号、题目数量、难度权重。阶段集合与顺序必须以此为准。")
    public String getTemplateStages(){
        return parseJsonField(input.inputSnapshotJson(), "stages");
    }

    @Tool(description = "获取 HR 的编排请求参数，包含总时长、最大轮数、指定阶段编码列表、候选面试官用户ID集合、补充提示词等。")
    public String getHrRequest() {
        return parseJsonField(input.requestJson(), null);
    }

    @Tool(description = "根据用户ID列表批量获取面试官姓名，返回 ID 到姓名的映射 JSON。")
    public String getInterviewerNames(List<Long> userIds) {
        try {
            return JSONUtil.toJsonStr(userApi.getUserNamesByIds(userIds));
        } catch (Exception e) {
            return "null";
        }
    }

    @Tool(description = "按权重把总时长分配到各阶段。totalMinutes 为总时长，weights 为与阶段顺序一一对应的整数权重数组，返回拆分后各阶段时长的 JSON 数组（向下取整，余量补到第一个阶段）。")
    public String calculateStageDurations(int totalMinutes, List<Integer> weights){
        if (weights == null || weights.isEmpty() || totalMinutes <= 0) {
            return "null";
        }
        int sum = weights.stream().mapToInt(Integer::intValue).sum();
        if (sum <= 0) {
            return "null";
        }
        List<Integer> durations = new ArrayList<>(weights.size());
        int allocated = 0;
        for (int i = 0; i < weights.size(); i++) {
            int d = i == 0 ? totalMinutes * weights.get(i) / sum
                    : (i == weights.size() - 1 ? totalMinutes - allocated
                    : totalMinutes * weights.get(i) / sum);
            allocated += d;
            durations.add(d);
        }
        return JSONUtil.toJsonStr(durations);
    }



    private String parseJsonField(String json, String field) {
        try {
            var root = JSONUtil.parseObj(json);
            var node = field == null ? root : root.get(field);
            return node == null ? "null" : node.toString();
        } catch (Exception e) {
            return "null";
        }
    }
}
