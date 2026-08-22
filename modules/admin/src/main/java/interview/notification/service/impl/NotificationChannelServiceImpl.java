package interview.notification.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.security.SecretCipher;
import interview.common.security.SecretCipherException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.notification.mapper.SysNotificationChannelMapper;
import interview.notification.model.entity.SysNotificationChannel;
import interview.notification.model.enums.ChannelType;
import interview.notification.model.req.NotificationChannelItemReq;
import interview.notification.model.req.NotificationChannelsUpdateReq;
import interview.notification.model.vo.NotificationChannelVO;
import interview.notification.model.vo.NotificationChannelsVO;
import interview.notification.service.NotificationChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通知发送渠道配置服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class NotificationChannelServiceImpl
        extends ServiceImpl<SysNotificationChannelMapper, SysNotificationChannel>
        implements NotificationChannelService {

    private static final String MASKED_SECRET = "******";
    /**
     * 凭证类字段名匹配：命中即在写入时加密、查询时脱敏。
     */
    private static final Pattern CREDENTIAL_KEY_PATTERN =
            Pattern.compile("(?i).*(password|passwd|secret|token|key).*");

    private final SecretCipher secretCipher;

    @Override
    public NotificationChannelsVO getChannels() {
        // 单表查询全部渠道行，按渠道类型稳定排序。
        List<SysNotificationChannel> channels = lambdaQuery()
                .select(SysNotificationChannel::getId, SysNotificationChannel::getChannelType,
                        SysNotificationChannel::getIsEnabled, SysNotificationChannel::getProvider,
                        SysNotificationChannel::getConfigJson, SysNotificationChannel::getVersion)
                .orderByAsc(SysNotificationChannel::getChannelType)
                .list();

        // 组装脱敏展示：凭证字段仅返回固定掩码，不暴露密文信封。
        List<NotificationChannelVO> channelVOs = channels.stream()
                .map(channel -> new NotificationChannelVO(
                        channel.getChannelType(),
                        channel.getIsEnabled(),
                        channel.getProvider(),
                        maskConfigSummary(channel.getConfigJson()),
                        channel.getVersion()))
                .toList();
        return new NotificationChannelsVO(channelVOs, aggregateVersion(channels));
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public NotificationChannelsVO updateChannels(NotificationChannelsUpdateReq req) {
        // 校验渠道项不重复，且 IN_APP 站内信恒为启用。
        Map<String, NotificationChannelItemReq> itemMap = req.getChannels().stream()
                .collect(Collectors.toMap(
                        NotificationChannelItemReq::getChannelType,
                        item -> item,
                        (left, right) -> {
                            throw new BusinessException(
                                    ErrorCode.PARAM_VALID_ERROR, "渠道类型重复");
                        }));
        NotificationChannelItemReq inApp = itemMap.get(ChannelType.IN_APP.name());
        if (inApp != null && !Boolean.TRUE.equals(inApp.getEnabled())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "IN_APP 站内信渠道不允许停用");
        }

        // 聚合版本 CAS：期望版本必须等于当前各行版本最大值。
        List<SysNotificationChannel> current = lambdaQuery().list();
        if (!Objects.equals(aggregateVersion(current), req.getExpectedVersion())) {
            // TODO: ErrorCode 缺少 NOTIFICATION_CHANNEL_VERSION_CONFLICT，暂以参数错误语义返回。
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "渠道配置已被修改，请刷新后重试");
        }

        // 逐渠道写入：凭证字段加密落盘，未提交 config 的渠道保留原参数。
        for (SysNotificationChannel row : current) {
            NotificationChannelItemReq item = itemMap.get(row.getChannelType());
            if (item == null) {
                continue;
            }
            Boolean enabled = ChannelType.IN_APP.name().equals(row.getChannelType())
                    ? Boolean.TRUE
                    : item.getEnabled();
            SysNotificationChannel update = new SysNotificationChannel();
            update.setId(row.getId());
            update.setVersion(row.getVersion());
            update.setIsEnabled(enabled);
            update.setConfigJson(encryptConfig(item.getConfig(), row.getConfigJson()));
            int affected = baseMapper.update(update,
                    Wrappers.<SysNotificationChannel>lambdaUpdate()
                            .eq(SysNotificationChannel::getId, row.getId())
                            .set(SysNotificationChannel::getProvider, item.getProvider()));
            if (affected == 0) {
                throw new BusinessException(
                        ErrorCode.PARAM_VALID_ERROR, "渠道配置已被修改，请刷新后重试");
            }
        }

        // 缺失的渠道行兜底补插，保证三渠道固定一行。
        for (NotificationChannelItemReq item : itemMap.values()) {
            if (current.stream().noneMatch(
                    row -> row.getChannelType().equals(item.getChannelType()))) {
                insertChannel(item);
            }
        }
        // TODO: 邮件/短信网关接入后，在此刷新发送客户端配置（解密凭证重建连接）。
        return getChannels();
    }

    /**
     * 兜底插入缺失的渠道行；IN_APP 恒为启用。
     */
    private void insertChannel(NotificationChannelItemReq item) {
        Boolean enabled = ChannelType.IN_APP.name().equals(item.getChannelType())
                ? Boolean.TRUE
                : item.getEnabled();
        SysNotificationChannel channel = SysNotificationChannel.builder()
                .channelType(item.getChannelType())
                .isEnabled(enabled)
                .provider(item.getProvider())
                .configJson(encryptConfig(item.getConfig(), null))
                .version(0)
                .build();
        baseMapper.insert(channel);
    }

    /**
     * 计算聚合版本号：各行版本最大值，无渠道行时为 0。
     */
    private Integer aggregateVersion(List<SysNotificationChannel> channels) {
        return channels.stream()
                .map(SysNotificationChannel::getVersion)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 写入侧处理渠道参数：凭证字段加密为密文信封；未提交新参数时保留原密文。
     */
    private String encryptConfig(Map<String, Object> config, String existingConfigJson) {
        if (config == null) {
            return existingConfigJson;
        }
        JSONObject encrypted = new JSONObject();
        config.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            String plain = String.valueOf(value);
            if (isCredentialKey(key) && !plain.isBlank()) {
                try {
                    encrypted.set(key, secretCipher.encrypt(plain));
                } catch (SecretCipherException | IllegalArgumentException exception) {
                    throw new BusinessException(ErrorCode.ENCRYPTION_ERROR);
                }
            } else {
                encrypted.set(key, plain);
            }
        });
        return encrypted.toString();
    }

    /**
     * 查询侧脱敏：凭证字段替换为固定掩码后返回紧凑 JSON 摘要。
     */
    private String maskConfigSummary(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            JSONObject raw = JSONUtil.parseObj(configJson);
            JSONObject masked = new JSONObject();
            raw.forEach((key, value) -> masked.set(
                    key, isCredentialKey(key) ? MASKED_SECRET : value));
            return masked.toString();
        } catch (RuntimeException exception) {
            // 无法解析的存量数据一律整体脱敏。
            return MASKED_SECRET;
        }
    }

    /**
     * 判断渠道参数键是否属于凭证类字段。
     */
    private boolean isCredentialKey(String key) {
        return key != null && CREDENTIAL_KEY_PATTERN.matcher(key).matches();
    }
}
