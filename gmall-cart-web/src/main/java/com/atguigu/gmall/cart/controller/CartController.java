package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;



//待完善，分登录未登录状态，
//添加 +,-,删除功能
    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked,String skuId,
            HttpServletRequest request,HttpServletResponse response,ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        //调用服务，修改状态
        OmsCartItem omsCartItemParam=new OmsCartItem();
        omsCartItemParam.setMemberId(memberId);
        omsCartItemParam.setProductSkuId(skuId);
        omsCartItemParam.setIsChecked(isChecked);
        cartService.checkCart(omsCartItemParam);
        List<OmsCartItem> cartList = cartService.cartList(memberId);
        cartList.sort(new Comparator<OmsCartItem>() {
            @Override
            public int compare(OmsCartItem o1, OmsCartItem o2) {
                return o1.getCreateDate().compareTo(o2.getCreateDate());
            }
        });
        BigDecimal totalAmount = getTotalAmount(cartList);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartList",cartList);
        return "cartListInner";
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        List<OmsCartItem> cartList=new ArrayList<>();
        if(StringUtils.isNotBlank(memberId)){
            cartList=cartService.cartList(memberId);
        }else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                cartList = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }
        cartList.sort(new Comparator<OmsCartItem>() {
            @Override
            public int compare(OmsCartItem o1, OmsCartItem o2) {
                return o1.getCreateDate().compareTo(o2.getCreateDate());
            }
        });
        BigDecimal totalAmount = getTotalAmount(cartList);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartList",cartList);
        return "cartList";

    }

    private BigDecimal getTotalAmount(List<OmsCartItem> cartList) {
        BigDecimal totalAmount=new BigDecimal("0.00");
        for (OmsCartItem omsCartItem : cartList) {
            if("1".equals(omsCartItem.getIsChecked())){
                totalAmount = totalAmount.add(omsCartItem.getPrice().multiply(omsCartItem.getQuantity())) ;
            }
        }
        return totalAmount;
    }


    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes){
        //调用商品服务查询商品信息
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));

        //判断用户是否登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        List<OmsCartItem> omsCartItemList=new ArrayList<>();
        if(StringUtils.isBlank(memberId)){  //未登录

            //cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isBlank(cartListCookie)){
                omsCartItemList.add(omsCartItem);
            }else{
                omsCartItemList=JSON.parseArray(cartListCookie,OmsCartItem.class);
                boolean exist=judge_cart_exist_and_modify(omsCartItem,omsCartItemList);
                if(!exist){
                    //不存在则添加
                    omsCartItemList.add(omsCartItem);
                }
            }
            //cookie存在也需要覆盖掉原来的
            // 因为cookie属于浏览器，服务器拿到的是副本，需要重新add回去。而session则不需要。
            CookieUtil.setCookie(request,response,"cartListCookie"
                    ,JSON.toJSONString(omsCartItemList),60*60*72,true);
        }else{
            //已经登录
            OmsCartItem omsCartItemFromDb=cartService.getCartByMemberIdAndSkuId(memberId,skuId); //查询当前用户是否添加过本件商品
            if(omsCartItemFromDb==null){
                //用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);
            }else{
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
            }
            //同步缓存
            cartService.flushCartCache(memberId);
        }

        //这里用重定向的意义在于防止刷新，造成表单重复提交，减轻服务器压力，一般写数据库的url都要设计成重定向比较好
        //String url="redirect:/success?skuId="+skuId+"&quantity="+quantity;
        //return url;
        //带参数重定向既可以自己拼接url也可以利用spring提供的RedirectAttributes，效果一样
        redirectAttributes.addAttribute("skuId",skuId);
        redirectAttributes.addAttribute("quantity",quantity);
        return "redirect:/success";
    }
    @RequestMapping("success")
    private String success(String skuId, int quantity,ModelMap modelMap){
        //调用商品服务查询商品信息
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        modelMap.put("skuInfo",pmsSkuInfo);
        modelMap.put("skuNum",quantity);
        return "success";
    }

    /**
     * 判断list中是否存在当前item，存在的话顺便更新list
     * @param omsCartItem
     * @param omsCartItemList
     * @return
     */
    private boolean judge_cart_exist_and_modify(OmsCartItem omsCartItem, List<OmsCartItem> omsCartItemList) {
        boolean exit=false;
        for (OmsCartItem cartItem : omsCartItemList) {
            if(omsCartItem.getProductSkuId().equals(cartItem.getProductSkuId())){
                exit=true;
                cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                break;
            }
        }
        return exit;
    }
}
