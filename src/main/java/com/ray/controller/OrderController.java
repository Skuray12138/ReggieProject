package com.ray.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.common.R;
import com.ray.entity.Orders;
import com.ray.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){

        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 展示订单详情
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(@PathVariable int page,int pageSize){
        Page<Orders> pageInfo = new Page<>(page,pageSize);



        return R.success(pageInfo);
    }

}
