package com.ray.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.CustomException;
import com.ray.dto.SetmealDto;
import com.ray.entity.Dish;
import com.ray.entity.Setmeal;
import com.ray.entity.SetmealDish;
import com.ray.mapper.SetmealMapper;
import com.ray.service.SetmealDishService;
import com.ray.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;


    /**
     * 新增套餐，同时保存套餐和菜品关联关系
     * @param setmealDto
     */
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        this.save(setmealDto);
        // 保存套餐基本信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item) -> {
        // 虽然setmealDto.getSetmealDishes()没有封装，但setmealDto继承的setmeal封装了自身ID
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        // 保存套餐和菜品关联信息
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     * @param ids
     */
    @Transactional
    public void removeWithDish(List<Long> ids){
        //查询套餐状态，确定是否可以删除
        // select count(*) from setmeal where id in () and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);
        int count = this.count(queryWrapper);
        if (count >0 ){
            //如果不能删除抛出一个业务异常
            throw new CustomException("套餐正在售卖，不能删除");
        }
        //如果可以删除，先删除套餐表中数据--setmeal
        this.removeByIds(ids);
        //删除关系表中数据--setmeal_dish
        // delete from setmeal_dish where setmeal id in ()
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids );
        setmealDishService.remove(lambdaQueryWrapper);

    }

    @Override
    public SetmealDto getByIdWithDishes(Long id) {
        // 查询套餐基本信息
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);
        // 查询套餐关联菜品信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);
        return setmealDto;

    }

    @Transactional
    public void updateWithDishes(SetmealDto setmealDto) {
        //更新setmeal表信息
        this.updateById(setmealDto);
        //清理当前菜品对应setmealDish表数据
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        //添加当前提交的setmealDish数据，并手动添加setmealId
        List<SetmealDish> list = setmealDto.getSetmealDishes();
        list = list.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(list);

    }

    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null ,Setmeal::getId,ids);
        List<Setmeal> list = this.list(queryWrapper);
        list = list.stream().map((item) -> {
            if (item != null){
                item.setStatus(status);
            }
            return item;
        }).collect(Collectors.toList());
        this.updateBatchById(list);
    }
}
