package interview.notification.content;

import interview.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 场景文案渲染测试。
 */
class NotificationSceneTextTest {

    @Test
    void render_shouldReplaceAllPlaceholders_whenVarsComplete() {
        Map<String, String> vars = Map.of(
                "interviewTime", "2026-09-10 14:00",
                "durationMinutes", "60");

        RenderedContent content = NotificationSceneText.render(
                interview.common.enums.NotifyScene.INTERVIEW_INVITE, vars);

        assertEquals("面试邀请", content.title());
        assertEquals("您有一场新的面试安排：2026-09-10 14:00，时长 60 分钟，请提前准备。", content.content());
    }

    @Test
    void render_shouldThrow_whenRequiredVarMissing() {
        // 白名单变量缺失必须显式失败，禁止内容残缺外发。
        assertThrows(BusinessException.class, () -> NotificationSceneText.render(
                interview.common.enums.NotifyScene.INTERVIEW_INVITE,
                Map.of("interviewTime", "2026-09-10 14:00")));
    }

    @Test
    void render_shouldThrow_whenRequiredVarBlank() {
        assertThrows(BusinessException.class, () -> NotificationSceneText.render(
                interview.common.enums.NotifyScene.INTERVIEW_CANCEL,
                Map.of("interviewTime", " ")));
    }

    @Test
    void render_shouldIgnoreExtraVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("decision", "接受");
        vars.put("extra", "多余变量");

        RenderedContent content = NotificationSceneText.render(
                interview.common.enums.NotifyScene.OFFER_DECIDED, vars);

        assertEquals("Offer 决策通知", content.title());
        assertEquals("候选人已对您的 Offer 做出决策：接受。", content.content());
    }

    @Test
    void render_shouldPassWithoutVars_whenTemplateHasNoPlaceholder() {
        RenderedContent content = NotificationSceneText.render(
                interview.common.enums.NotifyScene.OFFER_SENT, null);

        assertEquals("Offer 已发送", content.title());
        assertEquals("您收到一笔新的 Offer，请登录平台查看详情并及时确认。", content.content());
    }

    @Test
    void render_shouldUseCallerTitleAndContent_forSystemScene() {
        RenderedContent content = NotificationSceneText.render(
                interview.common.enums.NotifyScene.SYSTEM,
                Map.of("title", "标题", "content", "正文"));

        assertEquals("标题", content.title());
        assertEquals("正文", content.content());
    }

    @Test
    void render_shouldThrow_whenSceneHasNoText() {
        assertThrows(BusinessException.class, () -> NotificationSceneText.render(null, null));
    }
}
