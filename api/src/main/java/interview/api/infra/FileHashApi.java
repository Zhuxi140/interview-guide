package interview.api.infra;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * @author zhuxi
 */
public interface FileHashApi {

    String calculateHash(MultipartFile file);

    String calculateHash(byte[] bytes);

    String calculateHash(InputStream inputStream);
}
