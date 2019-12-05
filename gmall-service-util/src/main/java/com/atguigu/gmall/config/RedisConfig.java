package com.atguigu.gmall.config;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration   //spring容器启动就会自动读取@Configuration类，类中@Bean标注的方法返回对象将作为一个bean被注入到了spring中。
public class RedisConfig {
    // 读取配置文件中的redis的ip地址
    // 注意配置应该配在引用gmall-service-util的springboot工程中
    @Value("${spring.redis.host:disabled}")  //默认值disabled
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    @Bean
    public RedisUtil getRedisUtil(){
        if(host.equals("disabled")){
            return null;
        }
        RedisUtil redisUtil=new RedisUtil();
        redisUtil.initPool(host,port,database);
        return redisUtil;
    }

}
