package com.phy.starpicture.controller.user;

import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.manager.CosManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * 用户端 — 文件控制器
 * 提供文件下载等面向普通用户的接口。
 * 管理员专用的上传接口在 controller/admin/AdminFileController 中。
 */
@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 下载文件
     * 根据 COS 文件 key 从对象存储下载文件。
     */
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

    /**
     * 对文件名进行 URL 编码，解决中文文件名问题
     */
    private String encodeFileName(String key) {
        String name = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (Exception e) {
            return name;
        }
    }
}
