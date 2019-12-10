package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.util.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        //插入skuInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId=pmsSkuInfo.getId();
        //插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }
        //插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
        //插入图片关联
        List<PmsSkuImage> pmsSkuImages = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : pmsSkuImages) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }


    public PmsSkuInfo getSkuByIdFromDb(String skuId) {
        PmsSkuInfo pmsSkuInfo=new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        PmsSkuImage pmsSkuImage=new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo1.setSkuImageList(pmsSkuImages);
        return pmsSkuInfo1;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo pmsSkuInfo=null;
        Jedis jedis = redisUtil.getJedis();
        String skukey="sku:"+skuId+":info";
        String skuJson = jedis.get(skukey);
        if(StringUtils.isNotEmpty(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else{
            //缓存中没有，查询数据库
            //为防止缓存击穿，设置分布式锁
            String lockKey="sku:"+skuId+":lock";
            //添加锁的标识
            String token = UUID.randomUUID().toString();
            String OK = jedis.set(lockKey, token, "NX", "PX", 10000);
            if(StringUtils.isNotEmpty(OK)&& "OK".equals(OK)){
                //设置成功有权访问数据库
                pmsSkuInfo = getSkuByIdFromDb(skuId);
                //存入缓存
                if(pmsSkuInfo!=null){
                    jedis.set(skukey,JSON.toJSONString(pmsSkuInfo));
                }else{
                    //数据库中不存在该数据，为防止缓存穿透，将NULL值或空字符串设置给redis
                    //设置一个较短过期时间，短期内不可再访问数据库
                    jedis.setex(skukey,3*60,JSON.toJSONString(""));
                }
                //为了防止线程阻塞，还未返回，lock已经过期释放，并且被别人获取到，此时不能删除别人的锁
                //String lockToken = jedis.get(lockKey);
                //if(StringUtils.isNotEmpty(lockToken)&&token.equals(lockToken)){
                //    jedis.del(lockKey);  //解锁,删除自己的锁
                //}

                //甚至取回lockToken后进行判断的瞬间，lockKey失效，然后被别人拿到锁，set lockKey成功，此时还是不能删除别人的锁
                //于是可以使用lua脚本，去查lockKey的value时，如果是自己的token，立马在redis端就删除掉
                //redis lua脚本格式 eval script numkeys key [key ...] arg [arg ...]
                String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                jedis.eval(script, Collections.singletonList(lockKey),Collections.singletonList(token));

            }else{
                //设置失败，自旋（该线程在睡眠几秒后重新尝试访问本方法）
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //自旋，一定要加 return  否则会变成孤儿线程
                return getSkuById(skuId);
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos=pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId=pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue=new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }
}
