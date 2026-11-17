package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户相关接口")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO login) {
        if (!StringUtils.hasText(login.getCode())) {
            throw new LoginFailedException("缺少微信登录凭证");
        }
        String secret = jwtProperties.getUserSecretKey();
        if (!StringUtils.hasText(secret)) {
            throw new LoginFailedException("服务端未配置用户端 JWT 签名密钥");
        }

        User user = userService.wxLogin(login);
        String token = JwtUtil.createJWT(secret, jwtProperties.getUserTtl(),
                Collections.singletonMap(JwtClaimsConstant.USER_ID, user.getId()));
        return Result.success(UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build());
    }
}
