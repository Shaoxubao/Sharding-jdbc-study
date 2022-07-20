package com.baoge.controller;
import com.baoge.entity.Order;
import com.baoge.entity.User;
import com.baoge.mapper.UserMapper;
import com.baoge.mapper.UserOrderMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserOrderMapper userOrderMapper;

    @GetMapping("/save")
    public String insert() {
        User user = new User();
        user.setNickname("zhangsan"+ new Random().nextInt());
        user.setPassword("1234567");
        user.setSex(1);
        user.setBirthday(new Date());
        user.setAge(22);
        userMapper.addUser(user);
        System.out.println(user.getId());
        return "success";
    }

    @GetMapping("/listUser")
    public List<User> listUser() {
        return userMapper.findUsers();
    }

    @GetMapping("/saveOrder")
    public String insertOrder() {
        Order order = new Order();
        order.setCreateTime(new Date());
        order.setOrderNumber("133455678");
        order.setYearmonth("202203");
        order.setUserId(1L);
        userOrderMapper.addUserOrder(order);
        return "success";
    }
}