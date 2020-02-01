package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;


    @JmsListener(destination ="PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage){

        try {
            String out_trade_no = mapMessage.getString("out_trade_no");
            Integer count = mapMessage.getInt("count");
            //调用paymentService 支付宝检查接口
            Map<String,Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);
            if (!resultMap.isEmpty()){
                String trade_status=(String)resultMap.get("trade_status");
                //根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功更新数据和后续任务
                if("TRADE_SUCCESS".equals(trade_status)){
                    //支付成功，更新支付发送支付队列
                    PaymentInfo paymentInfo = new PaymentInfo();
                    paymentInfo.setOrderSn(out_trade_no);
                    paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));
                    paymentInfo.setPaymentStatus("已支付");
                    paymentInfo.setCallbackContent((String) resultMap.get("callback_content"));
                    paymentInfo.setCallbackTime(new Date());
                    paymentService.updatePaymentInfoByOrderSn(paymentInfo);
                    return;
                }
            }
            if(count>0){
                count--;
                //继续发送延迟检查任务，计算延迟时间等。
                paymentService.sendDelayPaymentResultCheckQueue(out_trade_no,count);
            }else{
                System.out.println("检查剩余次数用尽！");

            }

        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
