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
