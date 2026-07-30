package interview.ai.config.Listener;


import interview.ai.config.event.LLMSceneChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LLMSceneChangedListener {

    private final CacheManager cacheManager;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void clearSceneCache(LLMSceneChangeEvent event){
        Cache cache = cacheManager.getCache("llmScene");
        if (cache != null){
            cache.evict(event.sceneCode());
        }
        Cache routerCache = cacheManager.getCache("routeInfo");
        if (routerCache != null) {
            routerCache.clear();
        }
    }
}
