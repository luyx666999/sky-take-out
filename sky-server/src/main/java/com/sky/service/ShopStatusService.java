package com.sky.service;

import com.sky.constant.StatusConstant;
import com.sky.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShopStatusService {

    private static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    private final RedisTemplate<String, Object> redisTemplate;

    public void setStatus(Integer status) {
        if (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status)) {
            throw new BaseException("店铺营业状态只能为 0 或 1");
        }
        redisTemplate.opsForValue().set(SHOP_STATUS_KEY, status);
    }

    public Integer getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS_KEY);
        return status == null ? StatusConstant.DISABLE : status;
    }
}
