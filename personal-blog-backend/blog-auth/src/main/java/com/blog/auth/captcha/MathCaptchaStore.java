package com.blog.auth.captcha;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MathCaptchaStore {

    private record Entry(long expiresAtMillis, int answer) {
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public record Challenge(String captchaId, String question) {
    }

    public Challenge nextChallenge() {
        int a = 1 + random.nextInt(12);
        int b = 1 + random.nextInt(12);
        boolean add = random.nextBoolean();
        int answer = add ? a + b : a - b;
        String q = add ? (a + " + " + b + " = ?") : (a + " − " + b + " = ?");
        String id = UUID.randomUUID().toString();
        store.put(id, new Entry(System.currentTimeMillis() + 600_000L, answer));
        return new Challenge(id, q);
    }

    public boolean verifyAndConsume(String captchaId, Integer answer) {
        if (captchaId == null || answer == null) {
            return false;
        }
        Entry e = store.remove(captchaId);
        if (e == null || System.currentTimeMillis() > e.expiresAtMillis) {
            return false;
        }
        return e.answer == answer.intValue();
    }
}
