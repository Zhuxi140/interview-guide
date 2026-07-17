package interview.framework.initializer;

import interview.framework.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author zhuxi
 */
@Component
@RequiredArgsConstructor
public class StorageInitializer implements ApplicationRunner {

    private final FileStorageService storageService;

    @Override
    public void run(ApplicationArguments args) {
        storageService.ensureBucketExists();
    }
}