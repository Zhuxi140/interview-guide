package interview.infra.file;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.crypto.digest.DigestUtil;
import interview.api.infra.FileHashApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FileHashService implements FileHashApi {

    public String calculateHash(MultipartFile file){
        try(InputStream inputStream = file.getInputStream()){
            return DigestUtil.sha256Hex(inputStream);
        }catch (IOException e){
            log.error("读取上传文件流失败",e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public String calculateHash(byte[] bytes){
        return DigestUtil.sha256Hex(bytes);
    }

    public String calculateHash(InputStream inputStream){
        return DigestUtil.sha256Hex(inputStream);
    }
}
