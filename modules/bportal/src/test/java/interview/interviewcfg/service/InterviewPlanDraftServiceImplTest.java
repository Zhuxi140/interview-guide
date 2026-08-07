package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.api.bportal.JobValidationApi;
import interview.api.infra.LocalMessageApi;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.framework.config.CustomIdGenerator;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.req.InterviewScheduleCreateReq;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewScheduleCreateVO;
import interview.interviewcfg.service.InterviewScheduleService;
import interview.interviewcfg.service.impl.InterviewPlanDraftServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewPlanDraftServiceImplTest {

    private static final String PLAN_JSON = """
            {"stages":[{"phaseCode":"TECHNICAL","objectives":"鑰冨療涓氬姟璁捐","questionOutline":"1. 鏂规璁捐","durationMinutes":60}],
            "scheduleSuggestions":[{"suggestionId":1,"interviewerUserId":5001,"interviewTime":"2026-08-06T10:00:00+08:00","reason":"鏃堕棿绌洪棽"}]}
            """;

    @Mock
    private InterviewPlanDraftMapper mapper;
    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;
    @Mock
    private JobValidationApi jobValidationApi;
    @Mock
    private InterviewStageTemplateService interviewStageTemplateService;
    @Mock
    private InterviewScheduleService interviewScheduleService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LocalMessageApi localMessageApi;
    @Mock
    private CustomIdGenerator customIdGenerator;

    private InterviewPlanDraftServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 注册实体元数据，使 lambda select 能解析列名（Spring 启动时会自动完成，单测需手动）。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "InterviewPlanDraft"),
                InterviewPlanDraft.class);
        service = spy(new InterviewPlanDraftServiceImpl(
                enterpriseValidationApi, jobValidationApi, interviewStageTemplateService,
                interviewScheduleService, eventPublisher, localMessageApi,
                customIdGenerator));
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

    private InterviewPlanDraftApplyReq request(String key, String outline, String phaseCode) {
        InterviewPlanDraftApplyReq.Plan plan = new InterviewPlanDraftApplyReq.Plan(
                List.of(new InterviewPlanDraftApplyReq.Stage(
                        phaseCode, "鑰冨療涓氬姟璁捐", outline, 60)),
                List.of(new InterviewPlanDraftApplyReq.Suggestion(
                        1L, 5001L, OffsetDateTime.parse("2026-08-06T10:00:00+08:00"), "鏃堕棿绌洪棽")));
        return new InterviewPlanDraftApplyReq(3, plan);
    }

    private InterviewScheduleCreateVO scheduleVO(Long id) {
        return new InterviewScheduleCreateVO(
                id, 20L, (short) 1, "TECHNICAL", "技术面",
                interview.common.enums.InterviewScheduleStatus.PENDING_CONFIRMATION,
                0, OffsetDateTime.now(), 60, OffsetDateTime.now());
    }

    private InterviewPlanDraft readyDraft() {
        return InterviewPlanDraft.builder()
                .id(100L)
                .enterpriseId(10L)
                .applicationId(20L)
                .status(InterviewPlanDraftStatus.READY)
                .planJson(PLAN_JSON)
                .version(3)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
    }

    private String appliedPlanJsonOf(InterviewPlanDraftApplyReq req) {
        try {
            return objectMapper.writeValueAsString(objectMapper.valueToTree(req.plan()));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void apply_shouldMarkAppliedWhenReady() {
        when(mapper.selectOne(any())).thenReturn(readyDraft());
        when(mapper.update(any(), any())).thenReturn(1);
        when(interviewScheduleService.createSchedule(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(scheduleVO(32001L));

        InterviewPlanDraftApplyVO result = service.applyDraft(
                10L, 20L, 100L, "apply-key-1", request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL"));

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        assertNotNull(result.appliedAt());

        ArgumentCaptor<InterviewPlanDraft> captor = ArgumentCaptor.forClass(InterviewPlanDraft.class);
        verify(mapper).update(captor.capture(), any());
        InterviewPlanDraft update = captor.getValue();
        assertEquals(InterviewPlanDraftStatus.APPLIED, update.getStatus());
        assertEquals("apply-key-1", update.getApplyIdempotencyKey());
        assertEquals(3, update.getVersion());
        assertNotNull(update.getAppliedPlanJson());
        assertEquals("[32001]", update.getAppliedScheduleIds());
        verify(interviewScheduleService).createSchedule(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void apply_shouldCreateSchedulePerRoundAndPersistIds() {
        InterviewPlanDraft twoStageDraft = readyDraft();
        twoStageDraft.setPlanJson("""
                {"stages":[
                {"phaseCode":"TECHNICAL","objectives":"考察目标","questionOutline":"题纲","durationMinutes":60},
                {"phaseCode":"HR","objectives":"考察目标","questionOutline":"题纲","durationMinutes":30}],
                "scheduleSuggestions":[
                {"suggestionId":1,"interviewerUserId":5001,"interviewTime":"2026-08-06T10:00:00+08:00","reason":"reason"},
                {"suggestionId":2,"interviewerUserId":5002,"interviewTime":"2026-08-07T10:00:00+08:00","reason":"reason"}]}
                """);
        when(mapper.selectOne(any())).thenReturn(twoStageDraft);
        when(mapper.update(any(), any())).thenReturn(1);
        when(interviewScheduleService.createSchedule(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(scheduleVO(32001L), scheduleVO(32002L));

        // 两个阶段 + 两条排期建议，服务端按 roundNo=1..2 各建一个排期。
        InterviewPlanDraftApplyReq twoRound = new InterviewPlanDraftApplyReq(3,
                new InterviewPlanDraftApplyReq.Plan(
                        List.of(
                                new InterviewPlanDraftApplyReq.Stage("TECHNICAL", "考察目标", "题纲", 60),
                                new InterviewPlanDraftApplyReq.Stage("HR", "考察目标", "题纲", 30)),
                        List.of(
                                new InterviewPlanDraftApplyReq.Suggestion(
                                        1L, 5001L, OffsetDateTime.parse("2026-08-06T10:00:00+08:00"), "reason"),
                                new InterviewPlanDraftApplyReq.Suggestion(
                                        2L, 5002L, OffsetDateTime.parse("2026-08-07T10:00:00+08:00"), "reason"))));
        InterviewPlanDraftApplyVO result = service.applyDraft(10L, 20L, 100L, "apply-key-1", twoRound);

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        assertEquals(List.of(32001L, 32002L), result.scheduleIds());

        ArgumentCaptor<InterviewScheduleCreateReq> reqCaptor =
                ArgumentCaptor.forClass(InterviewScheduleCreateReq.class);
        verify(interviewScheduleService, org.mockito.Mockito.times(2))
                .createSchedule(anyLong(), anyLong(), reqCaptor.capture(), anyString());
        assertEquals((short) 1, reqCaptor.getAllValues().get(0).roundNo());
        assertEquals((short) 2, reqCaptor.getAllValues().get(1).roundNo());
        assertEquals(60, reqCaptor.getAllValues().get(0).durationMinutes());
        assertEquals(30, reqCaptor.getAllValues().get(1).durationMinutes());
        assertEquals(InterviewType.TEXT, reqCaptor.getAllValues().get(0).interviewType());
    }

    @Test
    void apply_shouldRejectSuggestionCountMismatch() {
        when(mapper.selectOne(any())).thenReturn(readyDraft());

        // 一个阶段却提交两条排期建议，与阶段处理不存在一一对应。
        InterviewPlanDraftApplyReq mismatch = new InterviewPlanDraftApplyReq(3,
                new InterviewPlanDraftApplyReq.Plan(
                        List.of(new InterviewPlanDraftApplyReq.Stage("TECHNICAL", "考察目标", "题纲", 60)),
                        List.of(
                                new InterviewPlanDraftApplyReq.Suggestion(
                                        1L, 5001L, OffsetDateTime.parse("2026-08-06T10:00:00+08:00"), "reason"),
                                new InterviewPlanDraftApplyReq.Suggestion(
                                        2L, 5002L, OffsetDateTime.parse("2026-08-07T10:00:00+08:00"), "reason"))));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1", mismatch));

        assertEquals(ErrorCode.INTERVIEW_SCHEDULE_SUGGESTION_MISMATCH.getCode(), exception.getCode());
        verify(mapper, never()).update(any(), any());
        verify(interviewScheduleService, never()).createSchedule(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void apply_shouldSkipScheduleCreationWhenNoSuggestions() {
        InterviewPlanDraft draft = readyDraft();
        draft.setPlanJson("{\"stages\":[{\"phaseCode\":\"TECHNICAL\",\"objectives\":\"objective\",\"questionOutline\":\"outline\",\"durationMinutes\":60}],\"scheduleSuggestions\":[]}");
        when(mapper.selectOne(any())).thenReturn(draft);
        when(mapper.update(any(), any())).thenReturn(1);

        InterviewPlanDraftApplyReq noSuggestion = new InterviewPlanDraftApplyReq(3,
                new InterviewPlanDraftApplyReq.Plan(
                        List.of(new InterviewPlanDraftApplyReq.Stage("TECHNICAL", "objective", "outline", 60)),
                        List.of()));
        InterviewPlanDraftApplyVO result = service.applyDraft(10L, 20L, 100L, "apply-key-1", noSuggestion);

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        assertTrue(result.scheduleIds().isEmpty());
        verify(interviewScheduleService, never()).createSchedule(anyLong(), anyLong(), any(), anyString());

        ArgumentCaptor<InterviewPlanDraft> captor = ArgumentCaptor.forClass(InterviewPlanDraft.class);
        verify(mapper).update(captor.capture(), any());
        assertEquals("[]", captor.getValue().getAppliedScheduleIds());
    }

@Test
    void apply_shouldReplayWhenSameKeyAndSamePlan() {
        InterviewPlanDraftApplyReq sameReq =
                request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL");
        InterviewPlanDraft applied = readyDraft();
        applied.setStatus(InterviewPlanDraftStatus.APPLIED);
        applied.setApplyIdempotencyKey("apply-key-1");
        applied.setAppliedPlanJson(appliedPlanJsonOf(sameReq));
        applied.setAppliedScheduleIds("[32001,32002]");
        applied.setAppliedAt(OffsetDateTime.parse("2026-08-06T11:00:00+08:00"));
        when(mapper.selectOne(any())).thenReturn(applied);

        InterviewPlanDraftApplyVO result = service.applyDraft(
                10L, 20L, 100L, "apply-key-1", sameReq);

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        assertEquals(List.of(32001L, 32002L), result.scheduleIds());
        assertEquals(applied.getAppliedAt(), result.appliedAt());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void apply_shouldRejectSameKeyWithDifferentPlan() {
        InterviewPlanDraft applied = readyDraft();
        applied.setStatus(InterviewPlanDraftStatus.APPLIED);
        applied.setApplyIdempotencyKey("apply-key-1");
        applied.setAppliedPlanJson(PLAN_JSON);
        applied.setAppliedScheduleIds("[32001]");
        when(mapper.selectOne(any())).thenReturn(applied);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "淇敼杩囩殑棰樼翰", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_PLAN_DRAFT_ALREADY_APPLIED.getCode(), exception.getCode());
    }

    @Test
    void apply_shouldRejectDifferentKeyWhenApplied() {
        InterviewPlanDraft applied = readyDraft();
        applied.setStatus(InterviewPlanDraftStatus.APPLIED);
        applied.setApplyIdempotencyKey("apply-key-1");
        applied.setAppliedPlanJson(PLAN_JSON);
        when(mapper.selectOne(any())).thenReturn(applied);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-2",
                        request("apply-key-2", "1. 鏂规璁捐", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_PLAN_DRAFT_ALREADY_APPLIED.getCode(), exception.getCode());
    }

    @Test
    void apply_shouldRejectWhenNotReady() {
        InterviewPlanDraft pending = readyDraft();
        pending.setStatus(InterviewPlanDraftStatus.PENDING);
        when(mapper.selectOne(any())).thenReturn(pending);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED.getCode(), exception.getCode());
    }

    @Test
    void apply_shouldRejectWhenStageStructureChanged() {
        when(mapper.selectOne(any())).thenReturn(readyDraft());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "1. 鏂规璁捐", "HR")));

        assertEquals(ErrorCode.INTERVIEW_TEMPLATE_STAGE_INVALID.getCode(), exception.getCode());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void apply_shouldRejectWhenTemplateVersionChanged() {
        InterviewPlanDraft ready = readyDraft();
        ready.setTemplateId(3L);
        ready.setInputSnapshotJson("{\"templateId\":3,\"templateVersion\":1,\"templateName\":\"t\",\"stages\":[]}");
        when(mapper.selectOne(any())).thenReturn(ready);
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(new InterviewTemplateSnapshot(3L, 2, "t", List.of()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_PLAN_TEMPLATE_CHANGED.getCode(), exception.getCode());
        verify(mapper, never()).update(any(), any());
        verify(interviewScheduleService, never()).createSchedule(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void apply_shouldRejectWhenTemplateDeleted() {
        InterviewPlanDraft ready = readyDraft();
        ready.setTemplateId(3L);
        ready.setInputSnapshotJson("{\"templateId\":3,\"templateVersion\":1,\"templateName\":\"t\",\"stages\":[]}");
        when(mapper.selectOne(any())).thenReturn(ready);
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "1. 鏂规璁", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND.getCode(), exception.getCode());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void apply_shouldProceedWhenTemplateVersionMatches() {
        InterviewPlanDraft ready = readyDraft();
        ready.setTemplateId(3L);
        ready.setInputSnapshotJson("{\"templateId\":3,\"templateVersion\":1,\"templateName\":\"t\",\"stages\":[]}");
        when(mapper.selectOne(any())).thenReturn(ready);
        when(mapper.update(any(), any())).thenReturn(1);
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(new InterviewTemplateSnapshot(3L, 1, "t", List.of()));
        when(interviewScheduleService.createSchedule(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(scheduleVO(32001L));

        InterviewPlanDraftApplyVO result = service.applyDraft(10L, 20L, 100L, "apply-key-1",
                request("apply-key-1", "1. 鏂规璁", "TECHNICAL"));

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        verify(interviewStageTemplateService).buildTemplateSnapshotWithoutAuth(10L, 3L);
    }

    @Test
    void apply_shouldReplayAfterLostCasRace() {
        InterviewPlanDraftApplyReq sameReq =
                request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL");
        InterviewPlanDraft applied = readyDraft();
        applied.setStatus(InterviewPlanDraftStatus.APPLIED);
        applied.setApplyIdempotencyKey("apply-key-1");
        applied.setAppliedPlanJson(appliedPlanJsonOf(sameReq));
        applied.setAppliedScheduleIds("[32001]");
        applied.setAppliedAt(OffsetDateTime.parse("2026-08-06T11:00:00+08:00"));
        when(mapper.selectOne(any())).thenReturn(readyDraft(), applied);
        when(mapper.update(any(), any())).thenReturn(0);
        when(interviewScheduleService.createSchedule(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(scheduleVO(32001L));

        InterviewPlanDraftApplyVO result = service.applyDraft(
                10L, 20L, 100L, "apply-key-1", sameReq);

        assertEquals(List.of(32001L), result.scheduleIds());
        assertEquals(applied.getAppliedAt(), result.appliedAt());
    }

    @Test
    void apply_shouldThrowVersionConflictWhenCasZeroAndStillReady() {
        InterviewPlanDraft newer = readyDraft();
        newer.setVersion(4);
        when(mapper.selectOne(any())).thenReturn(readyDraft(), newer);
        when(mapper.update(any(), any())).thenReturn(0);
        when(interviewScheduleService.createSchedule(anyLong(), anyLong(), any(), anyString()))
                .thenReturn(scheduleVO(32001L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyDraft(10L, 20L, 100L, "apply-key-1",
                        request("apply-key-1", "1. 鏂规璁捐", "TECHNICAL")));

        assertEquals(ErrorCode.INTERVIEW_PLAN_DRAFT_VERSION_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void detail_shouldReturnRevisedPlanWhenApplied() {
        InterviewPlanDraft applied = readyDraft();
        applied.setStatus(InterviewPlanDraftStatus.APPLIED);
        applied.setAppliedPlanJson("{\"stages\":[{\"phaseCode\":\"TECHNICAL\",\"objectives\":\"HR淇鐩爣\",\"questionOutline\":\"淇棰樼翰\",\"durationMinutes\":45}],\"scheduleSuggestions\":[]}");
        LambdaQueryChainWrapper<InterviewPlanDraft> query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.select(any(SFunction[].class))).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(applied);

        var result = service.getDraftDetail(10L, 20L, 100L);

        assertEquals(InterviewPlanDraftStatus.APPLIED, result.status());
        assertNotNull(result.plan());
        assertEquals("HR淇鐩爣", result.plan().stages().getFirst().objectives());
        assertTrue(result.plan().scheduleSuggestions().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void create_shouldRestrictSnapshotToSelectedPhaseCodes() {
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(threeStageSnapshot());
        doReturn(9001L).when(customIdGenerator).nextId(any(Class.class));
        when(localMessageApi.saveInCurrentTransaction(any())).thenReturn(55501L);
        LambdaQueryChainWrapper<InterviewPlanDraft> query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.select(any(SFunction[].class))).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        doReturn(query).when(query).in(any(), any(Object[].class));
        when(query.one()).thenReturn(null);
        when(query.exists()).thenReturn(false);

        InterviewPlanDraftCreateReq req = new InterviewPlanDraftCreateReq(
                3L, InterviewType.TEXT, 60, null, List.of(),
                List.of("HR", "TECHNICAL"), "多考察业务");
        service.createDraft(10L, 20L, "create-key-1", req);

        ArgumentCaptor<InterviewPlanDraft> captor = ArgumentCaptor.forClass(InterviewPlanDraft.class);
        verify(mapper).insert(captor.capture());
        InterviewPlanDraft saved = captor.getValue();
        JSONObject snapshot = JSONUtil.parseObj(saved.getInputSnapshotJson());
        List<String> codes = snapshot.getJSONArray("stages").stream()
                .map(item -> ((JSONObject) item).getStr("phaseCode"))
                .toList();
        assertEquals(List.of("HR", "TECHNICAL"), codes);
        assertNotNull(saved.getGenerationMessageId());
    }

    @Test
    void create_shouldRejectUnknownPhaseCode() {
        InterviewPlanDraftCreateReq req = new InterviewPlanDraftCreateReq(
                3L, InterviewType.TEXT, 100, null, List.of(),
                List.of("TECHNICAL", "NOT_EXIST"), null);
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(threeStageSnapshot());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createDraft(10L, 20L, "create-key-2", req));

        assertEquals(ErrorCode.INTERVIEW_TEMPLATE_STAGE_INVALID.getCode(), exception.getCode());
        verify(mapper, never()).insert(any(InterviewPlanDraft.class));
    }

    @Test
    void create_shouldRejectSubsetExceedingMaxRounds() {
        InterviewPlanDraftCreateReq req = new InterviewPlanDraftCreateReq(
                3L, InterviewType.TEXT, 100, 1, List.of(),
                List.of("TECHNICAL", "HR"), null);
        when(interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(10L, 3L))
                .thenReturn(threeStageSnapshot());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createDraft(10L, 20L, "create-key-3", req));

        assertEquals(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED.getCode(), exception.getCode());
        verify(mapper, never()).insert(any(InterviewPlanDraft.class));
    }

    private InterviewTemplateSnapshot threeStageSnapshot() {
        return new InterviewTemplateSnapshot(
                3L, 1, "三阶段模板",
                List.of(
                        new InterviewTemplateSnapshot.StageSnapshot(
                                "TECHNICAL", "技术面", 1, 5, 0.6, null, 1),
                        new InterviewTemplateSnapshot.StageSnapshot(
                                "HR", "HR面", 2, 3, 0.4, null, 1),
                        new InterviewTemplateSnapshot.StageSnapshot(
                                "ASSESSMENT", "评估面", 3, 2, 0.3, null, 1)));
    }
}
