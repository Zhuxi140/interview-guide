package interview.framework.file;


import cn.hutool.core.exceptions.UtilException;
import cn.hutool.crypto.digest.DigestUtil;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhuxi
 */

@Slf4j
@Service
public class FileHashService {

    /**
     * 计算 MultipartFile 的 SHA-256，避免全量读入内存
     */
    public String calculateHash(MultipartFile file){
        try(InputStream inputStream = file.getInputStream()){
            return DigestUtil.sha256Hex(inputStream);
        }catch (IOException e){
            log.error("读取上传文件流失败",e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 计算字节数组的哈希
     */
    public String calculateHash(byte[] bytes){
        return DigestUtil.sha256Hex(bytes);
    }

    /**
     * 计算输入流的哈希，该方法内部会关闭传入的流，调用方无需手动关闭。
     */
    public String calculateHash(InputStream inputStream){
        return DigestUtil.sha256Hex(inputStream);
    }
}
