package interview.data.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.mapper.DataRetentionPolicyMapper;
import interview.data.model.entity.DataRetentionPolicy;
import interview.data.model.enums.ArchiveResourceType;
import interview.data.model.req.DataRetentionPoliciesUpdateReq;
import interview.data.model.req.RetentionPolicyItemReq;
import interview.data.model.vo.DataRetentionPoliciesVO;
import interview.data.model.vo.RetentionPolicyItemVO;
import interview.data.service.DataRetentionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数据保留策略单行配置服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class DataRetentionPolicyServiceImpl
        extends ServiceImpl<DataRetentionPolicyMapper, DataRetentionPolicy>
        implements DataRetentionPolicyService {

    /**
     * 单行配置固定主键。
     */
    private static final long SINGLE_ROW_ID = 1L;
    private static final int DEFAULT_HOT_RETENTION_DAYS = 30;

    private final DataRetentionPolicyMapper dataRetentionPolicyMapper;

    @Override
    public DataRetentionPoliciesVO getPolicies() {
        // 读取单行配置；未初始化时返回内置默认策略（version=0），不落库。
        DataRetentionPolicy policy = dataRetentionPolicyMapper.selectById(SINGLE_ROW_ID);
        if (policy == null) {
            return new DataRetentionPoliciesVO(0, defaultPolicies(), null);
        }
        return new DataRetentionPoliciesVO(
                policy.getVersion(), parsePolicies(policy.getPoliciesJson()),
                policy.getUpdatedAt());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public DataRetentionPoliciesVO updatePolicies(DataRetentionPoliciesUpdateReq req) {
        // 校验策略项：资源类型合法且不重复。
        Map<String, RetentionPolicyItemReq> itemMap = req.getPolicies().stream()
                .collect(Collectors.toMap(
                        RetentionPolicyItemReq::getResourceType,
                        item -> item,
                        (left, right) -> {
                            throw new BusinessException(
                                    ErrorCode.PARAM_VALID_ERROR, "资源类型重复");
                        }));
        itemMap.keySet().forEach(this::requireResourceType);
        // 开启归档时必须配置冷数据保留天数。
        for (RetentionPolicyItemReq item : req.getPolicies()) {
            if (Boolean.TRUE.equals(item.getArchiveEnabled()) && item.getColdRetentionDays() == null) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                        "开启归档的资源必须配置冷数据保留天数");
            }
        }

        // 读取当前单行配置：不存在时仅接受 expectedVersion=0 并插入首行。
        DataRetentionPolicy current = dataRetentionPolicyMapper.selectById(SINGLE_ROW_ID);
        if (current == null) {
            if (req.getExpectedVersion() != 0) {
                // TODO: ErrorCode 缺少 DATA_RETENTION_VERSION_CONFLICT，暂以参数错误语义返回。
                throw new BusinessException(
                        ErrorCode.PARAM_VALID_ERROR, "保留策略已被修改，请刷新后重试");
            }
            DataRetentionPolicy inserted = DataRetentionPolicy.builder()
                    .id(SINGLE_ROW_ID)
                    .policiesJson(toPoliciesJson(req.getPolicies()))
                    .version(0)
                    .build();
            dataRetentionPolicyMapper.insert(inserted);
        } else {
            // 聚合版本 CAS：实体携带当前版本由乐观锁递增，零行更新视为冲突。
            if (!Objects.equals(current.getVersion(), req.getExpectedVersion())) {
                throw new BusinessException(
                        ErrorCode.PARAM_VALID_ERROR, "保留策略已被修改，请刷新后重试");
            }
            DataRetentionPolicy update = DataRetentionPolicy.builder()
                    .id(SINGLE_ROW_ID)
                    .version(req.getExpectedVersion())
                    .policiesJson(toPoliciesJson(req.getPolicies()))
                    .build();
            int affected = dataRetentionPolicyMapper.updateById(update);
            if (affected == 0) {
                throw new BusinessException(
                        ErrorCode.PARAM_VALID_ERROR, "保留策略已被修改，请刷新后重试");
            }
        }
        // TODO: 归档执行器接入后，在此发布策略变更事件刷新定时归档任务的调度参数。
        return getPolicies();
    }

    /**
     * 内置默认策略：全部资源热保留 30 天、关闭归档。
     */
    private List<RetentionPolicyItemVO> defaultPolicies() {
        List<RetentionPolicyItemVO> policies = new ArrayList<>();
        for (ArchiveResourceType resourceType : ArchiveResourceType.values()) {
            policies.add(new RetentionPolicyItemVO(
                    resourceType.name(), DEFAULT_HOT_RETENTION_DAYS, false, null));
        }
        return policies;
    }

    /**
     * 解析策略 JSON 为展示列表；解析失败时退回默认策略。
     */
    private List<RetentionPolicyItemVO> parsePolicies(String policiesJson) {
        if (policiesJson == null || policiesJson.isBlank()) {
            return defaultPolicies();
        }
        try {
            JSONArray array = JSONUtil.parseArray(policiesJson);
            List<RetentionPolicyItemVO> policies = new ArrayList<>();
            for (Object element : array) {
                JSONObject item = (JSONObject) element;
                policies.add(new RetentionPolicyItemVO(
                        item.getStr("resourceType"),
                        item.getInt("hotRetentionDays"),
                        item.getBool("archiveEnabled"),
                        item.getInt("coldRetentionDays")));
            }
            return policies;
        } catch (RuntimeException exception) {
            return defaultPolicies();
        }
    }

    /**
     * 将策略请求列表序列化为存储 JSON，保持入参顺序。
     */
    private String toPoliciesJson(List<RetentionPolicyItemReq> policies) {
        JSONArray array = new JSONArray();
        for (RetentionPolicyItemReq item : policies) {
            JSONObject json = new JSONObject();
            json.set("resourceType", item.getResourceType());
            json.set("hotRetentionDays", item.getHotRetentionDays());
            json.set("archiveEnabled", item.getArchiveEnabled());
            json.set("coldRetentionDays", item.getColdRetentionDays());
            array.add(json);
        }
        return array.toString();
    }

    /**
     * 校验资源类型枚举取值。
     */
    private void requireResourceType(String resourceType) {
        try {
            ArchiveResourceType.valueOf(resourceType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "资源类型取值不合法");
        }
    }
}
