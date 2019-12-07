# 启动 zookeeper
- 上传zookeeper-3.4.11.tar.gz到linux服务器/opt目录下
- 解压
- 配置
```
cd zookeeper-3.4.11
mkdir data
cd conf
cp zoo_sample.cfg zoo.cfg
vi zoo.cfg   #修改zookeeper数据路径
dataDir=/opt/zookeeper-3.4.11/data 
```
- 启动
```
cd /opt/zookeeper-3.4.11/bin
./zkServer.sh start
./zkServer.sh status
```

# tomcat启动dubbo-admin项目
- 下载apache-tomcat-8.5.24.tar.gz，dubbo-admin-2.6.0.war，上传到linux服务器/opt目录下
- 解压
```
cd /opt
tar -zxvf apache-tomcat-8.5.24.tar.gz
unzip dubbo-admin-2.6.0.war -d dubbo  
```
- 配置server.xml
```
cd apache-tomcat-8.5.24
vi conf/server.xml
#<Host></Host>标签中添加如下内容
#访问路径/dubbo，项目位置 /opt/dubbo，request.getContextPath() ==> /dubbo
#可以多个tomcat同时指向一个项目，而不是放在每个tomcat的webapps下
<Context path="/dubbo" docBase="/opt/dubbo" debug="0" privileged="true"/>
```
- 启动，停止tomcat
```
cd bin
./startup.sh   
./shutdown.sh
```

# 使用docker镜像部署zookeeper,dubbo-admin
- https://blog.csdn.net/qq_33562996/article/details/80599922
- dockerhub账号dingkango，里面有dubbo-admin的封装镜像

# 使用 dubbo 注意点
- dubbo在进行dubbo协议通讯时，需要实现序列化接口（封装的数据对象）
- consumer在三秒之内每隔一秒进行一次重新访问，默认一秒钟超时，三次访问超时之后会直接抛超时异常。我们在开发阶段可以将consumer设置的超时延长，方便debug
- consumer启动时默认会检查注入的provider是否存在。否则报错。我们可以配置关闭掉检查。
```
# 设置超时时间
spring.dubbo.consumer.timeout=600000
# 设置是否检查服务存在
spring.dubbo.consumer.check=false
```
# gmall-admin前端
- nodejs,npm安装
- config/dev.env.js    修改后端地址，端口
- index.js   修改前端访问地址，端口
- 编译启动  cd 到 gmall-admin目录下 运行 npm run dev
# 跨域问题
<br>gmall-admin前端：127.0.0.1:8888
<br>gmall-manage-web后端：127.0.0.1:8081
<br>两者来自不同的网域，所以在http的安全协议策略下，不信任。需要任意一方加入跨域协议。
<br>后台解决方案：在springmvc的控制层加入@CrossOrigin跨域访问的注解。添加完成后注意看请求Response Headers中多出的部分
![image](./pic/1.png)

# 网段变化后，虚拟机中配置步骤
```
#配置固定ip，虚拟机内外网段保持一致
vi /etc/sysconfig/network-scripts/ifcfg-enp0s3
IPADDR=

service network restart

#fastdfs storage配置ip变更
vi /etc/fdfs/storage.conf
tracker_server= 

service fdfs_storaged restart 

# fastdfs-nginx-module 配置ip变更
vi /etc/fdfs/mod_fastdfs.conf
tracker_server=

# nginx 配置变更
vi /usr/local/nginx/conf/nginx.conf
server_name 

/usr/local/nginx/sbin/nginx
/usr/local/nginx/sbin/nginx -s reload 
```
# docker 安装fastdfs
```
docker pull morunchang/fastdfs
docker run -d --name tracker --net=host morunchang/fastdfs sh tracker.sh
#TRACKER_IP配置docker所在主机的ip
docker run -d --name storage --net=host -e TRACKER_IP=192.168.9.108:22122 -e GROUP_NAME=group1 morunchang/fastdfs sh storage.sh

docker exec -it storage /bin/bash
vi /etc/nginx/conf/nginx.conf
# 修改nginx访问 监听端口和server_name
listen       80;
server_name  192.168.9.108;

#重启storage

```

# fastdfs-client-java
<br>git clone后拷贝到gmall项目下，file->new->module from existing sources ->选择fastdfs-client-java->选择maven 
<br>maven install 装到仓库后即可删除。
<br>gmall-manage-web中 maven dependency引入


# idea启动多个实例
Edit Configurations -> Allow parallel run

# nginx 负载均衡
```
upstream redisTest {
        server   192.168.9.250:9001 weight=3;
        server   192.168.9.250:9002  weight=3;
        server   192.168.9.250:9003  weight=3;
}
server {
        listen       9000;
        server_name  192.168.9.108;

        location / {
                root   html;
                index  index.html index.htm;
                proxy_pass  http://redisTest;
        }
}
```

# 使用Apache压力测试工具
```
yum -y install httpd-tools
# ab [options] [http[s]://]hostname[:port]/path
ab -c 200 -n 1000 http://192.168.9.108:9000/testRedisson   # 并发200，共发1000次请求
```



# elasticsearch，kibana安装配置
```
mkdir -p /opt/es
# 上传安装包，elasticsearch-6.3.1.tar.gz  elasticsearch-analysis-ik6.rar  kibana-6.3.1-linux-x86_64.tar.gz
# 解压elasticsearch-6.3.1.tar.gz
# es6 root用户无法启动,创建其他用户,临时处理权限问题
cd /opt/es
chmod 777 -R elasticsearch-6.3.1
adduser es
su es
# 配置测试环境启动占用jvm内存空间，默认是1g， 如果jvm.options中没有内容，需要切换到root用户进行操作
vi /opt/es/elasticsearch-6.3.1/config/jvm.options
-Xms256m
-Xmx256m
# 配置外网访问url
vi /opt/es/elasticsearch-6.3.1/config/elasticsearch.yml 
network.host: 192.168.9.108
http.port: 9200
# 启动命令
/opt/es/elasticsearch-6.3.1/bin/elasticsearch
# 启动后退出控制台（后台启动）
nohup /opt/es/elasticsearch-6.3.1/bin/elasticsearch &  

报错
[1]: max file descriptors [4096] for elasticsearch process is too low, increase to at least [65536]
[2]: max number of threads [3868] for user [es] is too low, increase to at least [4096]
[3]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
解决方案
[1][2]
ulimit -Hn  #查看nofile硬限制  -S 软限制
ulimit -Hu  #查看nproc 硬限制
su root   
vi /etc/security/limits.conf 
添加
es soft nofile 65536
es hard nofile 65536
es soft nproc  4096
es hard nproc  4096
# 用户重新登录（切换）生效
[3]:
vi /etc/sysctl.conf
添加
vm.max_map_count=655360
#然后执行命令
sysctl -p

#配置启动kibana
vi /opt/es/kibana-6.3.1-linux-x86_64/config/kibana.yml 
server.host: "0.0.0.0"
elasticsearch.url: "http://192.168.9.108:9200"
# cd 到 bin目录下，启动
nohup /opt/es/kibana-6.3.1-linux-x86_64/bin/kibana  &
# 查看进程id
ps -ef | grep kibana
ps -ef | grep node  
# 访问地址
http://192.168.9.108:5601
```



