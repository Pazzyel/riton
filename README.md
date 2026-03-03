# RITON

## 快速启动

在工作目录下创建`/target`目录，里面应该有项目对应的jar包，如`/target/riton-1.0.0.jar`

如果需要启动时执行指定SQL，在`/src/main/resources/db/riton.sql`中加入需要执行的执行的SQL

在工作目录下运行`docker compose up -d`即可启动项目及所需环境

## 注意事项

1. 前端请求被`@RequireTokenCheck`注解的controller，必须先请求`/optoken`，带上该controller的URI作为参数`requestPath`， 
得到对应的token后，将`operation-token`请求头的内容设置为获得的token，才能正常完成该请求。这是为了保证请求的幂等性。
没有token，已经被使用的token都将导致请求被拒绝

2. 只有通过/voucher/seckill添加的秒杀团购券，才能加载库存到Redis
