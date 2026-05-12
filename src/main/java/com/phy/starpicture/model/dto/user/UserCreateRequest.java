package com.phy.starpicture.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCreateRequest implements Serializable {

    private String userAccount;

    private String userPassword;

    private String userName;

    private String userRole;
}
