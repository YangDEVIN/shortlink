package com.shortlink.admin.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shortlink.admin.common.convention.exception.ClientException;
import com.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.shortlink.admin.dao.entity.UserDO;
import com.shortlink.admin.dao.mapper.UserMapper;
import com.shortlink.admin.dto.request.UserRegisterReqDTO;
import com.shortlink.admin.dto.respond.UserRespDTO;
import io.reactivex.rxjava3.internal.operators.completable.CompletableDoFinally;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户Service实现类
 */

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if(userDO == null){
            throw  new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO,result);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {

        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override

    public void register(UserRegisterReqDTO requestParam) {
        if(!hasUsername(requestParam.getUsername())){
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());

        if(!lock.tryLock()){
            throw new ClientException(USER_NAME_EXIST);
        }
        try{
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            String username = requestParam.getUsername();
            userRegisterCachePenetrationBloomFilter.add(username);
            if(inserted < 1){
                throw new ClientException(USER_SAVE_ERROR);
        }
        } finally {
            lock.unlock();
        }
    }
}
