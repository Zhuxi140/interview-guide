package interview.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.rag.model.entity.InterviewTemplateKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试模板-知识库关联 Mapper。
 */
@Mapper
public interface InterviewTemplateKnowledgeBaseMapper
        extends BaseMapper<InterviewTemplateKnowledgeBase> {
}
