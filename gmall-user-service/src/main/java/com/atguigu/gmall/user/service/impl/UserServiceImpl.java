package com.atguigu.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {
        //return userMapper.selectAllUser();
        return userMapper.selectAll();
    }

    @Override
    public List<UmsMemberReceiveAddress> getUmsMemberReceiveAddress(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses
                = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddresses;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis =null;
        try {
            jedis=redisUtil.getJedis();
            if (jedis!=null) {
                //正规项目中password需要MD5加密
                String userMemberStr = jedis.get("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info");
                if(StringUtils.isNotBlank(userMemberStr)){
                    //密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(userMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }
            }
            //连接redis失败或者缓存验证失败，开数据库
            UmsMember umsMemberFromDb=loginFromDb(umsMember);
            if(umsMemberFromDb!=null){
                jedis.setex("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info",
                        60*60*24, JSON.toJSONString(umsMemberFromDb));
                return umsMemberFromDb;
            }
        } finally {
            jedis.close();
        }
        return null;
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token",60*60,token);
        jedis.close();
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsCheck) {
        return userMapper.selectOne(umsCheck);
    }

    @Override
    public UmsMemberReceiveAddress getUmsMemberReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress address = umsMemberReceiveAddressMapper.selectByPrimaryKey(receiveAddressId);
        return address;
    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        List<UmsMember> select = userMapper.select(umsMember);
        if(select!=null&&select.size()>0){
            return select.get(0);
        }
        return null;
    }
}
