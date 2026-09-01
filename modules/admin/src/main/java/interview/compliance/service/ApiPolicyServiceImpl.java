package interview.compliance.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.compliance.mapper.UserApiPolicyMapper;
import interview.compliance.model.entity.UserApiPolicy;
import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import interview.compliance.model.req.ApiPolicyCreateReq;
import interview.compliance.model.req.ApiPolicySearchReq;
import interview.compliance.model.req.ApiPolicyUpdateReq;
import interview.compliance.model.vo.ApiPolicyListItemVO;
import interview.compliance.model.vo.ApiPolicyMutationVO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhuxi
 * @apiNote 平台端用户 API 策略管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiPolicyServiceImpl extends ServiceImpl<UserApiPolicyMapper, UserApiPolicy>
        implements ApiPolicyService {

    /** 受限 Ant 风格路径模板允许出现的字符集合（字母/数字/连字符/下划线/斜杠/星号/花括号参数位） */
    private static final Set<Character> PATH_ALLOWED_CHARS = buildAllowedPathChars();

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ApiPolicyMutationVO createPolicy(ApiPolicyCreateReq req) {
        // 分支一：RATE_LIMIT 必须同时携带正整数窗口次数与秒数。
        if (req.getPolicyType() == PolicyType.RATE_LIMIT
                && (req.getLimitCount() == null || req.getLimitSeconds() == null)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "限流策略必须同时提供正整数 limitCount 与 limitSeconds");
        }
        // 分支二：名单类策略不得携带限流参数。
        if (req.getPolicyType().isListType()
                && (req.getLimitCount() != null || req.getLimitSeconds() != null)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "黑白名单策略不得携带 limitCount/limitSeconds");
        }
        // 分支三：黑白名单互斥：同一用户不得同时存在另一类名单策略。
        verifyListTypeMutex(req.getUserId(), req.getPolicyType(), null);
        // 分支四：动作与策略类型一致性。
        verifyActionConsistency(req.getPolicyType(), req.getActionType());
        // 分支五：路径模板仅接受受限 Ant 风格，过期时间必须晚于当前。
        verifyPathPattern(req.getApiPathPattern());
        verifyExpireTime(req.getExpireTime());

        UserApiPolicy policy = UserApiPolicy.builder()
                .userId(req.getUserId())
                .apiPathRegex(StrUtil.blankToDefault(req.getApiPathPattern(), null))
                .policyType(req.getPolicyType())
                .limitCount(req.getLimitCount())
                .limitSeconds(req.getLimitSeconds())
                .actionType(req.getActionType())
                .expireTime(req.getExpireTime())
                .reason(req.getReason())
                .version(0)
                .build();
        save(policy);

        // TODO [Phase8] 策略执行点（网关/拦截器）接入后，此处需刷新用户的策略缓存。
        return toMutationVO(policy);
    }

    @Override
    public IPage<ApiPolicyListItemVO> pagePolicies(ApiPolicySearchReq req) {
        // 单表 LambdaQuery：按策略类型筛选、ID 倒序。
        LambdaQueryWrapper<UserApiPolicy> wrapper = new LambdaQueryWrapper<UserApiPolicy>()
                .eq(req.getPolicyType() != null, UserApiPolicy::getPolicyType, req.getPolicyType())
                .orderByDesc(UserApiPolicy::getId);
        Page<UserApiPolicy> policyPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<ApiPolicyListItemVO> voPage = new Page<>(
                policyPage.getCurrent(), policyPage.getSize(), policyPage.getTotal());
        voPage.setRecords(policyPage.getRecords().stream()
                .map(policy -> ApiPolicyListItemVO.builder()
                        .id(policy.getId())
                        .userId(policy.getUserId())
                        .policyType(policy.getPolicyType())
                        .actionType(policy.getActionType())
                        .expireTime(policy.getExpireTime())
                        .reason(policy.getReason())
                        .version(policy.getVersion())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ApiPolicyMutationVO updatePolicy(Long policyId, ApiPolicyUpdateReq req) {
        // 校验策略存在；策略类型创建后不可变更。
        UserApiPolicy existing = getRequiredPolicy(policyId);

        // 合并限流参数后复用创建侧校验分支：限流参数完整性 / 名单不得携带 / 动作一致性。
        Integer mergedLimitCount = req.getLimitCount() != null
                ? req.getLimitCount() : existing.getLimitCount();
        Integer mergedLimitSeconds = req.getLimitSeconds() != null
                ? req.getLimitSeconds() : existing.getLimitSeconds();
        if (existing.getPolicyType() == PolicyType.RATE_LIMIT
                && (mergedLimitCount == null || mergedLimitSeconds == null)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "限流策略必须同时提供正整数 limitCount 与 limitSeconds");
        }
        if (existing.getPolicyType().isListType()
                && (req.getLimitCount() != null || req.getLimitSeconds() != null)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "黑白名单策略不得携带 limitCount/limitSeconds");
        }
        PolicyAction mergedAction = req.getActionType() != null
                ? req.getActionType() : existing.getActionType();
        verifyActionConsistency(existing.getPolicyType(), mergedAction);
        verifyPathPattern(req.getApiPathPattern());

        // 半量更新：仅覆盖请求中出现的字段，乐观锁由 version 条件保证。
        UserApiPolicy update = new UserApiPolicy();
        update.setId(policyId);
        if (req.getApiPathPattern() != null) {
            update.setApiPathRegex(StrUtil.blankToDefault(req.getApiPathPattern(), null));
        }
        if (req.getLimitCount() != null) {
            update.setLimitCount(req.getLimitCount());
        }
        if (req.getLimitSeconds() != null) {
            update.setLimitSeconds(req.getLimitSeconds());
        }
        if (req.getActionType() != null) {
            update.setActionType(req.getActionType());
        }
        if (req.getExpireTime() != null) {
            verifyExpireTime(req.getExpireTime());
            update.setExpireTime(req.getExpireTime());
        }
        if (StrUtil.isNotBlank(req.getReason())) {
            update.setReason(req.getReason());
        }
        update.setVersion(req.getExpectedVersion());
        int affected = baseMapper.update(update,
                Wrappers.<UserApiPolicy>lambdaUpdate().eq(UserApiPolicy::getId, policyId));
        if (affected == 0) {
            throw versionConflict();
        }

        // TODO [Phase8] 策略执行点接入后，更新同样需刷新用户的策略缓存。
        return ApiPolicyMutationVO.builder()
                .id(policyId)
                .userId(existing.getUserId())
                .policyType(existing.getPolicyType())
                .actionType(mergedAction)
                .version(req.getExpectedVersion() + 1)
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deletePolicy(Long policyId, Integer expectedVersion) {
        // 校验策略存在。
        getRequiredPolicy(policyId);

        // 乐观锁条件更新为逻辑删除；0 行表示版本冲突或并发删除。
        UserApiPolicy update = new UserApiPolicy();
        update.setId(policyId);
        update.setVersion(expectedVersion);
        int affected = baseMapper.update(update,
                Wrappers.<UserApiPolicy>lambdaUpdate()
                        .eq(UserApiPolicy::getId, policyId)
                        .set(UserApiPolicy::getIsDeleted, true));
        if (affected == 0) {
            throw versionConflict();
        }

        // TODO [Phase8] 策略执行点接入后，删除需刷新用户的策略缓存。
    }

    /**
     * 黑白名单互斥校验：同一用户在未删除策略中不得同时持有两类名单
     * @param userId 被管控用户 ID
     * @param policyType 本次写入的策略类型
     * @param excludePolicyId 更新场景需排除的自身策略 ID；创建时为 null
     */
    private void verifyListTypeMutex(Long userId, PolicyType policyType, Long excludePolicyId) {
        PolicyType opposite = policyType.oppositeListType();
        if (opposite == null) {
            return;
        }
        boolean exists = lambdaQuery()
                .eq(UserApiPolicy::getUserId, userId)
                .eq(UserApiPolicy::getPolicyType, opposite)
                .ne(excludePolicyId != null, UserApiPolicy::getId, excludePolicyId)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "该用户已存在互斥的" + (opposite == PolicyType.BLACKLIST ? "黑" : "白")
                            + "名单策略，黑白名单不能同时生效");
        }
    }

    /**
     * 动作与策略类型一致性校验：BLACKLIST/RATE_LIMIT 拦截、WHITELIST 放行
     * @param policyType 策略类型
     * @param actionType 动作
     */
    private void verifyActionConsistency(PolicyType policyType, PolicyAction actionType) {
        PolicyAction expected = policyType == PolicyType.WHITELIST
                ? PolicyAction.ALLOW : PolicyAction.BLOCK;
        if (actionType != expected) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    policyType + " 策略动作必须为 " + expected.name());
        }
    }

    /**
     * 受限 Ant 风格路径模板校验：必须以 / 开头，仅允许白名单字符，拒绝正则元字符
     * @param apiPathPattern 路径模板；空表示匹配全部
     */
    private void verifyPathPattern(String apiPathPattern) {
        if (StrUtil.isBlank(apiPathPattern)) {
            return;
        }
        if (!apiPathPattern.startsWith("/")) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "API 路径模板必须以 / 开头");
        }
        for (char ch : apiPathPattern.toCharArray()) {
            if (!PATH_ALLOWED_CHARS.contains(ch)) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                        "API 路径模板仅支持受限 Ant 风格（字母/数字/-/_///*/{），不接受正则表达式");
            }
        }
    }

    /**
     * 过期时间校验：必须晚于当前时间
     * @param expireTime 过期时间
     */
    private void verifyExpireTime(OffsetDateTime expireTime) {
        if (expireTime != null && !expireTime.isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "过期时间必须晚于当前时间");
        }
    }

    /**
     * 加载必须存在的策略
     * @param policyId 策略 ID
     * @return 策略记录
     */
    private UserApiPolicy getRequiredPolicy(Long policyId) {
        UserApiPolicy policy = lambdaQuery()
                .eq(UserApiPolicy::getId, policyId)
                .one();
        if (policy == null) {
            // 100xxx 暂无 API 策略不存在专用错误码，按既有先例使用参数校验错误并携带提示信息。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "API 策略不存在");
        }
        return policy;
    }

    /**
     * 100xxx 暂无 API 策略版本冲突专用错误码，按既有先例使用参数校验错误并携带提示信息
     * @return 版本冲突异常
     */
    private BusinessException versionConflict() {
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "API 策略已被其他请求修改，请刷新后重试");
    }

    /**
     * 组装创建结果 VO
     * @param policy 策略记录
     * @return 创建结果 VO
     */
    private ApiPolicyMutationVO toMutationVO(UserApiPolicy policy) {
        return ApiPolicyMutationVO.builder()
                .id(policy.getId())
                .userId(policy.getUserId())
                .policyType(policy.getPolicyType())
                .actionType(policy.getActionType())
                .version(policy.getVersion())
                .build();
    }

    /**
     * 构建路径模板白名单字符集合
     * @return 白名单字符集合
     */
    private static Set<Character> buildAllowedPathChars() {
        Set<Character> chars = new HashSet<>();
        for (char ch = 'a'; ch <= 'z'; ch++) {
            chars.add(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            chars.add(ch);
        }
        for (char ch = '0'; ch <= '9'; ch++) {
            chars.add(ch);
        }
        chars.add('-');
        chars.add('_');
        chars.add('/');
        chars.add('*');
        chars.add('{');
        chars.add('}');
        return chars;
    }
}
