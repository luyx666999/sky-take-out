package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
@RequiredArgsConstructor
public class SetmealController {
    private final SetmealService setmealService;

    @GetMapping("/list")
    @ApiOperation("根据分类查询起售套餐")
    public Result<List<Setmeal>> list(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        Setmeal filter = Setmeal.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        return Result.success(setmealService.list(filter));
    }

    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐查询包含的菜品")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        return Result.success(setmealService.getDishItemById(id));
    }
}
