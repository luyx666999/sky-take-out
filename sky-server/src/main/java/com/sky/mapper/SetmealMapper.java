package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);

    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO query);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(@Param("id") Long id);

    List<Setmeal> listByIdsForUpdate(@Param("ids") List<Long> ids);

    void deleteByIds(@Param("ids") List<Long> ids);

    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

}
