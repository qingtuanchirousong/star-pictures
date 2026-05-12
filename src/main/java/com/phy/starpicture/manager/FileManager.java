package com.phy.starpicture.manager;

import cn.hutool.core.util.StrUtil;
import com.phy.starpicture.exception.BusinessException;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.model.entity.Picture;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 图片文件管理服务（业务层）
 * 在 CosManager 基础上封装图片上传的业务逻辑，包括文件校验、路径拼装和 CI 解析结果转换。
 * 底层调用 {@link CosManager} 完成实际的 COS 上传和数据万象解析。
 */
@Component
public class FileManager {

    /** 上传文件大小上限：10MB */
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    /** 允许上传的图片 MIME 类型 */
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "image/bmp"};

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片并解析其元数据（通过数据万象 CI）
     *
     * @param file   前端上传的图片文件
     * @param userId 上传用户 id，用于拼装 COS 路径
     * @return 填充了图片元数据的 Picture 实体（未持久化，需调用方自行保存到数据库）
     */
    public Picture uploadPicture(MultipartFile file, Long userId) {
        // 1. 校验文件合法性（非空、大小、格式）
        validatePicture(file);

        // 2. 上传到 COS，路径为 picture/{用户id}/{uuid}.{后缀}
        //    同时通过数据万象（CI）的 is_pic_info 获取图片元数据
        String pathPrefix = "picture/" + userId + "/";
        CosManager.UploadWithPicResult uploadResult = cosManager.uploadPictureWithInfo(file, pathPrefix);

        // 3. 拼接 CDN 访问 URL
        String url = cosManager.getCdnUrl(uploadResult.key);

        // 4. 提取 CI 返回的图片元数据（宽、高、格式、主色调等）
        CosManager.ImageParseResult parseResult = cosManager.parsePicInfo(uploadResult.ciUploadResult);

        // 5. 组装 Picture 实体返回给调用方
        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setName(file.getOriginalFilename());
        picture.setPicSize(file.getSize());
        if (parseResult != null) {
            picture.setPicWidth(parseResult.width);
            picture.setPicHeight(parseResult.height);
            picture.setPicScale(parseResult.scale);
            picture.setPicFormat(parseResult.format);
            picture.setPicColor(parseResult.dominantColor);
        }
        picture.setUserId(userId);
        return picture;
    }

    /**
     * 校验图片文件是否合法
     * 检查：非空、大小不超过 10MB、MIME 类型在白名单内
     */
    private void validatePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (!StrUtil.containsAnyIgnoreCase(contentType, ALLOWED_TYPES)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG / PNG / WebP / BMP 格式");
        }
    }
}
