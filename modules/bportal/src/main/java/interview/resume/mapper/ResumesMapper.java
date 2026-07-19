package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.Resumes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 简历底座表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
@Mapper
public interface ResumesMapper extends BaseMapper<Resumes> {

    @Select("""
    SELECT
        storage_url
    FROM resumes
    WHERE is_deleted = true AND user_id = #{userId} AND file_hash = #{hash}
    """)
    String selectDeletedByHash(String hash,Long userId);

}
