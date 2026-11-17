package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopStatusService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminShopController")
@RequiredArgsConstructor
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    private final ShopStatusService shopStatusService;

    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result<Void> setStatus(@PathVariable("status") Integer status) {
        shopStatusService.setStatus(status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus() {
        return Result.success(shopStatusService.getStatus());
    }
}
