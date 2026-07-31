package interview.resume.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumesMapperXmlTest {

    @Test
    void mapperXml_shouldLoadAnalysisStatements() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/ResumesMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(
                    input, configuration, "mapper/ResumesMapper.xml",
                    configuration.getSqlFragments()).parse();
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        String namespace = ResumesMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "claimAnalysis"));
        assertTrue(configuration.hasStatement(namespace + "completeAnalysis"));
        assertTrue(configuration.hasStatement(
                namespace + "failExhaustedAnalysis"));
    }
}
