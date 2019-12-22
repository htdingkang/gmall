package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;

import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;


    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request,UmsMember umsMember){
        String token="";
        //调用用户服务，验证用户名密码
        UmsMember umsMemberlogin=userService.login(umsMember);
        if(umsMemberlogin!=null){
            //登录成功
            //用jwt工具制作token
            String memberId=umsMemberlogin.getId();
            String nickname = umsMemberlogin.getNickname();
            Map<String,Object> userMap=new HashMap<>();
            userMap.put("memberId",memberId);
            userMap.put("nickname",nickname);

            String ip=request.getHeader("x-forward-for");
            if(StringUtils.isBlank(ip)){
                ip=request.getRemoteAddr();
                if (StringUtils.isBlank(ip)){
                    ip="127.0.0.1";
                }
            }
            //真正项目中需要一些策略+算法，加密key与salt盐值
            token = JwtUtil.encode("gmall", userMap, ip);

            //redis中存储一份token信息
            userService.addUserToken(token,memberId);

        }else{
            //登录失败
            token="fail";
        }
        return token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String index(String frontIp,String token){
        //通过jwt校验真假
        Map<String,String> map=new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "gmall", frontIp);
        if(decode!=null){
            map.put("status","success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname",(String) decode.get("nickname"));
        }else{
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }

}
