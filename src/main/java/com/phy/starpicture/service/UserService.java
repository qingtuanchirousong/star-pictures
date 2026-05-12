package com.phy.starpicture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.phy.starpicture.model.dto.user.UserQueryRequest;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.model.vo.UserVO;

public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    UserVO userLogin(String userAccount, String userPassword);

    long userCreate(String userAccount, String userPassword, String userName, String userRole);

    boolean userDelete(long id);

    boolean userUpdate(User user);

    Page<UserVO> userListByPage(UserQueryRequest queryRequest);

    User getUserById(long id);

    UserVO getUserVOById(long id);
}
