
此项目由VTEXS赞助的个人学习使用项目,非常感谢赞助[**VTEXS地址**](https://console.vtexs.com)

# onlyoffice-springboot-demo
springboot接入在线文档onlyoffice
请先去docker中拉取onlyOffice镜像 我这里用的8.3，构建指令网上都有自己
暂时只研究了后端集成部分
## 系统要求
- jdk版本17
- springboot 版本3.3
- Node.js 16.0 或更高版本
- Minoi 服务端
- OnlyOffice Document Server
- 文档编辑器Document
## 安装步骤

### 1. 安装依赖

```bash
npm install
```

### 2. 修改配置

1. 修改服务地址：
   - 打开 `vite.config.js`
   - 修改  配置为您服务端地址：
   ```javascript
   target: 'http://your-server:port'

   ```

2. 修改 OnlyOffice Document Server 地址：
   - 打开 `src/components/OnlyOfficeViewer.vue`
   - 修改 `documentServerUrl` 和 `callbackUrl` 配置：
   ```javascript
   documentServerUrl="http://your-onlyoffice-server:port/"
   "callbackUrl": "http://your-server:port/api/onlyoffice/callback"
   
   记得后端也修改一下，在application.yml中
   ```

### 3. 启动开发服务器

```bash
npm run dev
```

## 部署说明

### 构建生产版本

```bash
npm run build
```

构建后的文件将位于 `dist` 目录中。

### 部署到服务器

1. 将 `dist` 目录下的文件复制到您的Web服务器目录
2. 确保Web服务器正确配置了路由重写规则，以支持Vue Router的history模式
3. 配置Nginx/Apache等Web服务器，确保正确处理静态文件请求

## 注意事项

1. 确保Minoi服务端已正确安装并运行
2. OnlyOffice Document Server必须正确配置并运行
3. 确保所有服务之间的网络连接正常
4. 如果使用HTTPS，请确保所有服务都配置了正确的SSL证书

## 常见问题

1. 如果OnlyOffice无法加载文档，请检查Document Server的配置和网络连接

## 技术支持

如有问题，请联系技术支持团队或提交Issue。
