package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.constant.PictureConstant;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.manager.BingImageFetcher;
import com.phy.starpicture.manager.CosManager;
import com.phy.starpicture.manager.FileManager;
import com.phy.starpicture.mapper.PictureMapper;
import com.phy.starpicture.model.dto.picture.PictureBatchFetchRequest;
import com.phy.starpicture.model.dto.picture.PictureEditRequest;
import com.phy.starpicture.model.dto.picture.PictureQueryRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import com.phy.starpicture.service.UserService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phy.starpicture.manager.TwoLevelCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 * 负责图片上传、审核、查询、编辑、删除等业务逻辑。
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    private BingImageFetcher bingImageFetcher;

    @Resource
    private TwoLevelCacheManager cacheManager;

    @Resource
    private ObjectMapper objectMapper;

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

            // 清除分页缓存，保证下次查询数据一致
            cacheManager.evictPictureListCache();

            PictureVO vo = PictureVO.of(dbPicture);
            vo.setUser(loginUser);
            return vo;
        } else {
            // 首次上传：新增一条图片记录
            boolean saved = this.save(picture);
            ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "上传图片失败");

            // 清除分页缓存，保证下次查询数据一致
            cacheManager.evictPictureListCache();

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
        // 构建缓存 key：包含用户角色和所有查询参数，确保不同查询条件各自缓存
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        String cacheKey = new StringBuilder("picPage:")
                .append("uid:").append(loginUser.getId())
                .append(":role:").append(isAdmin ? "admin" : "user")
                .append(":name:").append(StrUtil.nullToDefault(queryRequest.getName(), ""))
                .append(":cat:").append(StrUtil.nullToDefault(queryRequest.getCategory(), ""))
                .append(":rev:").append(queryRequest.getReviewStatus())
                .append(":page:").append(queryRequest.getCurrent())
                .append(":size:").append(queryRequest.getPageSize())
                .toString();

        // Page<PictureVO> 的泛型类型，用于 JSON 反序列化
        JavaType javaType = objectMapper.getTypeFactory()
                .constructParametricType(Page.class, PictureVO.class);

        // 使用二级缓存：L1(Caffeine) → L2(Redis) → DB
        return cacheManager.get(cacheKey, javaType, () -> {
            // 构建查询条件
            QueryWrapper<Picture> qw = new QueryWrapper<>();
            qw.eq("isDelete", 0); // 逻辑删除过滤

            if (StrUtil.isNotBlank(queryRequest.getName())) {
                qw.like("name", queryRequest.getName());
            }
            if (StrUtil.isNotBlank(queryRequest.getCategory())) {
                qw.eq("category", queryRequest.getCategory());
            }
            // 审核状态过滤：管理员按指定状态筛选，普通用户强制只看已通过
            if (isAdmin && queryRequest.getReviewStatus() != null) {
                qw.eq("reviewStatus", queryRequest.getReviewStatus());
            } else if (!isAdmin) {
                qw.eq("reviewStatus", PictureConstant.REVIEW_STATUS_APPROVED);
            }

            qw.orderByDesc("editTime");

            // 分页查询
            Page<Picture> page = this.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()), qw);

            // 批量查询上传者信息
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
        });
    }

    /**
     * 管理员审核图片，将状态改为通过或拒绝，记录审核信息。
     */
    /**
     * 根据 id 获取图片详情（带缓存）
     * 优先从 Redis 缓存读取，缓存 30 分钟。写操作（编辑、删除、审核）会自动清除缓存。
     */
    @Override
    @Cacheable(value = "pictureDetail", key = "#id", unless = "#result == null")
    public PictureVO getPictureDetailById(Long id, UserVO loginUser) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片 id 不能为空");

        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null || picture.getIsDelete() == 1,
                ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 普通用户只能查看已审核通过的图片
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isAdmin && picture.getReviewStatus() != PictureConstant.REVIEW_STATUS_APPROVED) {
            throw new com.phy.starpicture.exception.BusinessException(
                    ErrorCode.NOT_FOUND_ERROR, "图片不存在或未审核通过");
        }

        // 填充上传者信息
        PictureVO vo = PictureVO.of(picture);
        UserVO uploader = userService.getUserVOById(picture.getUserId());
        vo.setUser(uploader);
        return vo;
    }

    // 写操作清除对应图片缓存
    @CacheEvict(value = "pictureDetail", key = "#reviewRequest.id")
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

        // 清除分页缓存
        cacheManager.evictPictureListCache();
    }

    /**
     * 编辑图片元数据，仅所有者或管理员可操作。
     */
    @CacheEvict(value = "pictureDetail", key = "#editRequest.id")
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

        // 清除分页缓存
        cacheManager.evictPictureListCache();
    }

    /**
     * 删除图片（逻辑删除），仅所有者或管理员可操作。
     */
    @CacheEvict(value = "pictureDetail", key = "#id")
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

        // 清除分页缓存
        cacheManager.evictPictureListCache();
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
     * 批量从网络抓取图片并入库
     * 管理员填写关键词和数量，系统从 Bing 搜索抓取图片并自动上传到 COS，创建已过审的记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PictureVO> batchFetchPictures(PictureBatchFetchRequest fetchRequest, UserVO loginUser) {
        String keyword = fetchRequest.getKeyword();
        int count = fetchRequest.getCount() != null ? fetchRequest.getCount() : 10;

        ThrowUtils.throwIf(StrUtil.isBlank(keyword), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        ThrowUtils.throwIf(count <= 0 || count > 30, ErrorCode.PARAMS_ERROR, "抓取数量需在 1~30 之间");

        // 1. 从 Bing 抓取图片 URL 列表
        List<BingImageFetcher.BingImage> bingImages = bingImageFetcher.fetchImages(keyword, count);
        ThrowUtils.throwIf(bingImages.isEmpty(), ErrorCode.OPERATION_ERROR, "未抓取到任何图片，请更换关键词重试");

        List<PictureVO> resultList = new ArrayList<>();
        String pathPrefix = "picture/" + loginUser.getId() + "/";

        for (BingImageFetcher.BingImage bi : bingImages) {
            try {
                // 2. 下载图片字节
                byte[] imageBytes = cn.hutool.http.HttpUtil.downloadBytes(bi.imageUrl);

                // 3. 上传到 COS（含 CI 元数据解析）
                CosManager.UploadWithPicResult uploadResult =
                        cosManager.uploadPictureFromBytes(imageBytes, extractFileName(bi.imageUrl), pathPrefix);

                // 4. 解析图片元数据
                CosManager.ImageParseResult parseResult = cosManager.parsePicInfo(uploadResult.ciUploadResult);

                // 5. 创建 Picture 记录（管理员抓取，自动过审）
                Picture picture = new Picture();
                picture.setUrl(cosManager.getCdnUrl(uploadResult.key));
                picture.setName(StrUtil.blankToDefault(bi.name, keyword));
                picture.setIntroduction("批量抓取自 Bing 搜索，关键词：" + keyword);
                picture.setUserId(loginUser.getId());
                // 管理员抓取自动过审
                picture.setReviewStatus(PictureConstant.REVIEW_STATUS_APPROVED);
                picture.setReviewerId(loginUser.getId());
                picture.setReviewTime(new Date());
                picture.setReviewMessage("管理员批量抓取，自动过审");

                // CI 元数据
                if (parseResult != null) {
                    picture.setPicWidth(parseResult.width);
                    picture.setPicHeight(parseResult.height);
                    picture.setPicScale(parseResult.scale);
                    picture.setPicFormat(parseResult.format);
                    picture.setPicColor(parseResult.dominantColor);
                }
                picture.setPicSize((long) imageBytes.length);

                this.save(picture);

                PictureVO vo = PictureVO.of(picture);
                vo.setUser(loginUser);
                resultList.add(vo);
            } catch (Exception e) {
                // 单张失败不影响整体，跳过继续
                log.error("批量抓取单张图片失败: " + bi.imageUrl, e);
            }
        }

        ThrowUtils.throwIf(resultList.isEmpty(), ErrorCode.OPERATION_ERROR, "所有图片抓取均失败，请稍后重试");

        // 批量入库后清除分页缓存
        cacheManager.evictPictureListCache();
        return resultList;
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileName(String url) {
        if (StrUtil.isBlank(url)) return "image.jpg";
        int queryIdx = url.indexOf("?");
        String path = queryIdx > 0 ? url.substring(0, queryIdx) : url;
        int lastSlash = path.lastIndexOf("/");
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : "image.jpg";
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
