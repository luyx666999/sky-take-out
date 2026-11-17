package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "C端-菜品浏览接口")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;

    @GetMapping("/list")
    @ApiOperation("根据分类查询起售菜品和口味")
    public Result<List<DishVO>> list(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        Dish filter = Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        return Result.success(dishService.listWithFlavor(filter));
    }
}
