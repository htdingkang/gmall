package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Autowired
    AlipayClient alipayClient;
    @Autowired
    PaymentService paymentService;

    @Reference
    OrderService orderService;

    @RequestMapping("index")
    @LoginRequired()
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap){

        String memberId=(String) request.getAttribute("memberId");
        String nickname=(String) request.getAttribute("nickname");
        modelMap.put("nickName",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody     //必须加@ResponseBody
    @LoginRequired()
    public String alipay(String outTradeNo){
        //根据外部订单号查询订单信息
        OmsOrder omsOrder=orderService.getOrderByOutTradeNo(outTradeNo);

        //获得一个支付宝请求的客户端（它不是一个链接，而是一个封装好的http的表单请求）
        String form="";
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        Map<String,Object> map= new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        //map.put("total_amount",omsOrder.getTotalAmount());
        map.put("total_amount",new BigDecimal("0.01"));  //测试，每次付款0.01
        map.put("subject","谷粒商城商品");              //建议查询订单子表的商品item集合，拼接标题
        String param= JSON.toJSONString(map);
        alipayRequest.setBizContent(param);
        //回调函数接口
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return form;   //直接返回带错误的页面，不用保存支付信息
        }

        System.out.println(form);  //查看请求支付宝的表单请求
/*<form name="punchout_form" method="post" action="https://openapi.alipay.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=LT9105peNtpPDo0xP8ueHRGsZfyCqG%2BU6ubO3zgjZjEElXIvWuaz19RjwzavGMZWxecI9vy6mC3tDZEXP%2F6ov7d87TDYA1tkihxt8cfDglwDoBf6XhcVCGq4QSrqEsV%2BL0aV0WK5%2BMeSDDQtZ2LRicWNJUI25SdU%2FDM8yPSKtPPzbPQSYwZgw6CUQfr9fAfWPHnG12vdAb5IY%2BacAsvGNyVBfrThiB%2Fl9E6X3q574VtiemHEmHF8t25cMXAC6r113DY8WcAYTk8EoSzOU0LWLSxYMSmDBSqAKgm9RrCnIWNpC6XavsVPK8qxU5jkdEpxdPoNfPZsastBLlAo64mpcw%3D%3D&return_url=http%3A%2F%2Fpayment.gmall.com%3A8087%2Falipay%2Fcallback%2Freturn&notify_url=http%3A%2F%2F60.205.215.91%2Falipay%2Fcallback%2Fnotify&version=1.0&app_id=2018020102122556&sign_type=RSA2&timestamp=2020-01-06+01%3A36%3A14&alipay_sdk=alipay-sdk-java-dynamicVersionNo&format=json">
<input type="hidden" name="biz_content" value="{&quot;out_trade_no&quot;:&quot;gmall202001060136101578245770977&quot;,&quot;total_amount&quot;:0.01,&quot;subject&quot;:&quot;谷粒商城商品demo&quot;,&quot;product_code&quot;:&quot;FAST_INSTANT_TRADE_PAY&quot;}">
<input type="submit" value="立即支付" style="display:none" >
</form>
<script>document.forms[0].submit();</script> */


        //生成并且保存用户的支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("谷粒商城商品"); //建议查询订单子表的商品item集合，拼接标题
        paymentInfo.setTotalAmount(omsOrder.getTotalAmount());
        paymentService.savePaymentInfo(paymentInfo);
        return form;
    }


    @RequestMapping("alipay/callback/return")
    @LoginRequired()
    public String alipayCallbackReturn(HttpServletRequest request, ModelMap modelMap){
        //获取支付宝的参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");  //支付宝的交易凭证号
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");  //null
        String callbackContent = request.getQueryString();
        //通过支付宝的paramsMap进行签名验证，2.0版本将paramsMap参数去掉了，导致同步请求没办法验证签名。
        //只能在异步通知的接口(notify_payment_url)中进行验签操作
        //同步接口中的某些返回值也不全


        if (StringUtils.isNotBlank(sign)) {  //模拟验证签名通过
            //更新用户的支付状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setPaymentStatus("成功付款");
            paymentInfo.setCallbackContent(callbackContent);
            paymentInfo.setCallbackTime(new Date());
            paymentService.updatePaymentInfoByOrderSn(paymentInfo);
        }

        //支付成功后，引起的系统服务变更，订单服务，库存服务，物流服务...

        return "finish";
    }

}
