package interview.infra.file;

import interview.common.constant.Message;
import interview.common.enums.MsgTopic;
import interview.common.spi.MessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileDeleteHandler implements MessageHandler {

    private final FileStorageService fileStorageService;

    @Override
    public MsgTopic getTopic() {
        return MsgTopic.FILE_DELETE;
    }

    @Override
    public void handle(Message msg) {
        fileStorageService.deleteFile(msg.payload());
    }
}
