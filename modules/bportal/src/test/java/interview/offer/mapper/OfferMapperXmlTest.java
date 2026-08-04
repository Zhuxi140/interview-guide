package interview.offer.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferMapperXmlTest {

    @Test
    void candidateQueries_shouldLoadAndExcludeDrafts() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        String resource = "mapper/OfferMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments()).parse();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        assertTrue(configuration.hasStatement(
                OfferMapper.class.getName() + ".pageCandidateOffers"));
        assertTrue(configuration.hasStatement(
                OfferMapper.class.getName() + ".getCandidateOfferDetail"));
        assertTrue(resourceText(resource).contains("o.status &lt;&gt; 'DRAFT'"));
    }

    private String resourceText(String resource) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
