package hr.orders.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private static final String REDIS_PREFIX = "hr-process-orders:processed-commands";
    private static final long DEDUPLICATION_TTL_HOURS = 24;
    private final RedisTemplate<String, String> redisTemplate;

    public boolean checkAndMarkProcessed(String simpleName, UUID commandId) {
        String key = buildKey(simpleName, commandId);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "processed", DEDUPLICATION_TTL_HOURS, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(success)) {
            log.debug("Duplicate detected: key={}", key);
            return false;
        } else {
            log.debug("Marked as processed: key={}", key);
            return true;
        }
    }

    private String buildKey(String simpleName, UUID commandId) {
        return String.format("%s:%s:%s", REDIS_PREFIX, simpleName, commandId);
    }
}
