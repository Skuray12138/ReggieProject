package com.ray.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.CustomException;
import com.ray.dto.DishDto;
import com.ray.entity.Dish;
import com.ray.entity.DishFlavor;
import com.ray.mapper.DishMapper;
import com.ray.service.DishFlavorService;
import com.ray.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对应的口味数据，口味表中还需保存对应的菜品id
     *
     * @param dishDto
     */
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品基本信息到菜品表dish
        this.save(dishDto);
        // 获取菜品id
        Long dishId = dishDto.getId();
        // 获取菜品口味列表
        List<DishFlavor> flavors = dishDto.getFlavors();
        // 循环为菜品口味添加菜品id，并最后再转为列表
        flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
        // 保存菜品口味数据到菜品口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据ID查询菜品信息和对应口味信息
     *
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        // 查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        // 查询当前菜品对应的口味信息，从dish_flavor查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新对应口味信息
     *
     * @param dishDto
     */
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);
        //清理当前菜品对应口味数据--dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        //添加当前提交过来的口味数据--dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        // 上一步将传输过来的dishDto.getFlavors列表放入新的列表flavors中
        // 下一步，因为前端发送来的封装数据只有name和value，没有对应的菜品ID
        // 因此使用循环方式对新的列表flavors中每个flavor属性添加菜品ID
        // 最后将所有flavor重新整合成列表flavors作为参数进行SQL语句
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    public void removeWithFlavor(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        // select count(*) from dish where id in () and status = 1
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        queryWrapper.eq(Dish::getStatus, 1);
        int count = this.count(queryWrapper);
        if (count > 0) {
            //如果不能删除抛出一个业务异常
            throw new CustomException("菜品正在售卖，不能删除");
        }
        //如果可以删除，先删除菜品表数据--dish
        this.removeByIds(ids);
        //删除口味表数据--dish_flavor
        //delete from dish_flavor where dish_id in ()
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(lambdaQueryWrapper);
    }

    /**
     * 修改菜品起售状态
     * @param status
     * @param ids
     */
    public void updateStatus(Integer status, List<Long> ids) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null, Dish::getId, ids);
        List<Dish> list = this.list(queryWrapper);
        for (Dish dish : list) {
            if (dish!= null){
                dish.setStatus(status);
                this.updateById(dish);
            }
        }
    }


}
