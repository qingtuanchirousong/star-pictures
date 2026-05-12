package com.phy.starpicture.controller;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.manager.CosManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    @ApiOperation("【管理员】上传文件")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = cosManager.uploadFile(file, "picture/");
        return ResultUtils.success(url);
    }

    @ApiOperation("下载文件")
    @GetMapping("/download")
    public void downloadFile(@RequestParam("fileKey") String fileKey, HttpServletResponse response) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodeFileName(fileKey));
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            cosManager.downloadToStream(fileKey, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("下载失败", e);
        }
    }

    private String encodeFileName(String key) {
        String name = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (Exception e) {
            return name;
        }
    }
}
