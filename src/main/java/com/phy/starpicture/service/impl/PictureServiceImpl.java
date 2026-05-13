package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.constant.PictureConstant;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.manager.CosManager;
import com.phy.starpicture.manager.FileManager;
import com.phy.starpicture.mapper.PictureMapper;
import com.phy.starpicture.model.dto.picture.PictureEditRequest;
import com.phy.starpicture.model.dto.picture.PictureQueryRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import com.phy.starpicture.service.UserService;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 * 负责图片上传、审核、查询、编辑、删除等业务逻辑。
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片，支持首次上传和重新上传。
     * 管理员上传自动过审，普通用户上传进入待审核状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PictureVO uploadPicture(MultipartFile file, Long pictureId, UserVO loginUser) {
        // 1. 调用 FileManager 完成校验 + 上传 COS + CI 解析，返回填充了元数据的 Picture（未持久化）
        Picture picture = fileManager.uploadPicture(file, loginUser.getId());

        // 2. 根据用户角色设置审核状态
        setReviewStatusOnUpload(picture, loginUser);

        // 3. 判断是首次上传还是重新上传
        if (pictureId != null) {
            // 重新上传：更新已有记录，只替换图片文件，保留名称、分类、标签等基础信息
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
            // 重新上传后重置为待审核（管理员除外）
            applyReviewStatus(dbPicture, loginUser);
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
     * 分页查询图片列表。
     * 管理员可查看所有状态的图片，普通用户只能看到审核通过的图片。
     */
    @Override
    public Page<PictureVO> listPictureByPage(PictureQueryRequest queryRequest, UserVO loginUser) {
        // 构建查询条件
        QueryWrapper<Picture> qw = new QueryWrapper<>();
        // 逻辑删除：只查未删除的
        qw.eq("isDelete", 0);

        // 名称模糊搜索
        if (StrUtil.isNotBlank(queryRequest.getName())) {
            qw.like("name", queryRequest.getName());
        }
        // 分类精确匹配
        if (StrUtil.isNotBlank(queryRequest.getCategory())) {
            qw.eq("category", queryRequest.getCategory());
        }
        // 审核状态过滤
        if (queryRequest.getReviewStatus() != null) {
            qw.eq("reviewStatus", queryRequest.getReviewStatus());
        } else {
            // 普通用户只能看到审核通过的图片
            boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
            if (!isAdmin) {
                qw.eq("reviewStatus", PictureConstant.REVIEW_STATUS_APPROVED);
            }
        }

        // 排序：按编辑时间倒序
        qw.orderByDesc("editTime");

        // 分页查询
        Page<Picture> page = this.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()), qw);

        // 收集所有上传者 userId，批量查询用户信息
        Set<Long> userIds = page.getRecords().stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userMap = userIds.stream()
                .map(id -> userService.getUserVOById(id))
                .collect(Collectors.toMap(UserVO::getId, vo -> vo));

        // 转换为 PictureVO 并填充用户信息
        Page<PictureVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(picture -> {
            PictureVO vo = PictureVO.of(picture);
            vo.setUser(userMap.get(picture.getUserId()));
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 管理员审核图片，将状态改为通过或拒绝，记录审核信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPicture(PictureReviewRequest reviewRequest, UserVO loginUser) {
        Long pictureId = reviewRequest.getId();
        Integer targetStatus = reviewRequest.getReviewStatus();
        // 参数校验：只允许设为通过或拒绝
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "图片 id 不能为空");
        ThrowUtils.throwIf(targetStatus != PictureConstant.REVIEW_STATUS_APPROVED
                && targetStatus != PictureConstant.REVIEW_STATUS_REJECTED,
                ErrorCode.PARAMS_ERROR, "审核状态只能为 1（通过）或 2（拒绝）");

        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 设置审核信息
        picture.setReviewStatus(targetStatus);
        picture.setReviewMessage(reviewRequest.getReviewMessage());
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());

        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "审核操作失败");
    }

    /**
     * 编辑图片元数据，仅所有者或管理员可操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPicture(PictureEditRequest editRequest, UserVO loginUser) {
        Long pictureId = editRequest.getId();
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "图片 id 不能为空");

        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验：仅所有者或管理员可编辑
        boolean isOwner = picture.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "只能编辑自己的图片");

        // 更新可编辑字段（只更新非空字段）
        if (StrUtil.isNotBlank(editRequest.getName())) {
            picture.setName(editRequest.getName());
        }
        if (StrUtil.isNotBlank(editRequest.getIntroduction())) {
            picture.setIntroduction(editRequest.getIntroduction());
        }
        if (StrUtil.isNotBlank(editRequest.getCategory())) {
            picture.setCategory(editRequest.getCategory());
        }
        if (editRequest.getTags() != null) {
            picture.setTags(editRequest.getTags());
        }

        boolean updated = this.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "编辑图片失败");
    }

    /**
     * 删除图片（逻辑删除），仅所有者或管理员可操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePicture(Long id, UserVO loginUser) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片 id 不能为空");

        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验：仅所有者或管理员可删除
        boolean isOwner = picture.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "只能删除自己的图片");

        // MyBatis-Plus 逻辑删除：将 isDelete 置为 1
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.SYSTEM_ERROR, "删除图片失败");
    }

    // ──────────────────────── 私有辅助方法 ────────────────────────

    /**
     * 从完整的 CDN URL 中提取 COS 对象 key
     * 例如 https://xxx.cos.ap-nanjing.myqcloud.com/picture/1/abc.jpg → picture/1/abc.jpg
     */
    private String extractKey(String url) {
        if (url == null) return null;
        int idx = url.indexOf(".com/");
        return idx >= 0 ? url.substring(idx + 5) : null;
    }

    /**
     * 上传时根据用户角色设定审核状态
     * 管理员上传自动过审，普通用户设为待审核
     */
    private void setReviewStatusOnUpload(Picture picture, UserVO loginUser) {
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            picture.setReviewStatus(PictureConstant.REVIEW_STATUS_APPROVED);
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员上传，自动过审");
        } else {
            picture.setReviewStatus(PictureConstant.REVIEW_STATUS_PENDING);
        }
    }

    /**
     * 重新上传时更新审核状态
     * 管理员重传继续保持通过，普通用户重传重置为待审核
     */
    private void applyReviewStatus(Picture picture, UserVO loginUser) {
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            picture.setReviewStatus(PictureConstant.REVIEW_STATUS_APPROVED);
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员重新上传，自动过审");
        } else {
            picture.setReviewStatus(PictureConstant.REVIEW_STATUS_PENDING);
            picture.setReviewerId(null);
            picture.setReviewTime(null);
            picture.setReviewMessage(null);
        }
    }
}
