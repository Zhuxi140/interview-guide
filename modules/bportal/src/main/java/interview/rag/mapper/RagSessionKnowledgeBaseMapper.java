package interview.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.rag.model.entity.RagSessionKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 会话-知识库关联 Mapper。
 */
@Mapper
public interface RagSessionKnowledgeBaseMapper extends BaseMapper<RagSessionKnowledgeBase> {
}
