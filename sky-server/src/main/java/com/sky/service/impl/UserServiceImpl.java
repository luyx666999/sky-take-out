package com.sky.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatProperties weChatProperties;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public UserServiceImpl(WeChatProperties weChatProperties, UserMapper userMapper,
                           RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.weChatProperties = weChatProperties;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public User wxLogin(UserLoginDTO login) {
        return findOrRegister(exchangeCode(login.getCode()));
    }

    private String exchangeCode(String code) {
        if (!StringUtils.hasText(weChatProperties.getAppid()) || !StringUtils.hasText(weChatProperties.getSecret())) {
            throw new LoginFailedException("服务端未配置小程序 AppID 或 AppSecret");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(WX_LOGIN)
                .queryParam("appid", "{appid}")
                .queryParam("secret", "{secret}")
                .queryParam("js_code", "{code}")
                .queryParam("grant_type", "authorization_code")
                .encode()
                .buildAndExpand(weChatProperties.getAppid(), weChatProperties.getSecret(), code)
                .toUri();
        byte[] body;
        try {
            // 微信有时会用 text/plain 返回 JSON。
            body = restTemplate.getForObject(uri, byte[].class);
        } catch (RestClientResponseException ex) {
            log.warn("请求微信登录接口失败，HTTP 状态码 {}", ex.getRawStatusCode());
            throw new LoginFailedException("微信登录服务暂不可用");
        } catch (RestClientException ex) {
            log.warn("请求微信登录接口失败，异常类型 {}",
                    NestedExceptionUtils.getMostSpecificCause(ex).getClass().getSimpleName());
            throw new LoginFailedException("微信登录服务暂不可用");
        }

        JsonNode session;
        try {
            session = body == null ? null : objectMapper.readTree(body);
        } catch (IOException ex) {
            throw new LoginFailedException("微信登录接口返回了无法解析的数据");
        }
        if (session == null || !session.isObject()) {
            throw new LoginFailedException("微信登录接口返回了无法解析的数据");
        }

        int errorCode = session.path("errcode").asInt();
        if (errorCode != 0) {
            throw new LoginFailedException("微信登录校验失败（微信错误码 " + errorCode + "）");
        }
        String openid = session.path("openid").textValue();
        if (!StringUtils.hasText(openid)) {
            throw new LoginFailedException("微信登录接口未返回 OpenID");
        }
        return openid;
    }

    // 课程数据库的 openid 没有唯一索引，串行处理本实例中的首次注册。
    private synchronized User findOrRegister(String openid) {
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            userMapper.insert(user);
        }
        return user;
    }
}
