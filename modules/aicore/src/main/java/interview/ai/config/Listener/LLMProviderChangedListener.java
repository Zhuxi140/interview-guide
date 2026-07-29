package interview.ai.config.Listener;


import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LLMProviderChangedListener {

    private final CacheManager cacheManager;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void clearProviderCache() {
        // Implementation for clearing provider cache
    }
}
