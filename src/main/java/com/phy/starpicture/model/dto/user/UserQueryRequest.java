package com.phy.starpicture.model.dto.user;

import com.phy.starpicture.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest implements Serializable {

    private String userAccount;

    private String userName;

    private String userRole;
}
