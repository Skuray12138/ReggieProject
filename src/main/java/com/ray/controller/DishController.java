package com.ray.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.common.R;
import com.ray.dto.DishDto;
import com.ray.entity.Category;
import com.ray.entity.Dish;
import com.ray.entity.DishFlavor;
import com.ray.service.CategoryService;
import com.ray.service.DishFlavorService;
import com.ray.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        //清理所有菜品缓存数据
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        // 精确清理某个分类下面的菜品缓存数据
//        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
//        redisTemplate.delete(key);
        return R.success("添加成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        // 创建page，条件表达式，查询到对应的Page<Dish>
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null,Dish::getName,name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo,queryWrapper);
        // 创建Page<DishDto>，为它Page中添加Page<Dish>查询到的值，除了records
        // records为查询到的Dish列表，里面没有categoryName，通过创建DishDto对象
        // 将Page<Dish>的records的属性赋值给DishDto对象，并通过categoryId查询到Name
        // 再对DishDto设置categoryName，然后将这些DishDto收集成List，作为records对象传给Page<DishDto>

        Page<DishDto> dishDtoPage = new Page<>();
        // 对象拷贝，参数列表为：原对象，目标对象，忽略属性
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        // 取出List<Dish>
        List<Dish> records = pageInfo.getRecords();
        // 创建List<DishDto>：赋值，查询Name，集合成列表
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            // 拷贝其他属性，除categoryName
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据ID查询菜品信息和口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable("id") Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);

        //清理所有菜品缓存数据
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        // 精确清理某个分类下面的菜品缓存数据
//        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
//        redisTemplate.delete(key);
        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应菜品数据，展示在添加套餐中
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){

        List<DishDto> dishDtoList = null;
        // 动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();//dish_13978443691_1
        //从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList != null){
            //如果存在直接返回，无需查询数据库
            return R.success(dishDtoList);
        }
        //如果不存在，需要查询数据库，将查询到的菜品数据缓存早Redis

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            // 拷贝其他属性，除categoryName
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(queryWrapper1);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());
        // 将查询到的菜品数据缓存早Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
//        queryWrapper.eq(Dish::getStatus,1);
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }


    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        dishService.removeWithFlavor(ids);
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return R.success("菜品删除成功");

    }

    @Transactional
    @PostMapping("/status/{status}")
    public R<String> updateWithStatus(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        dishService.updateStatus(status,ids);
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return R.success("菜品状态修改成功");
    }


}
