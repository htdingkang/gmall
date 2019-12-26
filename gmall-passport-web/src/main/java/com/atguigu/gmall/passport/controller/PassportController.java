package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;

import com.atguigu.gmall.util.HttpclientUtil;
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

    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request) {
        //授权码换取access_token
        String access_token_url = "https://api.weibo.com/oauth2/access_token";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id", "4127982357");
        paramMap.put("client_secret", "bc5fb8cde138655d4ecbaff69cfb3db8");
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        paramMap.put("code", code);
        String access_token_json = HttpclientUtil.doPost(access_token_url, paramMap);
        Map<String, String> access_token_map = JSON.parseObject(access_token_json, Map.class);
        //access_token换取用户信息
        String user_url = "https://api.weibo.com/2/users/show.json?access_token="
                + access_token_map.get("access_token") + "&uid="
                + access_token_map.get("uid");
        String user_json = HttpclientUtil.doGet(user_url);
        JSONObject jsonObject = JSON.parseObject(user_json);
        //将用户信息保存到数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token_map.get("access_token"));
        umsMember.setSourceUid(jsonObject.getString("idstr"));
        umsMember.setSourceType("2");
        umsMember.setCity(jsonObject.getString("location"));
        String gender = jsonObject.getString("gender");
        umsMember.setGender("n".equals(gender)?"0":"m".equals(gender)?"1":"2");
        umsMember.setNickname(jsonObject.getString("screen_name"));

        UmsMember umsCheck=new UmsMember();
        umsCheck.setSourceUid(jsonObject.getString("idstr"));
        UmsMember umsMemberDb = userService.checkOauthUser(umsCheck);
        if(umsMemberDb==null){
            userService.addOauthUser(umsMember);
        }else{
            umsMember=umsMemberDb;
        }
        //生成jwt的token，并重定向到首页，携带该token
        String token = "";
        String memberId=umsMember.getId();
        String nickname = umsMember.getNickname();
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


        return "redirect:http://search.gmall.com:8083/index?token=" + token;
    }


}
