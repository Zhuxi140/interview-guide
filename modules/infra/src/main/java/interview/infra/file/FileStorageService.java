package interview.infra.file;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import interview.api.infra.FileStorageApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.FileSort;
import interview.common.exception.BusinessException;
import interview.infra.config.properties.StorageConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService implements FileStorageApi {

    private final S3Client s3Client;
    private final StorageConfigProperties properties;

    Set<String> allowedTypes = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String uploadFile(MultipartFile file, FileSort prefix,String existingUrl){
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException(ErrorCode.INVALID_FILENAME);
        }
        String key = StrUtil.isNotBlank(existingUrl) ? existingUrl : generateFileKey(originalFilename, prefix);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(),file.getSize()));
            log.info("文件上传成功 {} -> {}",originalFilename,key);
            return key;
        }catch (IOException e){
            log.error("读取上传文件失败:{}",e.getMessage(),e);
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }catch (S3Exception e){
            checkStatusCode(e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public boolean fileExists(String key){
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        }catch (NoSuchKeyException e){
            return false;
        }catch (S3Exception e){
            checkStatusCode(e);
            log.warn("检查文件存在性失败: {} -> {}",key,e.getMessage(),e);
            return false;
        }
    }

    public byte[] downloadFile(String key){
        if (StrUtil.isBlank(key)) {
            throw new BusinessException(ErrorCode.INVALID_FILENAME);
        }

        if (!fileExists(key)){
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED,"文件不存在:" + key);
        }

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();
        }catch (S3Exception e){
            checkStatusCode(e);
            log.error("文件下载失败: {} -> {}",key,e.getMessage(),e);
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    public void deleteFile(String key){
        if (StrUtil.isBlank(key)){
            return;
        }
        try {
            // S3 DeleteObject 天然幂等，对象不存在同样视为成功。
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("文件删除成功: {}", key);
        }catch (NoSuchKeyException e){
            log.warn("文件不存在, 跳过删除: {}", key);
        }catch (S3Exception e){
            checkStatusCode(e);
            log.error("文件删除失败: {}", key, e);
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private void checkStatusCode(S3Exception e){
        if (e.statusCode() == 403){
            log.error("凭据验证有误。{}",e.getMessage(),e);
        }else if (e.statusCode() == 404){
            log.error("存储桶不存在。{}",e.getMessage(),e);
        }else if (e.statusCode() == 507){
            log.error("存储空间不足。{}",e.getMessage(),e);
        }else {
            log.error("未知错误。{}",e.getMessage(),e);
        }

    }

    public void  ensureBucketExists(){
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(properties.getBucket())
                    .build();
            s3Client.headBucket(request);
            log.info("存储桶已存在：{}",properties.getBucket());
        }catch (NoSuchBucketException e){
            log.info("存储桶不存在，正在创建:{}",properties.getBucket());
            CreateBucketRequest request = CreateBucketRequest
                    .builder()
                    .bucket(properties.getBucket())
                    .build();
            s3Client.createBucket(request);
            log.info("存储桶创建成功:{}",properties.getBucket());
        }catch (S3Exception e){
            checkStatusCode(e);
        }
    }

    public String generateFileKey(String originalFilename,FileSort prefix){

        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = IdUtil.fastSimpleUUID();

        String safeName;
        if (originalFilename == null){
            safeName = "unknown";
        }else{
            safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return String.format("%s/%s/%s_%s", prefix,datePath, uuid, safeName);
    }

    public String verifyFileType(long size, String originName, InputStream fileInputStream) throws IOException {
        if (size > 10 * 1024 * 1024 || size == 0){
            throw new BusinessException(ErrorCode.FILE_SIZE_TOO_LARGE_OR_EMPTY);
        }

        Tika tika = new Tika();
        String realType = tika.detect(fileInputStream);
        if (!allowedTypes.contains(realType)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_FILE_TYPE_NOT_SUPPORTED);
        }

        if (originName != null){
            String ext = originName.substring(originName.lastIndexOf(".") + 1).toLowerCase();
            if (!Set.of("pdf","doc","docx").contains(ext)){
                throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_FILE_TYPE_NOT_SUPPORTED);
            }
        }

        return realType;
    }
}
