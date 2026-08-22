package interview.anticheat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.anticheat.model.entity.AntiCheatLog;
import interview.anticheat.model.req.AntiCheatEvidenceUploadTicketReq;
import interview.anticheat.model.req.AntiCheatLogSearchReq;
import interview.anticheat.model.vo.AntiCheatEvidencePresignVO;
import interview.anticheat.model.vo.AntiCheatEvidenceUploadTicketVO;
import interview.anticheat.model.vo.AntiCheatLogDetailVO;
import interview.anticheat.model.vo.AntiCheatLogListItemVO;

/**
 * @author zhuxi
 * @apiNote 防作弊服务（C 端证据上传凭证 + 平台端日志查询）
 */
public interface AntiCheatService extends IService<AntiCheatLog> {

    /**
     * 为当前候选人在指定会话签发防作弊证据短期上传凭证（校验会话参与者归属）
     * @param sessionId 面试会话 ID
     * @param req 上传凭证请求（事件 ID、文件元信息、SHA-256）
     * @return 证据材料令牌（对象键）与凭证有效期
     */
    AntiCheatEvidenceUploadTicketVO createEvidenceUploadTicket(
            Long sessionId, AntiCheatEvidenceUploadTicketReq req);

    /**
     * 平台端分页查询防作弊检测日志（按事件类型/用户/会话筛选）
     * @param req 分页与筛选条件
     * @return 日志分页
     */
    IPage<AntiCheatLogListItemVO> pageLogs(AntiCheatLogSearchReq req);

    /**
     * 平台端查询单条防作弊日志详情
     * @param logId 日志 ID
     * @return 日志详情
     */
    AntiCheatLogDetailVO getLogDetail(Long logId);

    /**
     * 平台端获取防作弊证据短期下载地址
     * @param logId 日志 ID
     * @return 短期预签名下载地址与有效期
     */
    AntiCheatEvidencePresignVO getEvidencePresign(Long logId);
}
