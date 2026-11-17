package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    void insertBatch(@Param("dishes") List<SetmealDish> dishes);

    @Select("select * from setmeal_dish where setmeal_id = #{setmealId} order by id")
    List<SetmealDish> getBySetmealId(@Param("setmealId") Long setmealId);

    int countByDishIds(@Param("dishIds") List<Long> dishIds);

    void deleteBySetmealIds(@Param("setmealIds") List<Long> setmealIds);
}
