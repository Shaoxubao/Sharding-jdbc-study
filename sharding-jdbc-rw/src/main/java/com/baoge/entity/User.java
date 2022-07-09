package com.baoge.entity;
import lombok.Data;

@Data
public class User {
    // 主键
    private Integer id;
    // 昵称
    private String nickname;
    // 密码
    private String password;
    // 性
    private Integer sex;
    // 性
    private String birthday;
}