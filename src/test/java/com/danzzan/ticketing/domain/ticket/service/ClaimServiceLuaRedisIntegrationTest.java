package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import com.danzzan.ticketing.domain.ticket.redis.TicketRequestStatus;
import com.danzzan.ticketing.domain.ticket.service.model.ClaimResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = ".*local-compose.*")
class ClaimServiceLuaRedisIntegrationTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/claim_v2.lua"));
        script.setResultType(List.class);

        claimService = new ClaimServiceImpl(redisTemplate, script);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void claimSuccessReturnsRemainingAndPersistsStatus() {
        String eventId = "it-success-" + UUID.randomUUID();
        String userId = "u1";

        redisTemplate.opsForValue().set(TicketRedisKeys.stockKey(eventId), "1");

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.SUCCESS);
        assertThat(result.remaining()).isEqualTo(0L);
        assertThat(redisTemplate.opsForValue().get(TicketRedisKeys.statusKey(eventId, userId)))
                .isEqualTo(TicketRequestStatus.SUCCESS.name());
        assertThat(redisTemplate.opsForValue().get(TicketRedisKeys.userKey(eventId, userId)))
                .isEqualTo("1");
    }

    @Test
    void claimSoldOutReturnsNullRemainingAndPersistsStatus() {
        String eventId = "it-soldout-" + UUID.randomUUID();
        String userId = "u1";

        redisTemplate.opsForValue().set(TicketRedisKeys.stockKey(eventId), "0");

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.SOLD_OUT);
        assertThat(result.remaining()).isNull();
        assertThat(redisTemplate.opsForValue().get(TicketRedisKeys.statusKey(eventId, userId)))
                .isEqualTo(TicketRequestStatus.SOLD_OUT.name());
        assertThat(redisTemplate.opsForValue().get(TicketRedisKeys.userKey(eventId, userId)))
                .isNull();
    }

    @Test
    void claimAlreadyReturnsNullRemainingAndDoesNotDecrementStock() {
        String eventId = "it-already-" + UUID.randomUUID();
        String userId = "u1";
        String stockKey = TicketRedisKeys.stockKey(eventId);
        String userKey = TicketRedisKeys.userKey(eventId, userId);

        redisTemplate.opsForValue().set(stockKey, "5");
        redisTemplate.opsForValue().set(userKey, "1");

        ClaimResult result = claimService.claim(eventId, userId);

        assertThat(result.status()).isEqualTo(TicketRequestStatus.ALREADY);
        assertThat(result.remaining()).isNull();
        assertThat(redisTemplate.opsForValue().get(TicketRedisKeys.statusKey(eventId, userId)))
                .isEqualTo(TicketRequestStatus.ALREADY.name());
        assertThat(redisTemplate.opsForValue().get(stockKey)).isEqualTo("5");
    }
}
