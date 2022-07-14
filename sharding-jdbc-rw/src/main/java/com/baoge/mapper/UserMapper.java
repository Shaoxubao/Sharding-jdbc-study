package com.baoge.mapper;
import com.baoge.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("insert into ksd_user(nickname,password,sex,birthday,age) values(#{nickname},#{password},#{sex},#{birthday},#{age})")
    void addUser(User user);

    @Select("select * from ksd_user")
    List<User> findUsers();
}