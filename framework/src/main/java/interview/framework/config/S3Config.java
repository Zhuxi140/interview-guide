package interview.framework.config;


import interview.framework.config.properties.StorageConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * @author zhuxi
 */
@Configuration
@EnableConfigurationProperties(StorageConfigProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(StorageConfigProperties storage) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(storage.getAccessKey(), storage.getSecretKey());


        return S3Client.builder()
                .endpointOverride(URI.create(storage.getEndpoint()))
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }

}
