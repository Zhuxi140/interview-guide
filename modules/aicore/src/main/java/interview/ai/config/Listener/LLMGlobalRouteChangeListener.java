package interview.ai.config.Listener;

import interview.ai.config.event.LLMGlobalRouteChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LLMGlobalRouteChangeListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void clearGlobalRouteCache(LLMGlobalRouteChangeEvent event) {
        Cache cache = cacheManager.getCache("llmGlobalRoute");
        if (cache != null) {
            cache.clear();
        }
        Cache routerCache = cacheManager.getCache("routeInfo");
        if (routerCache != null) {
            routerCache.clear();
        }
    }
}
