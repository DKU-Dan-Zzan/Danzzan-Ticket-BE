package com.danzzan.ticketing.domain.ticket.redis;

public final class TicketRedisKeys {

    private static final String PREFIX = "ticket";
    private static final String COLON_ESCAPE = "%3A";

    private TicketRedisKeys() {
    }

    public static String stockKey(String eventId) {
        return PREFIX + ":" + keyPart(eventId, "eventId") + ":stock";
    }

    public static String userKey(String eventId, String userId) {
        return PREFIX + ":" + keyPart(eventId, "eventId") + ":user:" + keyPart(userId, "userId");
    }

    public static String statusKey(String eventId, String userId) {
        return PREFIX + ":" + keyPart(eventId, "eventId") + ":status:" + keyPart(userId, "userId");
    }

    public static String queueKey(String eventId) {
        return PREFIX + ":" + keyPart(eventId, "eventId") + ":queue";
    }

    public static String gateKey(String eventId) {
        return PREFIX + ":" + keyPart(eventId, "eventId") + ":gate";
    }

    private static String keyPart(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return raw.trim().replace(":", COLON_ESCAPE);
    }
}
