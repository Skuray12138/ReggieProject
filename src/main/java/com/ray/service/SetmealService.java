package com.ray.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.dto.SetmealDto;
import com.ray.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    public void saveWithDish(SetmealDto setmealDto);

    public void removeWithDish(List<Long> ids);

    public SetmealDto getByIdWithDishes(Long id);

    public void updateWithDishes(SetmealDto setmealDto);

    public void updateStatus(Integer status,List<Long> ids);
}
