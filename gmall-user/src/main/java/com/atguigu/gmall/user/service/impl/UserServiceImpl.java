package com.atguigu.gmall.user.service.impl;


import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> getAllUser() {
        //return userMapper.selectAllUser();
        return userMapper.selectAll();
    }

    @Override
    public List<UmsMemberReceiveAddress> getUmsMemberReceiveAddress(String memberId) {
//        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
//        umsMemberReceiveAddress.setMemberId(memberId);
//        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses
//                = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        Example example=new Example(UmsMemberReceiveAddress.class);
        example.createCriteria().andEqualTo("memberId",memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses
                = umsMemberReceiveAddressMapper.selectByExample(example);
        return umsMemberReceiveAddresses;
    }
}
