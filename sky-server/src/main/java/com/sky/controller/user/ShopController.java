package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopStatusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequiredArgsConstructor
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    private final ShopStatusService shopStatusService;

    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus() {
        return Result.success(shopStatusService.getStatus());
    }
}
