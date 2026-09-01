package interview.candidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.candidate.model.entity.ResumeImportBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeImportBatchMapper extends BaseMapper<ResumeImportBatch> {
}
