package interview.api.infra;

import interview.common.enums.FileSort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageApi {

    String uploadFile(MultipartFile file, FileSort prefix);

    boolean fileExists(String key);

    byte[] downloadFile(String key);

    void deleteFile(String key);

    String verifyFileType(long size, String originName, InputStream fileInputStream) throws IOException;
}
