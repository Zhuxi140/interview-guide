package interview.tutor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.tutor.model.req.TutorSessionCreateReq;
import interview.tutor.model.vo.TutorMessagePageVO;
import interview.tutor.model.vo.TutorSessionCreateVO;
import interview.tutor.model.vo.TutorSessionListItemVO;

/**
 * C 端 AI 答疑会话服务。
 *
 * @author zhuxi
 */
public interface AiTutorSessionService {

    /**
     * 基于本人已生成完成的面评报告创建答疑会话。
     *
     * @param req 创建请求
     * @return 创建结果
     */
    TutorSessionCreateVO createSession(TutorSessionCreateReq req);

    /**
     * 分页查询本人的答疑会话列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 会话分页结果
     */
    IPage<TutorSessionListItemVO> pageMySessions(Integer page, Integer size);

    /**
     * 游标查询会话内的答疑消息。
     *
     * @param sessionId 会话 ID
     * @param cursor    游标（上一页最后一条消息 ID），首页传 null
     * @param size      每页条数
     * @return 游标分页结果
     */
    TutorMessagePageVO listMessages(Long sessionId, Long cursor, Integer size);
}
