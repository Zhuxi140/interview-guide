package interview.offer.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.aicore.dto.JobApplicationSnapshotDTO;
import interview.api.bportal.InterviewFlowStatusApi;
import interview.api.bportal.JobValidationApi;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewFlowStatusEnum;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.offer.mapper.OfferMapper;
import interview.offer.model.bo.CandidateOfferQueryBO;
import interview.offer.model.entity.Offer;
import interview.offer.model.enums.OfferDecision;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Offer 写入与决策实现。
 */
@Service
@RequiredArgsConstructor
public class OfferServiceImpl extends ServiceImpl<OfferMapper, Offer> implements OfferService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final JobValidationApi jobValidationApi;
    private final InterviewFlowStatusApi interviewFlowStatusApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public OfferCreateVO createOffer(Long enterpriseId,
                                     Long applicationId,
                                     String idempotencyKey,
                                     OfferCreateReq req) {
        // ① 校验投递属于 enterpriseId 且未淘汰（PASSED/INTERVIEWING/OFFERED），
        //    返回候选人快照（candidateId 即候选人用户 ID，冗余写入 offers.candidate_user_id 供归属校验与查询）。
        JobApplicationSnapshotDTO snapshot = jobValidationApi.requirePassedApplicationWithInfo(
                applicationId, enterpriseId);

        // ② 面试完成门槛：全部轮次排期已完成或尚无排期才可创建 Offer，进行中/已淘汰抛 INTERVIEW_FLOW_NOT_COMPLETED。
        InterviewFlowStatusEnum flowStatus = interviewFlowStatusApi.getInterviewFlowStatus(
                applicationId, enterpriseId);
        if (flowStatus != InterviewFlowStatusEnum.INTERVIEW_COMPLETED
                && flowStatus != InterviewFlowStatusEnum.NO_SCHEDULE) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_COMPLETED);
        }

        // ③ 校验业务约束：薪资两值同时为空或同时非空且 max>=min（对应 ck_offers_salary）；currency 默认 CNY。
        validateSalaryPair(req.salaryMin(), req.salaryMax());
        String currency = req.currency() == null ? "CNY" : req.currency();

        // ④ 幂等预查：按 enterpriseId + createdBy + createIdempotencyKey（uk_offers_create_idempotency），
        //    命中即视为同一请求重放，返回原草稿结果。
        Offer replay = findCreateReplay(enterpriseId, idempotencyKey);
        if (replay != null) {
            return buildCreateVO(replay);
        }

        // ⑤ 组装 DRAFT Offer 并在同一本地事务内 insert；创建只保存录用邀请事实，不改投递与排期状态。
        Offer offer = Offer.builder()
                .enterpriseId(enterpriseId)
                .applicationId(applicationId)
                .candidateUserId(snapshot.candidateId())
                .title(req.title())
                .salaryMin(req.salaryMin())
                .salaryMax(req.salaryMax())
                .currency(currency)
                .plannedStartDate(req.plannedStartDate())
                .content(req.content())
                .expiresAt(req.expiresAt())
                .status(OfferStatus.DRAFT)
                .createIdempotencyKey(idempotencyKey)
                .version(0)
                .createdBy(AuthContext.getRequiredUserId())
                .build();
        try {
            baseMapper.insert(offer);
        } catch (DuplicateKeyException exception) {
            // 并发窗口由唯一索引兜底：同键重查重放；同投递已存在有效 Offer 报业务冲突。
            Offer concurrent = findCreateReplay(enterpriseId, idempotencyKey);
            if (concurrent != null) {
                return buildCreateVO(concurrent);
            }
            if (baseMapper.selectCount(Wrappers.<Offer>lambdaQuery()
                    .eq(Offer::getApplicationId, applicationId)) > 0) {
                throw new BusinessException(ErrorCode.OFFER_ALREADY_EXISTS);
            }
            throw exception;
        }

        // TODO [Notification] 事务提交后向企业操作人发送草稿创建通知（可靠消息/Outbox，失败重试不阻塞主流程）。

        // ⑥ 返回创建结果（offerId、applicationId、DRAFT、version、createdAt）。
        return buildCreateVO(offer);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public OfferUpdateVO updateOffer(Long enterpriseId, Long offerId, OfferUpdateReq req) {
        // ① 按 id + enterpriseId 查询 Offer（企业成员身份由 @RequireActiveEnterprise 统一保证），
        //    不存在或不属于该企业时抛 OFFER_NOT_FOUND。
        Offer offer = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getVersion,
                        Offer::getSalaryMin, Offer::getSalaryMax, Offer::getCurrency,
                        Offer::getExpiresAt)
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId));
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }

        // ② 状态机与乐观锁校验：仅 DRAFT 可修改，version 必须等于 expectedVersion。
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new BusinessException(ErrorCode.OFFER_STATUS_INVALID);
        }
        if (!req.expectedVersion().equals(offer.getVersion())) {
            throw new BusinessException(ErrorCode.OFFER_VERSION_CONFLICT);
        }

        // ③ 要求至少提交一个可更新字段，并复用创建接口的薪资组合与过期时间约束（按现有值合并后校验）。
        if (req.title() == null && req.salaryMin() == null && req.salaryMax() == null
                && req.currency() == null && req.plannedStartDate() == null
                && req.expiresAt() == null && req.content() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
        BigDecimal mergedSalaryMin = req.salaryMin() != null ? req.salaryMin() : offer.getSalaryMin();
        BigDecimal mergedSalaryMax = req.salaryMax() != null ? req.salaryMax() : offer.getSalaryMax();
        validateSalaryPair(mergedSalaryMin, mergedSalaryMax);

        // ④ 条件更新：id + enterpriseId + status=DRAFT + version=expectedVersion；
        //    实体更新按 NOT_NULL 策略仅覆盖非空字段，@Version 无乐观锁拦截器，需手动 set version+1；
        //    updatedBy/updatedAt/traceId 由实体更新自动填充。
        int newVersion = offer.getVersion() + 1;
        OffsetDateTime updatedAt = OffsetDateTime.now();
        Offer update = new Offer();
        update.setTitle(req.title());
        update.setSalaryMin(req.salaryMin());
        update.setSalaryMax(req.salaryMax());
        update.setCurrency(req.currency());
        update.setPlannedStartDate(req.plannedStartDate());
        update.setExpiresAt(req.expiresAt());
        update.setContent(req.content());
        update.setVersion(newVersion);
        int affected = baseMapper.update(update, Wrappers.<Offer>lambdaUpdate()
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getStatus, OfferStatus.DRAFT)
                .eq(Offer::getVersion, offer.getVersion()));

        // ⑤ 更新零行时重新读取归因：禁止覆盖并发发送/撤回/过期结果。
        if (affected != 1) {
            Offer latest = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                    .select(Offer::getId, Offer::getStatus, Offer::getVersion)
                    .eq(Offer::getId, offerId)
                    .eq(Offer::getEnterpriseId, enterpriseId));
            if (latest == null) {
                throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
            }
            if (latest.getStatus() != OfferStatus.DRAFT) {
                throw new BusinessException(ErrorCode.OFFER_STATUS_INVALID);
            }
            throw new BusinessException(ErrorCode.OFFER_VERSION_CONFLICT);
        }

        // TODO ⑥ 正文或薪资的历史版本如有合规要求，应另建审计快照表，不塞入 offers 当前行。

        // ⑦ 返回修改结果。
        return new OfferUpdateVO(offerId, OfferStatus.DRAFT, newVersion, updatedAt);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public OfferSendVO sendOffer(Long enterpriseId,
                                 Long offerId,
                                 String idempotencyKey,
                                 OfferSendReq req) {
        // ① 查询企业内 Offer，校验期望状态、版本与候选人联系方式/过期时间。
        Offer offer = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getVersion,
                        Offer::getExpiresAt, Offer::getSentAt,
                        Offer::getSendIdempotencyKey, Offer::getApplicationId)
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId));
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        if (req.expectedStatus() != offer.getStatus()) {
            throw new BusinessException(ErrorCode.OFFER_STATUS_INVALID);
        }
        if (!req.expectedVersion().equals(offer.getVersion())) {
            throw new BusinessException(ErrorCode.OFFER_VERSION_CONFLICT);
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.OFFER_EXPIRED);
        }

        // ② 幂等预查：按 enterpriseId + sendIdempotencyKey（uk_offers_send_idempotency），
        //    命中且状态为 SENT 时重放返回原 sentAt；同键被其它 Offer 占用视为幂等键冲突。
        Offer replay = findSendReplay(enterpriseId, idempotencyKey);
        if (replay != null) {
            if (replay.getId().equals(offerId)) {
                return buildSendVO(replay);
            }
            throw new BusinessException(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT);
        }

        // ③ 条件原子更新：offerId + DRAFT + version → SENT，写入 sentAt 与幂等键，仅一个并发操作能成功；
        //    审计字段由实体更新自动填充。
        OffsetDateTime sentAt = OffsetDateTime.now();
        int newVersion = offer.getVersion() + 1;
        Offer update = new Offer();
        update.setStatus(OfferStatus.SENT);
        update.setSentAt(sentAt);
        update.setSendIdempotencyKey(idempotencyKey);
        update.setVersion(newVersion);
        try {
            int affected = baseMapper.update(update, Wrappers.<Offer>lambdaUpdate()
                    .eq(Offer::getId, offerId)
                    .eq(Offer::getEnterpriseId, enterpriseId)
                    .eq(Offer::getStatus, OfferStatus.DRAFT)
                    .eq(Offer::getVersion, offer.getVersion()));
            if (affected != 1) {
                // ⑤ 更新零行：重读归因（重复发送/版本冲突/已撤回/已过期/已决策）。
                Offer latest = resolveSendConflict(enterpriseId, offerId, idempotencyKey);
                if (latest != null) {
                    return buildSendVO(latest);
                }
            }
        } catch (DuplicateKeyException exception) {
            // 并发请求撞 uk_offers_send_idempotency：按幂等键重查归因。
            Offer concurrent = findSendReplay(enterpriseId, idempotencyKey);
            if (concurrent != null) {
                if (concurrent.getId().equals(offerId)) {
                    return buildSendVO(concurrent);
                }
                throw new BusinessException(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT);
            }
            throw exception;
        }

        // TODO ④ 同一事务创建 Offer 通知 Outbox 消息，提交后派发；失败由消息租约与重试收敛，不回退 SENT。

        // ⑦ 投递状态 INTERVIEWING → OFFERED 回写（幂等，已处于 OFFERED 直接返回），作为招聘结果。
        jobValidationApi.markOffered(offer.getApplicationId(), enterpriseId);

        // ⑧ 返回发送结果。
        return new OfferSendVO(offerId, OfferStatus.SENT, newVersion, sentAt);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public OfferUpdateVO withdrawOffer(Long enterpriseId, Long offerId, OfferWithdrawReq req) {
        // ① 按 id + enterpriseId 查询 Offer（企业成员身份由 @RequireActiveEnterprise 统一保证）。
        Offer offer = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getVersion,
                        Offer::getExpiresAt)
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId));
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }

        // ② 状态机校验：仅 SENT 可撤回，DRAFT 未发送抛 OFFER_NOT_SENT_YET；
        //    其余终态（ACCEPTED/DECLINED/WITHDRAWN/EXPIRED）直接报对应业务冲突。
        if (offer.getStatus() == OfferStatus.DRAFT) {
            throw new BusinessException(ErrorCode.OFFER_NOT_SENT_YET);
        }
        if (req.expectedStatus() != offer.getStatus()) {
            throw new BusinessException(ErrorCode.OFFER_STATUS_INVALID);
        }
        if (!req.expectedVersion().equals(offer.getVersion())) {
            throw new BusinessException(ErrorCode.OFFER_VERSION_CONFLICT);
        }

        // ③ 过期预检；最终仍以条件更新与候选人决策、过期任务竞争为准。
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.OFFER_EXPIRED);
        }

        // ④ 条件原子更新：id + enterpriseId + status=SENT + version → WITHDRAWN；审计字段由实体更新自动填充。
        OffsetDateTime withdrawnAt = OffsetDateTime.now();
        int newVersion = offer.getVersion() + 1;
        Offer update = new Offer();
        update.setStatus(OfferStatus.WITHDRAWN);
        update.setWithdrawReason(req.reason());
        update.setWithdrawnAt(withdrawnAt);
        update.setVersion(newVersion);
        int affected = baseMapper.update(update, Wrappers.<Offer>lambdaUpdate()
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getStatus, OfferStatus.SENT)
                .eq(Offer::getVersion, offer.getVersion()));

        // ⑤ 更新零行时重新读取归因，区分并发决策/撤回/过期。
        if (affected != 1) {
            Offer latest = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                    .select(Offer::getId, Offer::getStatus)
                    .eq(Offer::getId, offerId)
                    .eq(Offer::getEnterpriseId, enterpriseId));
            if (latest == null) {
                throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
            }
            throw new BusinessException(classifyWithdrawConflict(latest.getStatus()));
        }

        // TODO ⑥ 同一事务记录撤回审计并创建候选人通知 Outbox（TODO [Notification]）。

        // ⑦ 返回撤回结果。
        return new OfferUpdateVO(offerId, OfferStatus.WITHDRAWN, newVersion, withdrawnAt);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public OfferDecisionVO decideOffer(Long offerId,
                                       String idempotencyKey,
                                       OfferDecisionReq req) {
        // ① 从 AuthContext 获取候选人 userId，按 offerId + candidateUserId 查询 Offer，
        //    查不到一律抛 OFFER_NOT_FOUND，不区分不存在与越权。
        Long candidateUserId = AuthContext.getRequiredUserId();
        Offer offer = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getExpiresAt,
                        Offer::getApplicationId, Offer::getEnterpriseId,
                        Offer::getDecisionIdempotencyKey, Offer::getDecidedAt)
                .eq(Offer::getId, offerId)
                .eq(Offer::getCandidateUserId, candidateUserId));
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }

        // ② 幂等预查：candidateUserId + decisionIdempotencyKey（uk_offers_decision_idempotency），
        //    命中且已决策时重放返回原 decidedAt；同键被其它 Offer 占用视为幂等键冲突。
        Offer replay = findDecisionReplay(candidateUserId, idempotencyKey);
        if (replay != null) {
            if (replay.getId().equals(offerId)) {
                return buildDecisionVO(replay);
            }
            throw new BusinessException(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT);
        }

        // ③ 状态校验：expectedStatus=SENT 且未过期。
        if (req.expectedStatus() != offer.getStatus()) {
            throw new BusinessException(ErrorCode.OFFER_STATUS_INVALID);
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.OFFER_EXPIRED);
        }

        // ④ 决策映射：ACCEPT → ACCEPTED，DECLINE → DECLINED；决策不校验版本号；
        //    审计字段由实体更新自动填充（updatedBy 即当前候选人 userId）。
        OfferStatus target = req.decision() == OfferDecision.ACCEPT
                ? OfferStatus.ACCEPTED
                : OfferStatus.DECLINED;
        OffsetDateTime decidedAt = OffsetDateTime.now();
        Offer update = new Offer();
        update.setStatus(target);
        update.setDecisionReason(req.reason());
        update.setDecidedAt(decidedAt);
        update.setDecisionIdempotencyKey(idempotencyKey);
        update.setVersion(offer.getVersion() + 1);
        try {
            int affected = baseMapper.update(update, Wrappers.<Offer>lambdaUpdate()
                    .eq(Offer::getId, offerId)
                    .eq(Offer::getCandidateUserId, candidateUserId)
                    .eq(Offer::getStatus, OfferStatus.SENT));
            if (affected != 1) {
                // ⑥ 更新零行：重读归因（同键重放/已决策/已撤回/已过期/冲突）。
                Offer latest = resolveDecisionConflict(candidateUserId, offerId, idempotencyKey);
                if (latest != null) {
                    return buildDecisionVO(latest);
                }
            }
        } catch (DuplicateKeyException exception) {
            // 并发决策撞 uk_offers_decision_idempotency：按幂等键重查归因。
            Offer concurrent = findDecisionReplay(candidateUserId, idempotencyKey);
            if (concurrent != null) {
                if (concurrent.getId().equals(offerId)) {
                    return buildDecisionVO(concurrent);
                }
                throw new BusinessException(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT);
            }
            throw exception;
        }

        // ⑦ 决策为 ACCEPT 时回写投递 OFFERED → HIRED（幂等），作为候选人接受 Offer 的招聘结果。
        if (req.decision() == OfferDecision.ACCEPT) {
            jobValidationApi.markHired(offer.getApplicationId(), offer.getEnterpriseId());
        }

        // TODO ⑧ 同一事务记录决策审计并创建企业通知 Outbox（TODO [Notification]）。

        // ⑨ 返回决策结果。
        return new OfferDecisionVO(offerId, target, decidedAt);
    }

    @Override
    public OfferHistoryVO listOffers(Long enterpriseId, Long applicationId) {
        // 按企业和投递双重限定查询，避免跨租户读取 Offer。
        List<OfferListItemVO> records = lambdaQuery()
                .select(
                        Offer::getId, Offer::getApplicationId,
                        Offer::getStatus, Offer::getVersion,
                        Offer::getSentAt, Offer::getDecidedAt,
                        Offer::getExpiresAt, Offer::getCreatedAt
                )
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getApplicationId, applicationId)
                .orderByDesc(Offer::getCreatedAt)
                .orderByDesc(Offer::getId)
                .list()
                .stream()
                .map(offer -> new OfferListItemVO(
                        offer.getId(), offer.getApplicationId(),
                        offer.getStatus(), offer.getVersion(),
                        offer.getSentAt(), offer.getDecidedAt(),
                        offer.getExpiresAt(), offer.getCreatedAt()
                ))
                .toList();
        return new OfferHistoryVO(records);
    }

    @Override
    public OfferDetailVO getOfferDetail(Long enterpriseId, Long offerId) {
        // 按 id + enterpriseId 查询 Offer 详情（企业成员身份由 @RequireActiveEnterprise 统一保证）。
        Offer offer = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId));
        if (offer == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        return new OfferDetailVO(
                offer.getId(), offer.getApplicationId(),
                offer.getTitle(), offer.getSalaryMin(), offer.getSalaryMax(),
                offer.getCurrency(), offer.getPlannedStartDate(), offer.getExpiresAt(),
                offer.getContent(), offer.getStatus(), offer.getVersion(),
                offer.getSentAt(), offer.getDecidedAt(),
                offer.getCreatedAt(), offer.getUpdatedAt()
        );
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

    private void validateSalaryPair(BigDecimal salaryMin, BigDecimal salaryMax) {
        // 薪资两值必须同时为空或同时非空且 max>=min（对应 ck_offers_salary）。
        if ((salaryMin == null) != (salaryMax == null)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
        if (salaryMin != null && salaryMax.compareTo(salaryMin) < 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
    }

    private Offer findCreateReplay(Long enterpriseId, String idempotencyKey) {
        return baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getApplicationId, Offer::getStatus,
                        Offer::getVersion, Offer::getCreatedAt)
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getCreatedBy, AuthContext.getRequiredUserId())
                .eq(Offer::getCreateIdempotencyKey, idempotencyKey));
    }

    private OfferCreateVO buildCreateVO(Offer offer) {
        return new OfferCreateVO(
                offer.getId(), offer.getApplicationId(),
                offer.getStatus(), offer.getVersion(), offer.getCreatedAt());
    }

    private Offer findSendReplay(Long enterpriseId, String idempotencyKey) {
        return baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getVersion, Offer::getSentAt)
                .eq(Offer::getEnterpriseId, enterpriseId)
                .eq(Offer::getSendIdempotencyKey, idempotencyKey)
                .eq(Offer::getStatus, OfferStatus.SENT));
    }

    private OfferSendVO buildSendVO(Offer offer) {
        return new OfferSendVO(offer.getId(), offer.getStatus(), offer.getVersion(), offer.getSentAt());
    }

    /**
     * 发送更新零行后重新读取归因。
     * @return 可重放的行（同键同 Offer 已被并发发送成功），否则抛业务冲突异常
     */
    private Offer resolveSendConflict(Long enterpriseId, Long offerId, String idempotencyKey) {
        Offer latest = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getVersion,
                        Offer::getSentAt, Offer::getSendIdempotencyKey)
                .eq(Offer::getId, offerId)
                .eq(Offer::getEnterpriseId, enterpriseId));
        if (latest == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        if (latest.getStatus() == OfferStatus.SENT
                && idempotencyKey.equals(latest.getSendIdempotencyKey())) {
            return latest;
        }
        throw new BusinessException(classifySendConflict(latest.getStatus()));
    }

    private ErrorCode classifySendConflict(OfferStatus status) {
        return switch (status) {
            case ACCEPTED, DECLINED -> ErrorCode.OFFER_ALREADY_DECIDED;
            case WITHDRAWN -> ErrorCode.OFFER_WITHDRAWN;
            case EXPIRED -> ErrorCode.OFFER_EXPIRED;
            default -> ErrorCode.OFFER_VERSION_CONFLICT;
        };
    }

    private Offer findDecisionReplay(Long candidateUserId, String idempotencyKey) {
        return baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(Offer::getId, Offer::getStatus, Offer::getDecidedAt)
                .eq(Offer::getCandidateUserId, candidateUserId)
                .eq(Offer::getDecisionIdempotencyKey, idempotencyKey)
                .in(Offer::getStatus, OfferStatus.ACCEPTED, OfferStatus.DECLINED));
    }

    private OfferDecisionVO buildDecisionVO(Offer offer) {
        return new OfferDecisionVO(offer.getId(), offer.getStatus(), offer.getDecidedAt());
    }

    /**
     * 决策更新零行后重新读取归因。
     * @return 可重放的行（同键已被并发决策成功），否则抛业务冲突异常
     */
    private Offer resolveDecisionConflict(Long candidateUserId, Long offerId, String idempotencyKey) {
        Offer latest = baseMapper.selectOne(Wrappers.<Offer>lambdaQuery()
                .select(
                        Offer::getId, Offer::getStatus, Offer::getDecidedAt,
                        Offer::getDecisionIdempotencyKey)
                .eq(Offer::getId, offerId)
                .eq(Offer::getCandidateUserId, candidateUserId));
        if (latest == null) {
            throw new BusinessException(ErrorCode.OFFER_NOT_FOUND);
        }
        if ((latest.getStatus() == OfferStatus.ACCEPTED || latest.getStatus() == OfferStatus.DECLINED)
                && idempotencyKey.equals(latest.getDecisionIdempotencyKey())) {
            return latest;
        }
        throw new BusinessException(classifyDecisionConflict(latest.getStatus()));
    }

    private ErrorCode classifyDecisionConflict(OfferStatus status) {
        return switch (status) {
            case ACCEPTED, DECLINED -> ErrorCode.OFFER_ALREADY_DECIDED;
            case WITHDRAWN -> ErrorCode.OFFER_WITHDRAWN;
            case EXPIRED -> ErrorCode.OFFER_EXPIRED;
            default -> ErrorCode.OFFER_DECISION_CONFLICT;
        };
    }

    private ErrorCode classifyWithdrawConflict(OfferStatus status) {
        return switch (status) {
            case ACCEPTED, DECLINED -> ErrorCode.OFFER_ALREADY_DECIDED;
            case WITHDRAWN -> ErrorCode.OFFER_WITHDRAWN;
            case EXPIRED -> ErrorCode.OFFER_EXPIRED;
            default -> ErrorCode.OFFER_VERSION_CONFLICT;
        };
    }
}
