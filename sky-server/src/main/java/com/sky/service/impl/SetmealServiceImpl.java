package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SetmealServiceImpl implements SetmealService {

    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final DishMapper dishMapper;

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        if (setmeal.getStatus() == null) {
            setmeal.setStatus(StatusConstant.DISABLE);
        }
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        validateDishes(dishes, setmeal.getStatus());
        setmealMapper.insert(setmeal);
        saveDishes(setmeal.getId(), dishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO query) {
        PageHelper.startPage(query.getPage(), query.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(query);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        for (Setmeal setmeal : setmealMapper.listByIdsForUpdate(ids)) {
            if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        setmealDishMapper.deleteBySetmealIds(ids);
        setmealMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal == null) {
            throw new BaseException("套餐不存在");
        }
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishMapper.getBySetmealId(id));
        return setmealVO;
    }

    @Override
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Long id = setmealDTO.getId();
        Setmeal current = getForUpdate(id);
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        Integer status = setmeal.getStatus() == null ? current.getStatus() : setmeal.getStatus();
        List<SetmealDish> dishes = setmealDTO.getSetmealDishes();
        validateDishes(dishes, status);
        setmealMapper.update(setmeal);
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(id));
        saveDishes(id, dishes);
    }

    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        if (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status)) {
            throw new BaseException("套餐状态无效");
        }
        getForUpdate(id);
        if (StatusConstant.ENABLE.equals(status)) {
            validateDishes(setmealDishMapper.getBySetmealId(id), status);
        }
        setmealMapper.update(Setmeal.builder().id(id).status(status).build());
    }

    @Override
    public List<Setmeal> list(Setmeal filter) {
        return setmealMapper.list(filter);
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    private Setmeal getForUpdate(Long id) {
        List<Setmeal> setmeals = id == null ? Collections.emptyList()
                : setmealMapper.listByIdsForUpdate(Collections.singletonList(id));
        if (setmeals.isEmpty()) {
            throw new BaseException("套餐不存在");
        }
        return setmeals.get(0);
    }

    private void validateDishes(List<SetmealDish> setmealDishes, Integer status) {
        if (setmealDishes == null || setmealDishes.isEmpty()) {
            return;
        }
        List<Long> dishIds = setmealDishes.stream()
                .map(SetmealDish::getDishId)
                .distinct()
                .collect(Collectors.toList());
        List<Dish> dishes = dishMapper.listByIdsForUpdate(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new BaseException("套餐中有不存在的菜品");
        }
        if (StatusConstant.ENABLE.equals(status)
                && dishes.stream().anyMatch(dish -> !StatusConstant.ENABLE.equals(dish.getStatus()))) {
            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
        }
    }

    private void saveDishes(Long setmealId, List<SetmealDish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return;
        }
        for (SetmealDish dish : dishes) {
            dish.setSetmealId(setmealId);
        }
        setmealDishMapper.insertBatch(dishes);
    }
}
