package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.mapper.SpaceMapper;
import com.phy.starpicture.model.entity.Space;
import com.phy.starpicture.service.SpaceService;
import org.springframework.stereotype.Service;

@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {
}
