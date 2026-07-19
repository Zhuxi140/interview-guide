package interview.api.infra;

import org.springframework.web.multipart.MultipartFile;

public interface FileParseApi {

    String parseText(MultipartFile file);
}
