package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    ActiveMQUtil activeMQUtil;


    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeKey="user:"+memberId+":tradeCode";
        String tradeCode= UUID.randomUUID().toString();
        jedis.setex(tradeKey,60*15,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public String checkTradeCode(String memberId,String tradeCode) {
        Jedis jedis =null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey="user:"+memberId+":tradeCode";
//            String tradeCodeFromCache = jedis.get(tradeKey);
//            if (StringUtils.isNotBlank(tradeCodeFromCache)&&tradeCodeFromCache.equals(tradeCode)){
//                jedis.del(tradeKey);
//                return "success";
//            }else{
//                return "fail";
//            }

            //使用lua脚本在发现key的同时将其删除，防止并发订单攻击
            String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            Long result = (Long)jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));
            if(result.longValue()==0){
                return "fail";
            }else{
                return "success";
            }
        } finally {
            jedis.close();
        }
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        //保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String id = omsOrder.getId();
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(id);
            //保存订单详情
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除购物车,测试期间，暂时注释掉
            //cartService.delCartByMemberIdAndSkuId(omsOrder.getMemberId(),omsOrderItem.getProductSkuId());
            //刷新购物车缓存
            cartService.flushCartCache(omsOrder.getMemberId());
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder=new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        return omsOrderMapper.select(omsOrder).get(0);
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        omsOrder.setStatus(1);
        Example example=new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        //发送一个订单已支付的队列，提供给库存消费。
        Connection connection=null;
        Session session=null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try {
            omsOrderMapper.updateByExampleSelective(omsOrder,example);
            Queue payment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            //TextMessage textMessage=new ActiveMQTextMessage();  //字符串文本
            MapMessage mapMessage =new ActiveMQMapMessage();    //hash结构

            producer.send(mapMessage);
            session.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
