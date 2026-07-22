package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
    WHERE is_deleted = true
      AND user_id = #{userId}
      AND file_hash = #{hash}
      AND analyze_status <> #{excludedStatus}
    ORDER BY updated_at DESC NULLS LAST, created_at DESC
    LIMIT 1
    """)
    String selectReusableDeletedByHash(@Param("hash") String hash,
                                       @Param("userId") Long userId,
                                       @Param("excludedStatus") AnalyzeStatus excludedStatus);

    @Select("SELECT pg_advisory_xact_lock(#{namespace}, #{ownerSlot})")
    void lockResumes(@Param("namespace") int namespace, @Param("ownerSlot") int ownerSlot);

    @Select("""
    SELECT *
    FROM resumes
    WHERE id = #{resumeId}
    """)
    Resumes selectIncludingDeletedById(@Param("resumeId") Long resumeId);

    @Select("""
    SELECT EXISTS(
        SELECT 1
        FROM resumes
        WHERE storage_url = #{storageUrl}
          AND id <> #{resumeId}
          AND is_deleted = false
    )
    """)
    boolean existsOtherAliveByStorageUrl(@Param("storageUrl") String storageUrl,
                                         @Param("resumeId") Long resumeId);
}
