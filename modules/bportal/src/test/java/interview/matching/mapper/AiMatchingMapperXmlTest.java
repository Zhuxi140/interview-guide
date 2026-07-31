package interview.matching.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMatchingMapperXmlTest {

    @Test
    void mapperXml_shouldLoadAiTaskAndLockStatements() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        load(configuration, "mapper/JobApplicationsMapper.xml");
        load(configuration, "mapper/ApplicationAiScreeningMapper.xml");
        load(configuration, "mapper/CandidateJobMatchAnalysisMapper.xml");
        load(configuration, "mapper/CandidateProfileMapper.xml");

        assertStatement(configuration, JobApplicationsMapper.class,
                "selectByIdForUpdate");
        assertStatement(configuration, ApplicationAiScreeningMapper.class,
                "claimScreening");
        assertStatement(configuration, CandidateJobMatchAnalysisMapper.class,
                "claimAnalysis");
        assertStatement(configuration,
                interview.resume.mapper.CandidateProfileMapper.class,
                "claimProfile");
    }

    @Test
    void mapperXml_shouldKeepAiDomainsSeparateAndFenceExecutions() {
        String applications = resourceText("mapper/JobApplicationsMapper.xml");
        String candidateList = applications.substring(
                applications.indexOf("id=\"pageMyApplicationsWithJoin\""));
        assertTrue(candidateList.contains("candidate_job_match_analyses"));
        assertFalse(candidateList.contains("application_ai_screenings"));

        String profile = resourceText("mapper/CandidateProfileMapper.xml");
        assertFalse(profile.contains("resume_analyses"));
        assertTrue(profile.contains("attempt_count = #{attemptCount}"));
        assertTrue(profile.contains("status = 'PROCESSING'"));

        String screening = resourceText(
                "mapper/ApplicationAiScreeningMapper.xml");
        String matching = resourceText(
                "mapper/CandidateJobMatchAnalysisMapper.xml");
        assertTrue(screening.contains("attempt_count = #{attemptCount}"));
        assertTrue(matching.contains("attempt_count = #{attemptCount}"));
    }

    private void load(MybatisConfiguration configuration, String resource) {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments()).parse();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void assertStatement(MybatisConfiguration configuration,
                                 Class<?> mapperType,
                                 String statement) {
        assertTrue(configuration.hasStatement(
                mapperType.getName() + "." + statement));
    }

    private String resourceText(String resource) {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
