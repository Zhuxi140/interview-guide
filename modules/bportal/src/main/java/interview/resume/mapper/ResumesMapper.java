package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Select("SELECT pg_advisory_xact_lock(#{lockKey})")
    void lockResumes(@Param("lockKey") Long lockKey);

    @Update("""
    UPDATE resumes SET
    analyze_status = #{status},
    is_deleted = true
    WHERE id = #{resumeId}
        AND analyze_status = 'UPLOADING'
        AND is_deleted = false
    """)
    void markUploadFailed(Long resumeId, AnalyzeStatus status);
}
