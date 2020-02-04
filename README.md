# user服务
- gmall-user-service    8070
- gmall-user-web        8090 
# manage服务
- gmall-manage-service   8071
- gmall-manage-web   8081

# 商品详情服务  
gmall-item-web  8082   直接调用 gmall-manage-service获取spu，sku相关内容 

# 商品搜索服务
gmall-search-service  8073     
gmall-search-web   8083    

# 购物车服务
gmall-cart-service   8074
gmall-cart-web       8084

#用户认证中心
gmall-passport-web   8085  调用 gmall-user-service  

#订单服务
gmall-order-service       8076
gmall-order-web           8086

#支付服务
gmall-payment             8087

#秒杀服务
gmall-seckill             8001


# TO DO
1. 登录合并购物车，删除cookie中的购物车数据
2. elasticsearch数据更新，热度值问题处理。
3. 购物车列表商品数量的+-，删除。
4. 我的购物车页面链接
5. 我的订单页面链接