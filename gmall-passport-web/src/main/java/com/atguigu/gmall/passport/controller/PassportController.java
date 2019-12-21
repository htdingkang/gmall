package com.atguigu.gmall.passport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PassportController {


    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(){
        //调用用户服务，验证用户名密码

        return "token";
    }

    @RequestMapping("verify")
    @ResponseBody
    public String index(String token){
        //通过jwt校验真假

        return "success";
    }

}
