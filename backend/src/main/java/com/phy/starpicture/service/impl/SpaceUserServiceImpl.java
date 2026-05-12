package com.phy.starpicture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.mapper.SpaceUserMapper;
import com.phy.starpicture.model.entity.SpaceUser;
import com.phy.starpicture.service.SpaceUserService;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {
}
