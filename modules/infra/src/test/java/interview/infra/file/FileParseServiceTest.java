package interview.infra.file;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FileParseServiceTest {

    private final FileParseService fileParseService = new FileParseService();

    @Test
    void parseDocx_shouldExtractText() throws Exception {
        String path = "C:\\Users\\zhuxi\\Desktop\\InterviewGuide\\简历模板.docx";
        try (InputStream is = new FileInputStream(path)) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "简历模板.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", is);

            String text = fileParseService.parseText(file);
            System.out.println("===== 提取结果 =====");
            System.out.println(text);
            System.out.println("===== 长度: " + text.length() + " =====");
            assertNotNull(text);
            assertFalse(text.isBlank());
            assertTrue(text.length() > 100);
            assertTrue(text.contains("姓名") || text.contains("求职意向") || text.contains("教育背景") || text.contains("工作经历"),
                    "提取文本应包含简历典型字段");
        }
    }
}
