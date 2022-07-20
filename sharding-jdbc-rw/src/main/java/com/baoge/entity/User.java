package com.baoge.entity;
import lombok.Data;

import java.util.Date;

@Data
public class User {
    // 主键
    private Long id;
    // 昵称
    private String nickname;
    // 密码
    private String password;
    // 性
    private Integer sex;
    // 性
    private Date birthday;
    // 年龄
    private int age;
}