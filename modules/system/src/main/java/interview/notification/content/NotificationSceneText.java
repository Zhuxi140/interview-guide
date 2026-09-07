package interview.notification.content;

import interview.common.enums.ErrorCode;
import interview.common.enums.NotifyScene;
import interview.common.exception.BusinessException;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知场景文案与占位符渲染。
 *
 * <p>模板内变量以 {@code ${name}} 形式声明，白名单即模板中实际出现的变量集合：
 * 渲染时要求调用方提供的变量完整覆盖白名单，缺失一律抛出参数错误，
 * 不做"留空静默降级"，避免通知内容残缺外发。变量多于白名单时忽略多余项。</p>
 *
 * <p>L1 阶段文案内置在本枚举中；后续接入模板表时，本类的对外签名保持不变，
 * 仅将取文来源替换为模板查询。</p>
 */
public enum NotificationSceneText {

    /**
     * 面试邀请（首轮创建与重新安排后再次邀请共用）。
     */
    INTERVIEW_INVITE(NotifyScene.INTERVIEW_INVITE, "面试邀请",
            "您有一场新的面试安排：${interviewTime}，时长 ${durationMinutes} 分钟，请提前准备。"),

    /**
     * 面试取消。
     */
    INTERVIEW_CANCEL(NotifyScene.INTERVIEW_CANCEL, "面试取消",
            "您原定 ${interviewTime} 的面试已被取消，请留意后续安排。"),

    /**
     * Offer 已发送。
     */
    OFFER_SENT(NotifyScene.OFFER_SENT, "Offer 已发送",
            "您收到一笔新的 Offer，请登录平台查看详情并及时确认。"),

    /**
     * 候选人已对 Offer 做出决策。
     */
    OFFER_DECIDED(NotifyScene.OFFER_DECIDED, "Offer 决策通知",
            "候选人已对您的 Offer 做出决策：${decision}。"),

    /**
     * 面评报告生成完毕。
     */
    REPORT_READY(NotifyScene.REPORT_READY, "面评报告已生成",
            "您关注的面试面评报告已生成，请登录平台查看。"),

    /**
     * 系统通知：标题与正文完全由调用方提供。
     */
    SYSTEM(NotifyScene.SYSTEM, "${title}", "${content}");

    /**
     * 占位符匹配：${name}，name 限定为字母数字下划线。
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    private final NotifyScene scene;

    private final String titleTemplate;

    private final String contentTemplate;

    NotificationSceneText(NotifyScene scene, String titleTemplate, String contentTemplate) {
        this.scene = scene;
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
    }

    /**
     * 按场景渲染标题与正文（内置文案）。
     *
     * @param scene 业务场景
     * @param vars  模板变量；无变量场景允许传 null 或空集合
     * @return 渲染结果
     * @throws BusinessException 场景无对应文案或白名单变量缺失
     */
    public static RenderedContent render(NotifyScene scene, Map<String, String> vars) {
        // 场景与文案一一对应，未登记场景直接拒绝，防止漏配文案外发。
        NotificationSceneText text = from(scene);
        return renderTemplate(text.titleTemplate, text.contentTemplate, vars);
    }

    /**
     * 按给定模板渲染：占位符白名单即模板中实际出现的变量集合。
     *
     * <p>供场景内置文案与模板表渲染共用，保证"变量缺失显式失败"的一致语义。</p>
     *
     * @param titleTemplate   标题模板
     * @param contentTemplate 正文模板
     * @param vars            模板变量；模板无占位符时允许 null
     * @return 渲染结果
     * @throws BusinessException 白名单变量缺失
     */
    public static RenderedContent renderTemplate(String titleTemplate, String contentTemplate,
                                                 Map<String, String> vars) {
        Map<String, String> safeVars = vars == null ? Map.of() : vars;

        // 白名单校验：模板声明的变量必须由调用方完整提供且非空白。
        Set<String> required = extractPlaceholders(titleTemplate + "\n" + contentTemplate);
        for (String name : required) {
            String value = safeVars.get(name);
            if (value == null || value.isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知变量缺失或为空：" + name);
            }
        }
        return new RenderedContent(replace(titleTemplate, safeVars), replace(contentTemplate, safeVars));
    }

    /**
     * 查找场景对应的文案模板。
     */
    private static NotificationSceneText from(NotifyScene scene) {
        if (scene == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知场景不能为空");
        }
        for (NotificationSceneText text : values()) {
            if (text.scene == scene) {
                return text;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "未登记的通知场景：" + scene.name());
    }

    /**
     * 提取模板中声明的全部变量名，保持出现顺序。
     */
    private static Set<String> extractPlaceholders(String template) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * 执行占位符替换；白名单之外的变量直接忽略。
     */
    private static String replace(String template, Map<String, String> vars) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        Map<String, String> cache = new HashMap<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = cache.computeIfAbsent(name, vars::get);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
