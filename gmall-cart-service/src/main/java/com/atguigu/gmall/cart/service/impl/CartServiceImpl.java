package com.atguigu.gmall.cart.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public OmsCartItem getCartByMemberIdAndSkuId(String memberId, String skuId) {
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItemFromDb = omsCartItemMapper.selectOne(omsCartItem);
        return omsCartItemFromDb;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())||StringUtils.isNotBlank(omsCartItem.getProductSkuId()))
            omsCartItemMapper.insertSelective(omsCartItem);
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        omsCartItemMapper.updateByPrimaryKeySelective(omsCartItemFromDb);
    }

    @Override
    public List<OmsCartItem> flushCartCache(String memberId) {

        OmsCartItem omsCartItem1 = new OmsCartItem();
        omsCartItem1.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem1);
        Map<String, String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }
        if(!map.isEmpty()){
            //因为用户对购物车的修改比较频繁，不适合用userId为key整个列表json后为value的String数据类型。
            //这里使用redis的hash数据结构。userId为redisKey，skuId为hashKey，这样方便直接找到某一条数据
            Jedis jedis = redisUtil.getJedis();
            jedis.del("user:" + memberId + ":cart");
            jedis.hmset("user:" + memberId + ":cart", map);
            jedis.expire("user:" + memberId + ":cart",60*60);
            jedis.close();
        }
        return omsCartItems;
    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        //查缓存，不存在的话去查数据库，再放入缓存，与item详情页逻辑一致
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            List<String> hvals = jedis.hvals("user:" + memberId + ":cart");
            if(!hvals.isEmpty()){
                for (String hval : hvals) {
                    OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                    omsCartItems.add(omsCartItem);
                }
            }else{
                //查数据库，放缓存
                //击穿问题
                RLock lock = redissonClient.getLock("redis-lock-cartList");//分布锁
                //加锁
                lock.lock();
                try{
                    omsCartItems = flushCartCache(memberId);
                }finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }

        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example example=new Example(OmsCartItem.class);
        example.createCriteria()
                .andEqualTo("productSkuId",omsCartItem.getProductSkuId())
                .andEqualTo("memberId",omsCartItem.getMemberId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);

        //缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }

    @Override
    public void delCartByMemberIdAndSkuId(String memberId, String productSkuId) {
        Example example=new Example(OmsCartItem.class);
        example.createCriteria()
                .andEqualTo("memberId",memberId)
                .andEqualTo("productSkuId",productSkuId);
        omsCartItemMapper.deleteByExample(example);
    }
}
