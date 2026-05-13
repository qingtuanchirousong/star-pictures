package com.phy.starpicture.controller.admin;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.manager.CosManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 管理员端 — 文件管理控制器
 * 提供管理员专用的文件上传接口。
 */
@Api(tags = "管理员-文件管理")
@RestController
@RequestMapping("/admin/file")
public class AdminFileController {

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件到 COS（管理员操作）
     */
    @ApiOperation("上传文件")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = cosManager.uploadFile(file, "picture/");
        return ResultUtils.success(url);
    }
}
