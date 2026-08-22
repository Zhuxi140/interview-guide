package interview.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.rag.model.entity.RagChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 对话会话 Mapper。
 */
@Mapper
public interface RagChatSessionMapper extends BaseMapper<RagChatSession> {
}
