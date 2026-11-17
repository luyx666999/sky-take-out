package com.sky.test;

import com.sky.config.RedisConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import(RedisConfiguration.class)
class SpringDataRedisIT {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final String prefix = "day05:practice:" + UUID.randomUUID() + ":";
    private final List<String> keys = new ArrayList<>();

    @AfterEach
    void deletePracticeKeys() {
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void stringCommands() {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        String name = key("name");
        String code = key("code");
        String lock = key("lock");

        values.set(name, "小明");
        assertThat(values.get(name)).isEqualTo("小明");

        values.set(code, "1234", Duration.ofMinutes(2));
        assertThat(values.get(code)).isEqualTo("1234");
        assertThat(redisTemplate.getExpire(code)).isBetween(1L, 120L);

        assertThat(values.setIfAbsent(lock, "1")).isTrue();
        assertThat(values.setIfAbsent(lock, "2")).isFalse();
        assertThat(values.get(lock)).isEqualTo("1");
    }

    @Test
    void hashCommands() {
        HashOperations<String, String, Object> hash = redisTemplate.opsForHash();
        String user = key("100");

        hash.put(user, "name", "tom");
        hash.put(user, "age", "20");

        assertThat(hash.get(user, "name")).isEqualTo("tom");
        assertThat(hash.keys(user)).containsExactlyInAnyOrder("name", "age");
        assertThat(hash.values(user)).containsExactlyInAnyOrder("tom", "20");
        assertThat(hash.delete(user, "age")).isEqualTo(1L);
        assertThat(hash.get(user, "age")).isNull();
    }

    @Test
    void listCommands() {
        ListOperations<String, Object> lists = redisTemplate.opsForList();
        String list = key("mylist");

        lists.leftPushAll(list, "a", "b", "c");
        lists.leftPush(list, "d");

        assertThat(lists.range(list, 0, -1)).containsExactly("d", "c", "b", "a");
        assertThat(lists.rightPop(list)).isEqualTo("a");
        assertThat(lists.size(list)).isEqualTo(3L);
    }

    @Test
    void setCommands() {
        SetOperations<String, Object> sets = redisTemplate.opsForSet();
        String first = key("set1");
        String second = key("set2");

        sets.add(first, "a", "b", "c", "d");
        sets.add(second, "a", "b", "x", "y");

        assertThat(sets.size(first)).isEqualTo(4L);
        assertThat(sets.intersect(first, second)).containsExactlyInAnyOrder("a", "b");
        assertThat(sets.union(first, second)).containsExactlyInAnyOrder("a", "b", "c", "d", "x", "y");
        assertThat(sets.remove(first, "a", "b")).isEqualTo(2L);
        assertThat(sets.members(first)).containsExactlyInAnyOrder("c", "d");
    }

    @Test
    void sortedSetCommands() {
        ZSetOperations<String, Object> sortedSets = redisTemplate.opsForZSet();
        String sortedSet = key("zset1");

        sortedSets.add(sortedSet, "a", 10);
        sortedSets.add(sortedSet, "b", 12);
        sortedSets.add(sortedSet, "c", 9);

        assertThat(sortedSets.range(sortedSet, 0, -1)).containsExactly("c", "a", "b");
        assertThat(sortedSets.incrementScore(sortedSet, "c", 10)).isEqualTo(19);
        assertThat(sortedSets.remove(sortedSet, "a", "b")).isEqualTo(2L);
        assertThat(sortedSets.range(sortedSet, 0, -1)).containsExactly("c");
    }

    @Test
    void keyCommands() {
        String status = key("status");
        redisTemplate.opsForValue().set(status, "open");

        assertThat(redisTemplate.keys(prefix + "*")).containsExactly(status);
        assertThat(redisTemplate.hasKey(status)).isTrue();
        assertThat(redisTemplate.type(status)).isEqualTo(DataType.STRING);
        assertThat(redisTemplate.delete(status)).isTrue();
        assertThat(redisTemplate.hasKey(status)).isFalse();
    }

    private String key(String name) {
        String key = prefix + name;
        keys.add(key);
        return key;
    }
}
