package interview.candidate.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.UserApi;
import interview.api.system.dto.UserProfileDTO;
import interview.api.system.dto.UserProfileUpdateDTO;
import interview.candidate.mapper.CandidateBasicProfileMapper;
import interview.candidate.model.entity.CandidateBasicProfile;
import interview.candidate.model.enums.CandidateBasicProfileStatus;
import interview.candidate.model.req.CandidateProfileUpdateReq;
import interview.candidate.model.vo.CandidateBasicProfileVO;
import interview.candidate.model.vo.CandidateProfileUpdateVO;
import interview.candidate.service.CandidateBasicProfileService;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.vo.CandidateSkillScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateBasicProfileServiceImpl
        extends ServiceImpl<CandidateBasicProfileMapper, CandidateBasicProfile>
        implements CandidateBasicProfileService {

    private final UserApi userApi;
    private final CandidateProfileMapper candidateProfileMapper;
    private final CandidateSkillScoresMapper candidateSkillScoresMapper;
    private final ObjectMapper objectMapper;

    @Override
    public CandidateBasicProfileVO getMyProfile() {
        Long userId = AuthContext.getRequiredUserId();
        UserProfileDTO user = userApi.getUserProfile(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        CandidateBasicProfile profile = lambdaQuery()
                .eq(CandidateBasicProfile::getUserId, userId)
                .one();
        return toVO(user, profile, latestDimensions(userId));
    }

    @Override
    @Transactional
    public CandidateProfileUpdateVO updateMyProfile(CandidateProfileUpdateReq req) {
        if (req.getDisplayName() == null && req.getEmail() == null
                && req.getCity() == null && req.getEducation() == null
                && req.getSummary() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "至少提交一个可更新字段");
        }
        Long userId = AuthContext.getRequiredUserId();
        CandidateBasicProfile current = lambdaQuery()
                .eq(CandidateBasicProfile::getUserId, userId)
                .one();
        int currentVersion = current == null ? 0 : current.getVersion();
        if (currentVersion != req.getExpectedVersion()) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_VERSION_CONFLICT);
        }

        // sys_users 由 system 模块通过内部 API 更新，候选人扩展字段留在 bportal。
        UserProfileDTO user = req.getDisplayName() == null && req.getEmail() == null
                ? userApi.getUserProfile(userId)
                : userApi.updateUserProfile(
                        userId, new UserProfileUpdateDTO(req.getDisplayName(), req.getEmail()));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String city = chooseValue(req.getCity(), current == null ? null : current.getExpectedCity());
        String education = chooseValue(
                req.getEducation(), current == null ? null : current.getEducation());
        String summary = chooseValue(req.getSummary(), current == null ? null : current.getSummary());
        CandidateBasicProfileStatus status = isComplete(user, city, education, summary)
                ? CandidateBasicProfileStatus.COMPLETE
                : CandidateBasicProfileStatus.INCOMPLETE;

        CandidateBasicProfile saved;
        try {
            if (current == null) {
                saved = CandidateBasicProfile.builder()
                        .userId(userId)
                        .expectedCity(city)
                        .education(education)
                        .summary(summary)
                        .profileStatus(status)
                        .version(1)
                        .build();
                if (!save(saved)) {
                    throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_VERSION_CONFLICT);
                }
            } else {
                CandidateBasicProfile update = CandidateBasicProfile.builder()
                        .id(current.getId())
                        .expectedCity(city)
                        .education(education)
                        .summary(summary)
                        .profileStatus(status)
                        .version(req.getExpectedVersion())
                        .build();
                if (!updateById(update)) {
                    throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_VERSION_CONFLICT);
                }
                saved = lambdaQuery()
                        .select(CandidateBasicProfile::getId,
                                CandidateBasicProfile::getVersion,
                                CandidateBasicProfile::getUpdatedAt)
                        .eq(CandidateBasicProfile::getId, current.getId())
                        .one();
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CANDIDATE_PROFILE_VERSION_CONFLICT);
        }
        return new CandidateProfileUpdateVO(
                userId, saved.getVersion(), saved.getUpdatedAt());
    }

    private CandidateBasicProfileVO toVO(
            UserProfileDTO user, CandidateBasicProfile profile,
            List<CandidateSkillScoreVO> dimensions) {
        if (profile == null) {
            return new CandidateBasicProfileVO(
                    user.userId(), user.displayName(), user.email(), null, null, null,
                    CandidateBasicProfileStatus.INCOMPLETE, dimensions, 0, user.updatedAt());
        }
        return new CandidateBasicProfileVO(
                user.userId(), user.displayName(), user.email(),
                profile.getExpectedCity(), profile.getEducation(), profile.getSummary(),
                profile.getProfileStatus(), dimensions, profile.getVersion(), profile.getUpdatedAt());
    }

    private List<CandidateSkillScoreVO> latestDimensions(Long userId) {
        CandidateProfile aiProfile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .select(CandidateProfile::getId)
                        .eq(CandidateProfile::getCandidateId, userId)
                        .eq(CandidateProfile::getStatus, AiTaskStatus.COMPLETED)
                        .orderByDesc(CandidateProfile::getAnalyzedAt)
                        .last("LIMIT 1"));
        if (aiProfile == null) {
            return List.of();
        }
        return candidateSkillScoresMapper.selectList(
                        Wrappers.<CandidateSkillScores>lambdaQuery()
                                .eq(CandidateSkillScores::getCandidateProfileId, aiProfile.getId())
                                .orderByAsc(CandidateSkillScores::getDimensionCode))
                .stream()
                .map(score -> new CandidateSkillScoreVO(
                        score.getDimensionCode(), score.getScore(), score.getAiJustification(),
                        readEvidence(score.getEvidenceJson())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> readEvidence(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return (List<String>) (List<?>) objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String chooseValue(String requested, String current) {
        return requested == null ? current : StrUtil.emptyToNull(requested.trim());
    }

    private boolean isComplete(
            UserProfileDTO user, String city, String education, String summary) {
        return StrUtil.isAllNotBlank(
                user.displayName(), user.email(), city, education, summary);
    }
}
