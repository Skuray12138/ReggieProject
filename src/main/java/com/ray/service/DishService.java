package com.ray.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.dto.DishDto;
import com.ray.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {
    public void saveWithFlavor(DishDto dishDto);

    public DishDto getByIdWithFlavor(Long id);

    public void updateWithFlavor(DishDto dishDto);

    public void removeWithFlavor(List<Long> ids);

    public void updateStatus(Integer status,List<Long> ids);


}
