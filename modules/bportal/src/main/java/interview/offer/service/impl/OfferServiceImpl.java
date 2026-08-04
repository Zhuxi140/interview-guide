package interview.offer.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.offer.mapper.OfferMapper;
import interview.offer.model.bo.CandidateOfferQueryBO;
import interview.offer.model.entity.Offer;
import interview.offer.model.enums.OfferStatus;
import interview.offer.model.req.OfferCreateReq;
import interview.offer.model.req.OfferDecisionReq;
import interview.offer.model.req.OfferSendReq;
import interview.offer.model.req.OfferUpdateReq;
import interview.offer.model.req.OfferWithdrawReq;
import interview.offer.model.vo.CandidateOfferDetailVO;
import interview.offer.model.vo.CandidateOfferListItemVO;
import interview.offer.model.vo.OfferCreateVO;
import interview.offer.model.vo.OfferDecisionVO;
import interview.offer.model.vo.OfferDetailVO;
import interview.offer.model.vo.OfferHistoryVO;
import interview.offer.model.vo.OfferListItemVO;
import interview.offer.model.vo.OfferSendVO;
import interview.offer.model.vo.OfferUpdateVO;
import interview.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Offer 写入与决策实现。
 */
@Service
@RequiredArgsConstructor
public class OfferServiceImpl extends ServiceImpl<OfferMapper, Offer> implements OfferService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public OfferCreateVO createOffer(Long enterpriseId,
                                     Long applicationId,
                                     String idempotencyKey,
                                     OfferCreateReq req) {
        // TODO ① 从 AuthContext 获取企业操作人，通过 EnterpriseValidationApi 校验企业成员身份，并校验 Idempotency-Key 非空。
        // TODO ② 通过 JobValidationApi.requirePassedApplicationWithInfo 校验投递属于 enterpriseId 且候选人已完成面试，
        //         取得候选人快照（DTO 的 candidateId 即候选人用户 ID，冗余写入 offers.candidate_user_id 供归属校验与查询）。
        // TODO ③ 校验业务约束：薪资两值必须同时为空或同时非空且 max>=min（对应 ck_offers_salary）；
        //         currency 为空时默认 CNY；expiresAt 必须晚于当前时间（@Future + ck_offers_expiration 兜底）。
        // TODO ④ 幂等预查：按 enterpriseId + createdBy + createIdempotencyKey 查询（对应 uk_offers_create_idempotency），
        //         命中即视为同一请求重放，返回原草稿的 offerId/applicationId/status/version/createdAt。
        // TODO ⑤ 组装 DRAFT Offer：version=0、createdBy/updatedBy/traceId 审计字段，同一本地事务内 insert；
        //         并发窗口由唯一索引兜底——uk_offers_create_idempotency 冲突时重查并重放返回原草稿，
        //         uk_offers_application_open_or_accepted 冲突时抛 OFFER_ALREADY_EXISTS（同一投递已有 DRAFT/SENT/ACCEPTED）。
        // TODO ⑥ 事务提交后如接入通知，向企业操作人发送草稿创建通知（TODO [Notification]，可靠消息/Outbox，失败重试不阻塞主流程）。
        // TODO ⑦ Offer 仅保存录用邀请事实，不修改 job_applications.status 或 interview_schedule.status。
        // TODO ⑧ 返回 OfferCreateVO（offerId、applicationId、DRAFT、初始 version、createdAt）。
        return null;
    }

    @Override
    public OfferUpdateVO updateOffer(Long enterpriseId, Long offerId, OfferUpdateReq req) {
        // TODO ① 从 AuthContext 获取企业操作人，通过 EnterpriseValidationApi 校验企业成员身份；
        //         按 id + enterpriseId 查询 Offer，不存在或不属于该企业时抛 OFFER_NOT_FOUND。
        // TODO ② 状态机校验：仅 DRAFT 可修改（OFFER_STATUS_INVALID）；
        //         乐观锁校验：version 必须等于 req.expectedVersion（OFFER_VERSION_CONFLICT）。
        // TODO ③ 要求至少提交一个可更新字段（title/salaryMin/salaryMax/currency/plannedStartDate/expiresAt/content 全空时
        //         抛 PARAM_VALID_ERROR），并复用创建接口的薪资组合、币种、日期和过期时间约束。
        // TODO ④ 条件更新：id + enterpriseId + status=DRAFT + version=expectedVersion（注意 @Version 无乐观锁拦截器，
        //         必须手动拼接 version 条件并在 SET 中置 version+1），仅覆盖非空字段；
        //         updatedBy/traceId/updatedAt 为 LambdaUpdate 非实体自动填充，必须显式 set。
        // TODO ⑤ 更新零行时重新读取归因：Offer 不存在 → OFFER_NOT_FOUND；状态已变（并发发送/撤回/过期）→ OFFER_STATUS_INVALID；
        //         DRAFT 但版本不同 → OFFER_VERSION_CONFLICT；禁止覆盖并发发送结果。
        // TODO ⑥ 正文或薪资的历史版本如有合规要求，应另建审计快照表，不塞入 offers 当前行。
        // TODO ⑦ 返回 OfferUpdateVO（offerId、DRAFT、新 version、updatedAt）。
        return null;
    }

    @Override
    public OfferSendVO sendOffer(Long enterpriseId,
                                 Long offerId,
                                 String idempotencyKey,
                                 OfferSendReq req) {
        // TODO ① 查询企业内 Offer，校验 expectedStatus=DRAFT、expectedVersion、候选人联系方式和过期时间仍然有效。
        // TODO ② 按 enterpriseId + offerId + 操作人 + Idempotency-Key 查询发送请求；成功重放返回原 sentAt。
        // TODO ③ 在事务内按 offerId + DRAFT + version 条件更新为 SENT、递增 version、写入 sentAt 和幂等请求记录。
        // TODO ④ 同一事务创建 Offer 通知 Outbox 消息；不能先发送通知再更新状态，也不能远程写中央消息表模拟本地事务。
        // TODO ⑤ 更新零行时区分重复发送、版本冲突、Offer 已撤销和过期；相同幂等请求不得重复通知。
        // TODO ⑥ 事务提交后立即派发通知，邮件/短信失败由消息租约和重试机制收敛，不回退 SENT 业务事实。
        // TODO ⑦ 返回 offerId、SENT、新 version 和 sentAt。
        return null;
    }

    @Override
    public OfferUpdateVO withdrawOffer(Long enterpriseId, Long offerId, OfferWithdrawReq req) {
        // TODO ① 从 AuthContext 获取企业操作人，通过 EnterpriseValidationApi 校验企业成员身份；
        //         按 id + enterpriseId 查询 Offer，不存在或不属于该企业时抛 OFFER_NOT_FOUND。
        // TODO ② 校验 req.expectedStatus 与当前状态一致（OFFER_STATUS_INVALID），且仅 SENT 可撤回
        //         （DRAFT 未发送抛 OFFER_NOT_SENT_YET）；乐观锁校验 version=req.expectedVersion（OFFER_VERSION_CONFLICT）。
        // TODO ③ 执行更新前预检 Offer 未过期（OFFER_EXPIRED），但过期判定最终仍须依赖条件更新与候选人决策、过期任务竞争。
        // TODO ④ 条件原子更新：id + enterpriseId + status=SENT + version → WITHDRAWN + withdrawReason + withdrawnAt +
        //         version+1，仅有一个并发操作（撤回/候选人决策/过期任务）能成功；withdrawReason 规范化后写入，上限 256 字符。
        // TODO ⑤ 更新零行时重新读取归因：不存在 → OFFER_NOT_FOUND；ACCEPTED/DECLINED → OFFER_ALREADY_DECIDED；
        //         WITHDRAWN → OFFER_WITHDRAWN；EXPIRED → OFFER_EXPIRED；其余状态或版本变化 → OFFER_VERSION_CONFLICT。
        // TODO ⑥ 同一事务记录撤回审计（withdrawnAt/updatedBy/traceId 显式 set）并创建候选人通知 Outbox；
        //         事务提交后派发通知，失败由消息重试收敛（TODO [Notification]）。
        // TODO ⑦ 返回 OfferUpdateVO（offerId、WITHDRAWN、新 version、updatedAt）。
        return null;
    }

    @Override
    public OfferDecisionVO decideOffer(Long offerId,
                                       String idempotencyKey,
                                       OfferDecisionReq req) {
        // TODO ① 从 AuthContext 获取候选人 userId，按 offerId + candidateUserId 查询 Offer；
        //         查不到一律抛 OFFER_NOT_FOUND，不区分不存在与越权，禁止读取或操作他人 Offer。
        // TODO ② 校验 Idempotency-Key 非空；幂等预查：candidateUserId + decisionIdempotencyKey
        //         （对应 uk_offers_decision_idempotency），命中且已决策时重放返回原 decidedAt。
        // TODO ③ 状态校验：expectedStatus=SENT（OFFER_STATUS_INVALID）、未过期（OFFER_EXPIRED）。
        // TODO ④ 将 ACCEPT/DECLINE 映射为 ACCEPTED/DECLINED，decisionReason 按产品规则决定是否必填并限制 256 字符。
        // TODO ⑤ 条件原子更新：id + candidateUserId + status=SENT → 目标状态 + decisionReason + decidedAt +
        //         decisionIdempotencyKey + version+1，与 HR 撤回、过期任务竞争，只有一个操作成功；决策不校验版本号。
        // TODO ⑥ 更新零行时重新读取归因：同键已决策 → 重放返回原 decidedAt；ACCEPTED/DECLINED → OFFER_ALREADY_DECIDED；
        //         WITHDRAWN → OFFER_WITHDRAWN；EXPIRED → OFFER_EXPIRED；其余 → OFFER_DECISION_CONFLICT。
        // TODO ⑦ 同一事务记录决策审计并创建企业通知 Outbox（TODO [Notification]）；接受 Offer 不回写
        //         job_applications.status 或 interview_schedule.status，综合看板通过资源关联聚合展示。
        // TODO ⑧ 返回 OfferDecisionVO（offerId、ACCEPTED/DECLINED、decidedAt）。
        return null;
    }

    @Override
    public OfferHistoryVO listOffers(Long enterpriseId, Long applicationId) {
        // 按企业和投递双重限定查询，避免跨租户读取 Offer。
        List<OfferListItemVO> records = lambdaQuery()
                .select(
                        Offer::getId,
                        Offer::getApplicationId,
                        Offer::getStatus,
                        Offer::getVersion,
                        Offer::getSentAt,
                        Offer::getDecidedAt,
                        Offer::getExpiresAt,
                        Offer::getCreatedAt
                )
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getApplicationId, applicationId)
                .orderByDesc(Offer::getCreatedAt)
                .orderByDesc(Offer::getId)
                .list()
                .stream()
                .map(offer -> new OfferListItemVO(
                        offer.getId(),
                        offer.getApplicationId(),
                        offer.getStatus(),
                        offer.getVersion(),
                        offer.getSentAt(),
                        offer.getDecidedAt(),
                        offer.getExpiresAt(),
                        offer.getCreatedAt()
                ))
                .toList();
        return new OfferHistoryVO(records);
    }

    @Override
    public OfferDetailVO getOfferDetail(Long enterpriseId, Long offerId) {
        // TODO ① 校验 enterpriseId 归属，按 enterpriseId + offerId 查询 Offer 详情。
        // TODO ② 映射为 OfferDetailVO 返回（含所有字段：title、salaryMin/Max、currency、plannedStartDate、expiresAt、content、status、version、sentAt、decidedAt、createdAt、updatedAt）。
        return null;
    }

    @Override
    public IPage<CandidateOfferListItemVO> pageCandidateOffers(Integer page,
                                                                Integer size,
                                                                OfferStatus status,
                                                                String sort,
                                                                String order) {
        // 校验查询边界，候选人不可通过筛选读取尚未发送的草稿。
        validatePage(page, size);
        if (!"createdAt".equals(sort)) {
            throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
        validateOrder(order);
        Long userId = AuthContext.getRequiredUserId();

        // 在 Offer 所属模块一次关联投递和岗位，企业名称通过 system API 批量补充。
        IPage<CandidateOfferQueryBO> boPage = baseMapper.pageCandidateOffers(
                new Page<>(page, size),
                userId,
                status,
                "asc".equalsIgnoreCase(order)
        );
        List<Long> enterpriseIds = boPage.getRecords().stream()
                .map(CandidateOfferQueryBO::enterpriseId)
                .distinct()
                .toList();
        Map<Long, String> enterpriseNames = enterpriseIds.isEmpty()
                ? Map.of()
                : enterpriseValidationApi.getNameList(enterpriseIds);
        return boPage.convert(offer -> new CandidateOfferListItemVO(
                offer.id(),
                enterpriseNames.get(offer.enterpriseId()),
                offer.jobTitle(),
                offer.status(),
                offer.expiresAt(),
                offer.createdAt()
        ));
    }

    @Override
    public CandidateOfferDetailVO getCandidateOfferDetail(Long offerId) {
        // 查询条件同时绑定当前候选人，并排除尚未发送的 DRAFT。
        CandidateOfferQueryBO offer = baseMapper.getCandidateOfferDetail(
                offerId, AuthContext.getRequiredUserId());
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        Map<Long, String> enterpriseNames = enterpriseValidationApi.getNameList(
                List.of(offer.enterpriseId()));
        return new CandidateOfferDetailVO(
                offer.id(),
                enterpriseNames.get(offer.enterpriseId()),
                offer.jobTitle(),
                offer.offerTitle(),
                offer.salaryMin(),
                offer.salaryMax(),
                offer.currency(),
                offer.plannedStartDate(),
                offer.expiresAt(),
                offer.content(),
                offer.status(),
                offer.version(),
                offer.sentAt()
        );
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateOrder(String order) {
        if (!"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCode.SORT_DIRECTION_INVALID);
        }
    }
}
