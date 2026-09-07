package interview.notification.content;

/**
 * 场景文案渲染结果。
 *
 * @param title 渲染后的消息标题
 * @param content 渲染后的消息正文
 */
public record RenderedContent(String title, String content) {
}
