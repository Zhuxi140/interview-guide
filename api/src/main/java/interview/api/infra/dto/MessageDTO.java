package interview.api.infra.dto;

import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */

@Data
@Builder
public class MessageDTO {

    private MsgTopic topic;
    private String bizKey;
    private Integer schemaVersion;
    private String payload;
    private MsgStatus status;
    private Integer retryCount;
    private Integer maxRetries;
    private OffsetDateTime nextRetryAt;
    private String retryHistory;
    private String lastError;
    private MsgPriority priority;

}
