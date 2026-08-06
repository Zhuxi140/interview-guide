package interview.offer.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.aicore.dto.JobApplicationSnapshotDTO;
import interview.api.bportal.InterviewFlowStatusApi;
import interview.api.bportal.JobValidationApi;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewFlowStatusEnum;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.offer.mapper.OfferMapper;
import interview.offer.model.entity.Offer;
import interview.offer.model.enums.OfferDecision;
import interview.offer.model.enums.OfferStatus;
import interview.offer.model.req.OfferCreateReq;
import interview.offer.model.req.OfferDecisionReq;
import interview.offer.model.req.OfferSendReq;
import interview.offer.model.req.OfferUpdateReq;
import interview.offer.model.req.OfferWithdrawReq;
import interview.offer.model.vo.OfferCreateVO;
import interview.offer.model.vo.OfferDecisionVO;
import interview.offer.model.vo.OfferDetailVO;
import interview.offer.model.vo.OfferSendVO;
import interview.offer.model.vo.OfferUpdateVO;
import interview.offer.service.impl.OfferServiceImpl;
import interview.job.service.JobService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

    private static final long ENTERPRISE_ID = 10L;
    private static final long APPLICATION_ID = 20L;
    private static final long OFFER_ID = 36001L;
    private static final long CANDIDATE_USER_ID = 5001L;
    private static final String CREATE_KEY = "create-key-1";
    private static final String SEND_KEY = "send-key-1";
    private static final String DECISION_KEY = "decision-key-1";

    @Mock
    private OfferMapper mapper;
    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;
    @Mock
    private JobValidationApi jobValidationApi;
    @Mock
    private InterviewFlowStatusApi interviewFlowStatusApi;
    @Mock
    private JobService jobService;

    private OfferServiceImpl service;

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使 lambda select 能解析列名（Spring 启动时会自动完成，单测需手动）。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "Offer"),
                Offer.class);
        service = spy(new OfferServiceImpl(
                enterpriseValidationApi, jobValidationApi, interviewFlowStatusApi, jobService));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(1L)
                .userType(UserType.ENTERPRISE_USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    private Offer createReq() {
        return Offer.builder()
                .id(OFFER_ID)
                .enterpriseId(ENTERPRISE_ID)
                .applicationId(APPLICATION_ID)
                .candidateUserId(CANDIDATE_USER_ID)
                .title("Java后端工程师录用通知")
                .salaryMin(new BigDecimal("18000"))
                .salaryMax(new BigDecimal("25000"))
                .currency("CNY")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .content("恭喜通过面试")
                .status(OfferStatus.DRAFT)
                .version(3)
                .createIdempotencyKey(CREATE_KEY)
                .createdBy(1L)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Offer sentRow(OfferStatus status) {
        Offer offer = createReq();
        offer.setStatus(status);
        offer.setSentAt(OffsetDateTime.now());
        offer.setSendIdempotencyKey(SEND_KEY);
        return offer;
    }

    private void stubSnapshot() {
        doReturn(new JobApplicationSnapshotDTO(ENTERPRISE_ID, 30L, CANDIDATE_USER_ID))
                .when(jobValidationApi).requirePassedApplicationWithInfo(APPLICATION_ID, ENTERPRISE_ID);
        // lenient：单个用例覆盖面试流程结果时，该共享 stub 不必被实际命中。
        lenient().when(interviewFlowStatusApi.getInterviewFlowStatus(APPLICATION_ID, ENTERPRISE_ID))
                .thenReturn(InterviewFlowStatusEnum.INTERVIEW_COMPLETED);
    }

    private OfferCreateReq createOfferReq() {
        return new OfferCreateReq(
                "Java后端工程师录用通知",
                new BigDecimal("18000"),
                new BigDecimal("25000"),
                null,
                null,
                OffsetDateTime.now().plusDays(7),
                "恭喜通过面试");
    }

    @Test
    void createOffer_shouldCreateDraftWhenApplicationOffered() {
        stubSnapshot();
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId(OFFER_ID);
            offer.setCreatedAt(OffsetDateTime.now());
            return 1;
        });

        OfferCreateVO result = service.createOffer(
                ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq());

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(APPLICATION_ID, result.applicationId());
        assertEquals(OfferStatus.DRAFT, result.status());
        assertEquals(0, result.version());
        assertNotNull(result.createdAt());

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(mapper).insert(captor.capture());
        Offer inserted = captor.getValue();
        assertEquals(ENTERPRISE_ID, inserted.getEnterpriseId());
        assertEquals(APPLICATION_ID, inserted.getApplicationId());
        assertEquals(CANDIDATE_USER_ID, inserted.getCandidateUserId());
        assertEquals("CNY", inserted.getCurrency());
        assertEquals(0, inserted.getVersion());
        assertEquals(CREATE_KEY, inserted.getCreateIdempotencyKey());
        assertEquals(1L, inserted.getCreatedBy());
        assertEquals(OfferStatus.DRAFT, inserted.getStatus());
    }

    @Test
    void createOffer_shouldReplayWhenSameIdempotencyKey() {
        stubSnapshot();
        Offer replayed = createReq();
        replayed.setId(OFFER_ID);
        replayed.setStatus(OfferStatus.DRAFT);
        replayed.setVersion(0);
        when(mapper.selectOne(any())).thenReturn(replayed);

        OfferCreateVO result = service.createOffer(
                ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq());

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(APPLICATION_ID, result.applicationId());
        assertEquals(OfferStatus.DRAFT, result.status());
        assertEquals(0, result.version());
        verify(mapper, never()).insert(any(Offer.class));
    }

    @Test
    void createOffer_shouldRejectDuplicateApplicationOffer() {
        stubSnapshot();
        when(mapper.selectOne(any())).thenReturn(null);
        doThrow(new DuplicateKeyException("duplicate key")).when(mapper).insert(any(Offer.class));
        when(mapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOffer(ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq()));

        assertEquals(ErrorCode.OFFER_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void createOffer_shouldReplayConcurrentSameKeyOnDuplicate() {
        stubSnapshot();
        Offer replayed = createReq();
        replayed.setId(OFFER_ID);
        when(mapper.selectOne(any())).thenReturn(null).thenReturn(replayed);
        doThrow(new DuplicateKeyException("duplicate key")).when(mapper).insert(any(Offer.class));

        OfferCreateVO result = service.createOffer(
                ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq());

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(replayed.getVersion(), result.version());
    }

    @Test
    void createOffer_shouldRejectSalaryMismatch() {
        stubSnapshot();
        OfferCreateReq badReq = new OfferCreateReq(
                "标题", new BigDecimal("18000"), null, null, null,
                OffsetDateTime.now().plusDays(7), "内容");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOffer(ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, badReq));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
        verify(mapper, never()).insert(any(Offer.class));
    }

    @Test
    void createOffer_shouldRejectWhenInterviewInProgress() {
        stubSnapshot();
        doReturn(InterviewFlowStatusEnum.IN_PROGRESS)
                .when(interviewFlowStatusApi).getInterviewFlowStatus(APPLICATION_ID, ENTERPRISE_ID);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOffer(ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq()));

        assertEquals(ErrorCode.INTERVIEW_FLOW_NOT_COMPLETED.getCode(), exception.getCode());
        verify(mapper, never()).insert(any(Offer.class));
    }

    @Test
    void createOffer_shouldRejectWhenInterviewTerminal() {
        stubSnapshot();
        doReturn(InterviewFlowStatusEnum.REJECTED_TERMINAL)
                .when(interviewFlowStatusApi).getInterviewFlowStatus(APPLICATION_ID, ENTERPRISE_ID);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOffer(ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq()));

        assertEquals(ErrorCode.INTERVIEW_FLOW_NOT_COMPLETED.getCode(), exception.getCode());
    }

    @Test
    void createOffer_shouldAllowWhenNoSchedule() {
        stubSnapshot();
        doReturn(InterviewFlowStatusEnum.NO_SCHEDULE)
                .when(interviewFlowStatusApi).getInterviewFlowStatus(APPLICATION_ID, ENTERPRISE_ID);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId(OFFER_ID);
            offer.setCreatedAt(OffsetDateTime.now());
            return 1;
        });

        OfferCreateVO result = service.createOffer(
                ENTERPRISE_ID, APPLICATION_ID, CREATE_KEY, createOfferReq());

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.DRAFT, result.status());
    }

    @Test
    void updateOffer_shouldUpdatePartialFields() {
        when(mapper.selectOne(any())).thenReturn(createReq());
        when(mapper.update(any(), any())).thenReturn(1);

        OfferUpdateReq req = new OfferUpdateReq(
                3, "新标题", null, null, null, null, null, null);
        OfferUpdateVO result = service.updateOffer(ENTERPRISE_ID, OFFER_ID, req);

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.DRAFT, result.status());
        assertEquals(4, result.version());
        assertNotNull(result.updatedAt());
        verify(mapper).update(any(Offer.class), any());
    }

    @Test
    void updateOffer_shouldRejectWhenStatusNotDraft() {
        Offer offer = createReq();
        offer.setStatus(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(offer);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOffer(ENTERPRISE_ID, OFFER_ID,
                        new OfferUpdateReq(3, "新标题", null, null, null, null, null, null)));

        assertEquals(ErrorCode.OFFER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void updateOffer_shouldRejectVersionMismatch() {
        when(mapper.selectOne(any())).thenReturn(createReq());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOffer(ENTERPRISE_ID, OFFER_ID,
                        new OfferUpdateReq(2, "新标题", null, null, null, null, null, null)));

        assertEquals(ErrorCode.OFFER_VERSION_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void updateOffer_shouldRejectWhenAllFieldsNull() {
        when(mapper.selectOne(any())).thenReturn(createReq());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOffer(ENTERPRISE_ID, OFFER_ID,
                        new OfferUpdateReq(3, null, null, null, null, null, null, null)));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateOffer_shouldRejectSalaryMismatchAfterMerge() {
        Offer offer = createReq();
        offer.setSalaryMin(null);
        offer.setSalaryMax(null);
        when(mapper.selectOne(any())).thenReturn(offer);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOffer(ENTERPRISE_ID, OFFER_ID,
                        new OfferUpdateReq(3, null, new BigDecimal("18000"), null,
                                null, null, null, null)));

        assertEquals(ErrorCode.PARAM_VALID_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateOffer_zeroRowsShouldClassifyStatusChanged() {
        Offer sent = createReq();
        sent.setStatus(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(sent);
        when(mapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOffer(ENTERPRISE_ID, OFFER_ID,
                        new OfferUpdateReq(3, "新标题", null, null, null, null, null, null)));

        assertEquals(ErrorCode.OFFER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void sendOffer_shouldSendAndMarkOffered() {
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(null);
        when(mapper.update(any(), any())).thenReturn(1);

        OfferSendVO result = service.sendOffer(
                ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3));

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.SENT, result.status());
        assertEquals(4, result.version());
        assertNotNull(result.sentAt());
        verify(jobValidationApi).markOffered(APPLICATION_ID, ENTERPRISE_ID);
    }

    @Test
    void sendOffer_shouldReplayWhenSameKey() {
        Offer sent = sentRow(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(sent);

        OfferSendVO result = service.sendOffer(
                ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3));

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.SENT, result.status());
        assertEquals(sent.getSentAt(), result.sentAt());
        verify(mapper, never()).update(any(), any());
        verify(jobValidationApi, never()).markOffered(any(), any());
    }

    @Test
    void sendOffer_shouldRejectIdempotencyKeyUsedByAnotherOffer() {
        Offer otherSent = sentRow(OfferStatus.SENT);
        otherSent.setId(99999L);
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(otherSent);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendOffer(
                        ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3)));

        assertEquals(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void sendOffer_shouldRejectWhenExpired() {
        Offer expired = createReq();
        expired.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(expired);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendOffer(
                        ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3)));

        assertEquals(ErrorCode.OFFER_EXPIRED.getCode(), exception.getCode());
    }

    @Test
    void sendOffer_shouldRejectWhenExpectedStatusMismatch() {
        Offer sent = sentRow(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(sent);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendOffer(
                        ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3)));

        assertEquals(ErrorCode.OFFER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void sendOffer_zeroRowsShouldReplaySameKey() {
        Offer sent = sentRow(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(null).thenReturn(sent);
        when(mapper.update(any(), any())).thenReturn(0);

        OfferSendVO result = service.sendOffer(
                ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3));

        assertEquals(OfferStatus.SENT, result.status());
        assertEquals(sent.getSentAt(), result.sentAt());
        verify(jobValidationApi, never()).markOffered(any(), any());
    }

    @Test
    void sendOffer_zeroRowsShouldClassifyWithdrawn() {
        Offer withdrawn = sentRow(OfferStatus.WITHDRAWN);
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(null).thenReturn(withdrawn);
        when(mapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.sendOffer(
                        ENTERPRISE_ID, OFFER_ID, SEND_KEY, new OfferSendReq(OfferStatus.DRAFT, 3)));

        assertEquals(ErrorCode.OFFER_WITHDRAWN.getCode(), exception.getCode());
    }

    @Test
    void withdrawOffer_shouldWithdrawWhenSent() {
        Offer sent = sentRow(OfferStatus.SENT);
        when(mapper.selectOne(any())).thenReturn(sent);
        when(mapper.update(any(), any())).thenReturn(1);

        OfferUpdateVO result = service.withdrawOffer(
                ENTERPRISE_ID, OFFER_ID, new OfferWithdrawReq(OfferStatus.SENT, 3, "招聘计划调整"));

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.WITHDRAWN, result.status());
        assertEquals(4, result.version());
        assertNotNull(result.updatedAt());
    }

    @Test
    void withdrawOffer_shouldRejectWhenDraft() {
        when(mapper.selectOne(any())).thenReturn(createReq());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.withdrawOffer(
                        ENTERPRISE_ID, OFFER_ID, new OfferWithdrawReq(OfferStatus.SENT, 3, "招聘计划调整")));

        assertEquals(ErrorCode.OFFER_NOT_SENT_YET.getCode(), exception.getCode());
    }

    @Test
    void withdrawOffer_shouldRejectWhenExpectedStatusMismatch() {
        Offer withdrawn = sentRow(OfferStatus.WITHDRAWN);
        when(mapper.selectOne(any())).thenReturn(withdrawn);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.withdrawOffer(
                        ENTERPRISE_ID, OFFER_ID, new OfferWithdrawReq(OfferStatus.SENT, 3, "招聘计划调整")));

        assertEquals(ErrorCode.OFFER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void withdrawOffer_shouldRejectWhenExpired() {
        Offer sent = sentRow(OfferStatus.SENT);
        sent.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(sent);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.withdrawOffer(
                        ENTERPRISE_ID, OFFER_ID, new OfferWithdrawReq(OfferStatus.SENT, 3, "招聘计划调整")));

        assertEquals(ErrorCode.OFFER_EXPIRED.getCode(), exception.getCode());
    }

    @Test
    void withdrawOffer_zeroRowsShouldClassifyAlreadyDecided() {
        Offer accepted = sentRow(OfferStatus.ACCEPTED);
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(accepted);
        when(mapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.withdrawOffer(
                        ENTERPRISE_ID, OFFER_ID, new OfferWithdrawReq(OfferStatus.SENT, 3, "招聘计划调整")));

        assertEquals(ErrorCode.OFFER_ALREADY_DECIDED.getCode(), exception.getCode());
    }

    @Test
    void decideOffer_shouldAcceptAndMarkHired() {
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(null);
        when(mapper.update(any(), any())).thenReturn(1);

        OfferDecisionVO result = service.decideOffer(
                OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受"));

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(OfferStatus.ACCEPTED, result.status());
        assertNotNull(result.decidedAt());
        verify(jobValidationApi).markHired(APPLICATION_ID, ENTERPRISE_ID);
    }

    @Test
    void decideOffer_shouldDeclineWithoutMarkHired() {
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(null);
        when(mapper.update(any(), any())).thenReturn(1);

        OfferDecisionVO result = service.decideOffer(
                OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.DECLINE, "已接受其他机会"));

        assertEquals(OfferStatus.DECLINED, result.status());
        verify(jobValidationApi, never()).markHired(any(), any());
    }

    @Test
    void decideOffer_shouldReplayWhenSameKey() {
        Offer decided = sentRow(OfferStatus.ACCEPTED);
        decided.setDecisionIdempotencyKey(DECISION_KEY);
        decided.setDecidedAt(OffsetDateTime.now());
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(decided);

        OfferDecisionVO result = service.decideOffer(
                OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受"));

        assertEquals(OfferStatus.ACCEPTED, result.status());
        assertEquals(decided.getDecidedAt(), result.decidedAt());
        verify(mapper, never()).update(any(), any());
        verify(jobValidationApi, never()).markHired(any(), any());
    }

    @Test
    void decideOffer_shouldRejectIdempotencyKeyOfAnotherOffer() {
        Offer decided = sentRow(OfferStatus.ACCEPTED);
        decided.setId(99999L);
        decided.setDecisionIdempotencyKey(DECISION_KEY);
        decided.setDecidedAt(OffsetDateTime.now());
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(decided);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.decideOffer(
                        OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受")));

        assertEquals(ErrorCode.OFFER_IDEMPOTENCY_KEY_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void decideOffer_shouldRejectWhenNotSent() {
        when(mapper.selectOne(any())).thenReturn(createReq()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.decideOffer(
                        OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受")));

        assertEquals(ErrorCode.OFFER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void decideOffer_shouldRejectWhenExpired() {
        Offer sent = sentRow(OfferStatus.SENT);
        sent.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(sent).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.decideOffer(
                        OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受")));

        assertEquals(ErrorCode.OFFER_EXPIRED.getCode(), exception.getCode());
    }

    @Test
    void decideOffer_zeroRowsShouldReplaySameKey() {
        Offer decided = sentRow(OfferStatus.ACCEPTED);
        decided.setDecisionIdempotencyKey(DECISION_KEY);
        decided.setDecidedAt(OffsetDateTime.now());
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(null).thenReturn(decided);
        when(mapper.update(any(), any())).thenReturn(0);

        OfferDecisionVO result = service.decideOffer(
                OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受"));

        assertEquals(OfferStatus.ACCEPTED, result.status());
        assertEquals(decided.getDecidedAt(), result.decidedAt());
        verify(jobValidationApi, never()).markHired(any(), any());
    }

    @Test
    void decideOffer_zeroRowsShouldClassifyDecidedByOtherKey() {
        Offer declined = sentRow(OfferStatus.DECLINED);
        declined.setDecisionIdempotencyKey("other-key");
        declined.setDecidedAt(OffsetDateTime.now());
        when(mapper.selectOne(any())).thenReturn(sentRow(OfferStatus.SENT)).thenReturn(null).thenReturn(declined);
        when(mapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.decideOffer(
                        OFFER_ID, DECISION_KEY, new OfferDecisionReq(OfferStatus.SENT, OfferDecision.ACCEPT, "接受")));

        assertEquals(ErrorCode.OFFER_ALREADY_DECIDED.getCode(), exception.getCode());
    }

    @Test
    void getOfferDetail_shouldReturnMappedDetail() {
        Offer offer = createReq();
        offer.setSentAt(OffsetDateTime.now());
        offer.setDecidedAt(OffsetDateTime.now());
        when(mapper.selectOne(any())).thenReturn(offer);

        OfferDetailVO result = service.getOfferDetail(ENTERPRISE_ID, OFFER_ID);

        assertEquals(OFFER_ID, result.offerId());
        assertEquals(APPLICATION_ID, result.applicationId());
        assertEquals("Java后端工程师录用通知", result.title());
        assertEquals(new BigDecimal("18000"), result.salaryMin());
        assertEquals(new BigDecimal("25000"), result.salaryMax());
        assertEquals("CNY", result.currency());
        assertEquals(offer.getExpiresAt(), result.expiresAt());
        assertEquals("恭喜通过面试", result.content());
        assertEquals(OfferStatus.DRAFT, result.status());
        assertEquals(3, result.version());
        assertEquals(offer.getSentAt(), result.sentAt());
        assertEquals(offer.getDecidedAt(), result.decidedAt());
        assertEquals(offer.getCreatedAt(), result.createdAt());
    }

    @Test
    void getOfferDetail_shouldRejectWhenNotFound() {
        when(mapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getOfferDetail(ENTERPRISE_ID, OFFER_ID));

        assertEquals(ErrorCode.OFFER_NOT_FOUND.getCode(), exception.getCode());
    }
}
