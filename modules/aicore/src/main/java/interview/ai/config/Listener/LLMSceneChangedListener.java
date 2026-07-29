package interview.ai.config.Listener;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LLMSceneChangedListener {


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void clearSceneCache(){

    }
}
