package com.atguigu.gmall.interceptors;

import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{

        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            //判断被拦截的请求访问的方法上是否有@LoginRequired注解(是否需要拦截)
            HandlerMethod hm=(HandlerMethod) handler;
            LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);
            //是否拦截
            if(methodAnnotation==null){
                return true;
            }

            //四种情况
            //                         oldToken为空       oldToken不为空
            //     newToken为空         未登陆过            之前登陆过
            //   newToken不为空         刚登录              过期

            String token = "";
            String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
            if (StringUtils.isNotBlank(oldToken)) {
                token = oldToken;
            }
            String newToken = request.getParameter("token");
            if (StringUtils.isNotBlank(newToken)) {
                token = newToken;
            }

            //调用验证中心进行验证,httpclient
            String success = "fail";
            if (StringUtils.isNotBlank(token)) {
                success = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token);
            }

            //是否必须登录
            boolean loginSuccess = methodAnnotation.loginSuccess();

            if("success".equals(success)){
                //验证通过，无论是否必须登录，都需要进行以下操作
                //需要将token携带的用户信息写入request
                request.setAttribute("memberId","1");
                request.setAttribute("nickname","nicknamme");
                //验证通过，覆盖cookie中的token
                CookieUtil.setCookie(request,response,"oldToken",token,60*20,true);
            }else{
                //token验证失败并且需要登录的
                if(loginSuccess){
                    //重定向回passport登录页面
                    StringBuffer requestURL = request.getRequestURL();
                    response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+requestURL);
                    return false;
                }

            }
            return true;
        }
}

