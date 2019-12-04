package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ItemController {
    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;



    @RequestMapping("{skuId}.html")
    public String index(@PathVariable String skuId,ModelMap map){
        PmsSkuInfo pmsSkuInfo=skuService.getSkuById(skuId);
        //sku对象
        map.put("skuInfo",pmsSkuInfo);
        //销售属性，属性值选项
        List<PmsProductSaleAttr> pmsProductSaleAttrs=spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        return "item";
    }














    //thymeleaf测试
    @RequestMapping("index")
    public String index(ModelMap modelMap){
        modelMap.put("hello","hello thymeleaf!");
        List<String> list=new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("循环数据"+i);
        }
        modelMap.put("list",list);
        modelMap.put("check","1");
        return "index";
    }

}
