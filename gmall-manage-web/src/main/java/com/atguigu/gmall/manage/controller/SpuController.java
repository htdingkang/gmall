package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@CrossOrigin
public class SpuController {

    @Reference
    SpuService spuService;

    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){
        List<PmsProductInfo> pmsProductInfos=spuService.spuList(catalog3Id);
        return pmsProductInfos;
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){

        return "success";
    }


    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile){
        //将图片或者文件上传到文件存储服务器

        //将图片的存储路径信息返回给前台一并存到数据库
        String imgUrl="https://imgcps.jd.com/ling/11977252/56ul5LmmL-e7j-euoS_nlJ_mtLs/5aW95LmmOTnlhYMxMOS7tg/p-5bd8253082acdd181d02fa4b/5dc85474.jpg";
        return imgUrl;
    }



}
