package com.baoge.controller;
import com.baoge.entity.User;
import com.baoge.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserMapper userMapper;
    @GetMapping("/save")
    public String insert() {
        User user = new User();
        user.setNickname("zhangsan"+ new Random().nextInt());
        user.setPassword("1234567");
        user.setSex(1);
        user.setBirthday("1988-12-03");
        user.setAge(21);
        userMapper.addUser(user);
        return "success";
    }
    @GetMapping("/listUser")
    public List<User> listUser() {
        return userMapper.findUsers();
    }
}