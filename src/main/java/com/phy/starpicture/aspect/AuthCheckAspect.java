package com.phy.starpicture.aspect;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.BusinessException;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.model.vo.UserVO;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;

@Aspect
@Component
public class AuthCheckAspect {

    @Before("@annotation(authCheck)")
    public void checkAuth(AuthCheck authCheck) {
        // TODO: 开发环境临时跳过验证，生产环境请删除以下 return 语句


        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attributes.getRequest().getSession();
        UserVO loginUser = (UserVO) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }

        String requiredRole = authCheck.requiredRole();
        if (requiredRole != null && !requiredRole.isEmpty()) {
            String userRole = loginUser.getUserRole();
            if (!requiredRole.equals(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限，需要 " + requiredRole + " 角色");
            }
        }
    }
}
