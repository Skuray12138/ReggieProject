package com.ray.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
