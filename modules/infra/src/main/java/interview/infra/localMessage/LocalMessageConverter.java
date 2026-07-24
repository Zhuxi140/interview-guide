package interview.infra.localMessage;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.model.vo.LocalMessageDetailVO;
import interview.infra.localMessage.model.vo.LocalMessagePageVO;
import interview.infra.localMessage.model.vo.LocalMessageRetryVO;
import org.mapstruct.Mapper;

/**
 * 本地消息管理端响应转换器。
 */
@Mapper(componentModel = "spring")
public interface LocalMessageConverter {

    default String map(MsgTopic topic) { return topic.name(); }

    default String map(MsgPriority priority) { return priority.name(); }

    default String map(MsgStatus status) { return status.name(); }

    LocalMessagePageVO toPageVO(LocalMessage message);

    LocalMessageDetailVO toDetailVO(LocalMessage message);

    LocalMessageRetryVO toRetryVO(LocalMessage message);

    default IPage<LocalMessagePageVO> toPageVO(IPage<LocalMessage> source) {
        Page<LocalMessagePageVO> target =
                new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream().map(this::toPageVO).toList());
        return target;
    }
}
