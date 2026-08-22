package interview.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.rag.model.entity.RagChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 对话消息 Mapper。
 */
@Mapper
public interface RagChatMessageMapper extends BaseMapper<RagChatMessage> {
}
