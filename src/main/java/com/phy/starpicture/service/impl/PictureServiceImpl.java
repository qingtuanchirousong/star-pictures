package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.manager.CosManager;
import com.phy.starpicture.manager.FileManager;
import com.phy.starpicture.mapper.PictureMapper;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 图片服务实现类
 * 负责图片上传的业务逻辑编排：调用 FileManager 上传并解析，持久化到数据库，返回 VO。
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * 支持首次上传（pictureId 为 null）和重新上传（pictureId 不为 null，只更新图片文件，基础信息不变）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PictureVO uploadPicture(MultipartFile file, Long pictureId, UserVO loginUser) {
        // 1. 调用 FileManager 完成校验 + 上传 COS + CI 解析，返回填充了元数据的 Picture（未持久化）
        Picture picture = fileManager.uploadPicture(file, loginUser.getId());

        // 2. 判断是首次上传还是重新上传
        if (pictureId != null) {
            // 重新上传：更新已有记录，只替换图片文件（URL、尺寸等），保留名称、分类、标签等基础信息
            Picture dbPicture = this.getById(pictureId);
            ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            ThrowUtils.throwIf(!dbPicture.getUserId().equals(loginUser.getId()),
                    ErrorCode.NO_AUTH_ERROR, "只能更新自己上传的图片");

            // 删除旧的 COS 文件
            String oldKey = extractKey(dbPicture.getUrl());
            if (oldKey != null) {
                cosManager.deleteFile(oldKey);
            }

            // 更新图片文件信息，保留基础字段不变
            dbPicture.setUrl(picture.getUrl());
            dbPicture.setPicSize(picture.getPicSize());
            dbPicture.setPicWidth(picture.getPicWidth());
            dbPicture.setPicHeight(picture.getPicHeight());
            dbPicture.setPicScale(picture.getPicScale());
            dbPicture.setPicFormat(picture.getPicFormat());
            dbPicture.setPicColor(picture.getPicColor());
            boolean updated = this.updateById(dbPicture);
            ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新图片失败");

            PictureVO vo = PictureVO.of(dbPicture);
            vo.setUser(loginUser);
            return vo;
        } else {
            // 首次上传：新增一条图片记录
            boolean saved = this.save(picture);
            ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "上传图片失败");

            PictureVO vo = PictureVO.of(picture);
            vo.setUser(loginUser);
            return vo;
        }
    }

    /**
     * 从完整的 CDN URL 中提取 COS 对象 key
     * 例如 https://xxx.cos.ap-nanjing.myqcloud.com/picture/1/abc.jpg → picture/1/abc.jpg
     */
    private String extractKey(String url) {
        if (url == null) return null;
        int idx = url.indexOf(".com/");
        return idx >= 0 ? url.substring(idx + 5) : null;
    }
}
