package com.atguigu.gmall;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.JwtUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
    public static void main(String[] args) {
        String encode = encode();
        base64Decode(encode);
    }

    private static String encode(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("memberId","1");
        map.put("nickname","zhangsan");
        String ip="127.0.0.1";
        String time=new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String encode = JwtUtil.encode("2019gmall1220", map, ip + time);
        System.out.println(encode);
        return encode;
    }

    /**
     * 一个JWT由三个部分组成：公共部分、私有部分、签名部分。最后由这三者组合进行base64编码得到JWT。
     * 编码结果中间个人信息部分是可以解密的，真正防伪的是签名部分
     * @param encode
     */
    private static void base64Decode(String encode){
        String tokenUserInfo = StringUtils.substringBetween(encode, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        System.out.println("64="+map);
    }

}
