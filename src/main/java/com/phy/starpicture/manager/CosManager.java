package com.phy.starpicture.manager;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.phy.starpicture.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 腾讯云 COS 对象存储操作管理类
 * 封装文件上传（含数据万象图片解析）、下载、删除等基础操作。
 * 所有上传/下载均以临时文件为中介，操作完成后自动清理临时文件。
 */
@Component
public class CosManager {

    @Resource
    private CosConfig cosConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 普通文件上传，返回 CDN 访问 URL（不解析图片信息）
     *
     * @param multipartFile 前端上传的文件
     * @param pathPrefix    COS 存储路径前缀（如 "file/", "picture/1/"）
     * @return 完整的 CDN 访问 URL
     */
    public String uploadFile(MultipartFile multipartFile, String pathPrefix) {
        String key = buildFileKey(multipartFile, pathPrefix);

        File tempFile = null;
        try {
            tempFile = multipartFileToFile(multipartFile);
            PutObjectRequest request = new PutObjectRequest(cosConfig.getBucket(), key, tempFile);
            request.setStorageClass(StorageClass.Standard);
            cosClient.putObject(request);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        } finally {
            deleteTempFile(tempFile);
        }

        return cosConfig.getCdnDomain() + "/" + key;
    }

    /**
     * 上传图片并通过数据万象（CI）同步解析图片信息
     * 利用腾讯云数据万象的 {@code PicOperations.setIsPicInfo(1)} 功能，
     * 在上传时自动向 COS 请求图片元数据（格式、宽高、主色调等）。
     *
     * @param multipartFile 前端上传的图片文件
     * @param pathPrefix    COS 存储路径前缀
     * @return UploadWithPicResult 包含 COS 存储 key 和 CI 返回结果，调用方从中获取 URL 和图片元数据
     */
    public UploadWithPicResult uploadPictureWithInfo(MultipartFile multipartFile, String pathPrefix) {
        String key = buildFileKey(multipartFile, pathPrefix);

        File tempFile = null;
        try {
            tempFile = multipartFileToFile(multipartFile);

            PutObjectRequest request = new PutObjectRequest(cosConfig.getBucket(), key, tempFile);
            request.setStorageClass(StorageClass.Standard);

            // 配置数据万象图片处理规则：开启图片信息获取
            PicOperations picOps = new PicOperations();
            picOps.setIsPicInfo(1);  // 获取图片格式、宽高、主色调、MD5 等
            request.setPicOperations(picOps);

            // 上传 + CI 同步处理
            PutObjectResult result = cosClient.putObject(request);
            return new UploadWithPicResult(key, result.getCiUploadResult());
        } catch (IOException e) {
            throw new RuntimeException("图片上传失败", e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 从 CIUploadResult 中提取原始图片的元数据
     * 通常在 {@link #uploadPictureWithInfo} 返回后调用。
     *
     * @param ciUploadResult 数据万象上传结果（可为 null）
     * @return 图片元数据，如果 CI 未返回则为 null
     */
    public ImageParseResult parsePicInfo(CIUploadResult ciUploadResult) {
        if (ciUploadResult == null) return null;
        OriginalInfo originalInfo = ciUploadResult.getOriginalInfo();
        if (originalInfo == null) return null;
        ImageInfo ciImage = originalInfo.getImageInfo();
        if (ciImage == null) return null;

        ImageParseResult result = new ImageParseResult();
        result.width = ciImage.getWidth();
        result.height = ciImage.getHeight();
        result.format = StrUtil.isNotBlank(ciImage.getFormat()) ? ciImage.getFormat() : "unknown";
        // CI 主色调格式为 0xRRGGBB 或纯 RRGGBB，统一转为 #RRGGBB
        String ave = ciImage.getAve();
        if (StrUtil.isNotBlank(ave)) {
            ave = ave.replace("0x", "").replace("0X", "");
            result.dominantColor = "#" + ave;
        } else {
            result.dominantColor = "#000000";
        }
        result.scale = result.height > 0 ? (double) result.width / result.height : 1.0;
        return result;
    }

    /**
     * 流式下载 COS 文件到输出流
     *
     * @param key          COS 对象 key
     * @param outputStream 目标输出流
     */
    public void downloadToStream(String key, OutputStream outputStream) {
        GetObjectRequest request = new GetObjectRequest(cosConfig.getBucket(), key);
        COSObject cosObject = cosClient.getObject(request);
        try (InputStream inputStream = cosObject.getObjectContent()) {
            IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 获取 COS 对象的完整 CDN 访问 URL
     */
    public String getCdnUrl(String key) {
        return cosConfig.getCdnDomain() + "/" + key;
    }

    /**
     * 删除 COS 对象
     */
    public void deleteFile(String key) {
        cosClient.deleteObject(cosConfig.getBucket(), key);
    }

    /**
     * 拼装存储 key：路径前缀 + UUID + 原文件后缀
     */
    private String buildFileKey(MultipartFile multipartFile, String pathPrefix) {
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return (StrUtil.isBlank(pathPrefix) ? "file/" : pathPrefix) + IdUtil.simpleUUID() + suffix;
    }

    /**
     * MultipartFile 转为临时文件（用完需手动删除）
     */
    private File multipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("cos_upload_", ".tmp");
        try (InputStream inputStream = multipartFile.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            IoUtil.copy(inputStream, outputStream);
        }
        return file;
    }

    /**
     * 安全删除临时文件
     */
    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /**
     * 上传图片的返回结果，包含 COS 存储 key 和 CI 解析结果
     */
    public static class UploadWithPicResult {
        /** COS 对象 key，用于拼接 CDN URL */
        public final String key;
        /** 数据万象返回的原始结果（可为 null） */
        public final CIUploadResult ciUploadResult;

        public UploadWithPicResult(String key, CIUploadResult ciUploadResult) {
            this.key = key;
            this.ciUploadResult = ciUploadResult;
        }
    }

    /**
     * 图片解析结果，封装 CI 返回的图片元数据
     */
    public static class ImageParseResult {
        /** 图片宽度（像素） */
        public int width;
        /** 图片高度（像素） */
        public int height;
        /** 宽高比 */
        public double scale;
        /** 图片格式（如 jpeg、png、webp） */
        public String format;
        /** 主色调（十六进制，如 #3A7CBF） */
        public String dominantColor;
    }
}
