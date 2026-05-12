package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.mapper.PictureMapper;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.service.PictureService;
import org.springframework.stereotype.Service;

@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
}
