package interview.api.infra;

import interview.common.enums.FileSort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public interface FileStorageApi {

    String uploadFile(MultipartFile file, FileSort prefix,String existingUrl);

    boolean fileExists(String key);

    byte[] downloadFile(String key);

    void deleteFile(String key);

    String verifyFileType(long size, String originName, InputStream fileInputStream) throws IOException;

    String generateFileKey(String originalFilename,FileSort prefix);

    /**
     * 生成对象的短期下载地址
     * @param key 对象键
     * @param duration 地址有效期
     * @return 预签名下载地址
     */
    String generatePresignedDownloadUrl(String key, Duration duration);
}
