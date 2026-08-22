package com.interview.bootstrap;

import interview.common.enums.FileSort;
import interview.infra.config.S3Config;
import interview.infra.config.properties.StorageConfigProperties;
import interview.infra.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FileStorageTest.TestConfig.class)
class FileStorageTest {

    @Configuration
    @Import({S3Config.class, FileStorageService.class})
    @EnableConfigurationProperties(StorageConfigProperties.class)
    static class TestConfig {
    }

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void ensureBucket() {
        fileStorageService.ensureBucketExists();
    }

    @Test
    void testUploadPage1() throws Exception {
        Path filePath = Path.of("C:\\Users\\zhuxi\\Desktop\\InterviewGuide\\page-1.jpg");
        // 手工联调用例：本机无样例文件时跳过而不是失败。
        Assumptions.assumeTrue(Files.exists(filePath), "page-1.jpg not found");
        byte[] content = Files.readAllBytes(filePath);
        String originalName = filePath.getFileName().toString();
        String contentType = Files.probeContentType(filePath);

        MockMultipartFile file = new MockMultipartFile(
                "file", originalName, contentType, content
        );

        String key = fileStorageService.uploadFile(file, FileSort.RESUME, null);
        assertNotNull(key);
        assertFalse(key.isBlank());
        System.out.println("=== Upload OK ===");
        System.out.println("Key: " + key);
        System.out.println("Size: " + content.length + " bytes");

        assertTrue(fileStorageService.fileExists(key));
        System.out.println("fileExists: true");
    }
}
