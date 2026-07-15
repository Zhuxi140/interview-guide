package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.ResumeAnalyses;
import interview.resume.model.vo.ResumeAnalysisVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 简历 AI 分析结果表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */

@Mapper
public interface ResumeAnalysesMapper extends BaseMapper<ResumeAnalyses> {

    ResumeAnalysisVO getResumeAnalysisVO();
}
