## 版本

- vue/cli 5.0.x
- node 18.17.x

## 启动

(1) 启动后端

后端端口8081

(2) openapi爬取后端接口

```shell
openapi --input http://localhost:8081/api/v2/api-docs --output ./generated --client axios && node ./scripts/patch-openapi-config.js
```

(3) 启动前端

