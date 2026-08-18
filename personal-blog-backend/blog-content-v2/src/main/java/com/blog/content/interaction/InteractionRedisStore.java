package com.blog.content.interaction;

import com.blog.content.config.interaction.InteractionProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class InteractionRedisStore {
    private static final String LIKED_PREFIX = "interaction:liked:";
    private static final String FAV_PREFIX = "interaction:fav:";
    private static final String COUNT_PREFIX = "interaction:like:count:";
    private static final String DIRTY_LIKES = "interaction:dirty:likes";

    private final StringRedisTemplate redis;
    private final InteractionProperties properties;

    public InteractionRedisStore(StringRedisTemplate redis, InteractionProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean redisEnabled() {
        return properties.isRedisCountEnabled();
    }

    public Boolean isLikedCached(Long userId, Long articleId) {
        if (!redisEnabled() || userId == null || articleId == null) {
            return null;
        }
        return Boolean.TRUE.equals(redis.opsForSet().isMember(likedKey(userId), String.valueOf(articleId)));
    }

    public void setLiked(Long userId, Long articleId, boolean liked) {
        if (!redisEnabled() || userId == null || articleId == null) {
            return;
        }
        String key = likedKey(userId);
        String member = String.valueOf(articleId);
        if (liked) {
            redis.opsForSet().add(key, member);
        } else {
            redis.opsForSet().remove(key, member);
        }
    }

    public Integer getLikeCountCached(Long articleId) {
        if (!redisEnabled() || articleId == null) {
            return null;
        }
        String raw = redis.opsForValue().get(COUNT_PREFIX + articleId);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void seedLikeCount(Long articleId, int count) {
        if (!redisEnabled() || articleId == null) {
            return;
        }
        redis.opsForValue().set(COUNT_PREFIX + articleId, String.valueOf(Math.max(0, count)));
    }

    public LikeToggleResult toggleLike(Long userId, Long articleId, boolean currentlyLiked) {
        boolean liked = !currentlyLiked;
        int count = 0;
        if (redisEnabled()) {
            setLiked(userId, articleId, liked);
            String countKey = COUNT_PREFIX + articleId;
            if (liked) {
                Long v = redis.opsForValue().increment(countKey);
                count = v != null ? v.intValue() : 1;
            } else {
                Long v = redis.opsForValue().decrement(countKey);
                count = v != null ? Math.max(0, v.intValue()) : 0;
                if (count == 0) {
                    redis.opsForValue().set(countKey, "0");
                }
            }
            redis.opsForSet().add(DIRTY_LIKES, String.valueOf(articleId));
        }
        return new LikeToggleResult(liked, count);
    }

    public Boolean isFavoritedCached(Long userId, Long articleId) {
        if (!redisEnabled() || userId == null || articleId == null) {
            return null;
        }
        return Boolean.TRUE.equals(redis.opsForSet().isMember(favKey(userId), String.valueOf(articleId)));
    }

    public void setFavorited(Long userId, Long articleId, boolean favorited) {
        if (!redisEnabled() || userId == null || articleId == null) {
            return;
        }
        String member = String.valueOf(articleId);
        if (favorited) {
            redis.opsForSet().add(favKey(userId), member);
        } else {
            redis.opsForSet().remove(favKey(userId), member);
        }
    }

    public Set<String> dirtyLikeArticleIds() {
        Set<String> members = redis.opsForSet().members(DIRTY_LIKES);
        return members != null ? members : Set.of();
    }

    public void clearDirtyLike(Long articleId) {
        if (articleId != null) {
            redis.opsForSet().remove(DIRTY_LIKES, String.valueOf(articleId));
        }
    }

    private static String likedKey(Long userId) {
        return LIKED_PREFIX + userId;
    }

    private static String favKey(Long userId) {
        return FAV_PREFIX + userId;
    }

    public record LikeToggleResult(boolean liked, int count) {
    }
}
