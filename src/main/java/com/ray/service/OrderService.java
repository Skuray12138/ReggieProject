package com.ray.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.entity.Orders;

public interface OrderService extends IService<Orders> {

    public void submit(Orders orders);
}


