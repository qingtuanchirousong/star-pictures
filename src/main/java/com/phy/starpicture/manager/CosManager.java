package com.phy.starpicture.manager;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.phy.starpicture.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.StorageClass;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class CosManager {

    @Resource
    private CosConfig cosConfig;

    @Resource
    private COSClient cosClient;

    public String uploadFile(MultipartFile multipartFile, String pathPrefix) {
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = (StrUtil.isBlank(pathPrefix) ? "file/" : pathPrefix) + IdUtil.simpleUUID() + suffix;

        File tempFile = null;
        try {
            tempFile = multipartFileToFile(multipartFile);
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosConfig.getBucket(), key, tempFile);
            putObjectRequest.setStorageClass(StorageClass.Standard);
            cosClient.putObject(putObjectRequest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return cosConfig.getCdnDomain() + "/" + key;
    }

    public void downloadToStream(String key, OutputStream outputStream) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosConfig.getBucket(), key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        try (InputStream inputStream = cosObject.getObjectContent()) {
            IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    public String getCdnUrl(String key) {
        return cosConfig.getCdnDomain() + "/" + key;
    }

    public void deleteFile(String key) {
        cosClient.deleteObject(cosConfig.getBucket(), key);
    }

    private File multipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("cos_upload_", ".tmp");
        try (InputStream inputStream = multipartFile.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            IoUtil.copy(inputStream, outputStream);
        }
        return file;
    }
}
