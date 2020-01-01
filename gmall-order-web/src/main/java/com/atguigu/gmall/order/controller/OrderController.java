package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;

import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {
    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;
    @Reference
    SkuService skuService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public String submitOrder(String receiveAddressId,String tradeCode,HttpServletRequest request, ModelMap modelMap){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        //同一个订单不可提交多次，为了防止提交完了订单到达结算页面后利用浏览器的回退，又回到上一步重新提交订单。
        //在结算页面生成一个交易码，且只能使用一次。
        //检查交易码
        String success = orderService.checkTradeCode(memberId,tradeCode);

        if ("success".equals(success)) {
            OmsOrder omsOrder=new OmsOrder();
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            String outTradeNo="gmall";
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo += sdf.format(new Date());
            outTradeNo +=System.currentTimeMillis();//将毫秒时间戳拼接到外部订单号
            omsOrder.setOrderSn(outTradeNo); //外部订单号，用来和外部系统进行交互，防止重复

            UmsMemberReceiveAddress address = userService.getUmsMemberReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverProvince(address.getProvince());
            omsOrder.setReceiverCity(address.getCity());
            omsOrder.setReceiverRegion(address.getRegion());
            omsOrder.setReceiverDetailAddress(address.getDetailAddress());
            omsOrder.setReceiverName(address.getName());
            omsOrder.setReceiverPhone(address.getPhoneNumber());

            omsOrder.setStatus(0); //已提交

            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            //根据用户id获取要购买的商品列表（购物车）和总价格
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            BigDecimal totalPrice = new BigDecimal("0.00");
            for (OmsCartItem omsCartItem : omsCartItems) {
                if("1".equals(omsCartItem.getIsChecked())){
                    //获得订单详情列表
                    OmsOrderItem omsOrderItem=new OmsOrderItem();
                    //验价
                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if(b==false){
                        return "fail";
                    }
                    // 验库存,远程掉用库存系统

                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());

                    omsOrderItem.setOrderSn(outTradeNo);//外部订单号，用来和外部系统进行交互，防止重复
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编号"); //在仓库中的skuId
                    omsOrderItems.add(omsOrderItem);
                    totalPrice=totalPrice.add(omsOrderItem.getProductPrice().multiply(omsOrderItem.getProductQuantity()));
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);
            omsOrder.setTotalAmount(totalPrice);  //总价格


            //将订单和订单详情写入数据库
            //删除购物车的对应商品
            orderService.saveOrder(omsOrder);

            //重定向到支付系统
            return null;
        } else {
            return "fail";
        }
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        //收件人地址列表
        List<UmsMemberReceiveAddress> umsMemberReceiveAddress = userService.getUmsMemberReceiveAddress(memberId);
        //将购物车集合转化为页面清单集合
        List<OmsCartItem> omsCartItems=cartService.cartList(memberId);

        List<OmsOrderItem> omsOrderItems=new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            if ("1".equals(omsCartItem.getIsChecked())) {
                OmsOrderItem omsOrderItem=new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItems.add(omsOrderItem);
            }
        }
        modelMap.put("userAddressList",umsMemberReceiveAddress);
        modelMap.put("orderDetailList",omsOrderItems);
        modelMap.put("totalAmount",getTotalAmount(omsCartItems));
        //生成交易码，由于订单只能提交一次，提供给提交订单时校验使用
        String tradeCode = orderService.genTradeCode(memberId);
        modelMap.put("tradeCode",tradeCode);
        return "trade";
    }
    private BigDecimal getTotalAmount(List<OmsCartItem> cartList) {
        BigDecimal totalAmount=new BigDecimal("0.00");
        for (OmsCartItem omsCartItem : cartList) {
            if("1".equals(omsCartItem.getIsChecked())){
                totalAmount = totalAmount.add(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
            }
        }
        return totalAmount;
    }
}
