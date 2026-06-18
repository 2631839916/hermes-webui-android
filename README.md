# Hermes WebUI Android App

Hermes WebUI 的 Android 原生应用，使用 WebView 包装。

## 构建方式

### 方式 1：本地构建
```bash
# 需要 Android SDK
./gradlew assembleDebug
```

### 方式 2：GitHub Actions（推荐）
1. Fork 本仓库
2. 推送代码到 main 分支
3. GitHub Actions 会自动构建 APK
4. 在 Actions 页面下载 APK

## 功能
- ✅ 完整的 WebView 支持
- ✅ 文件上传/下载
- ✅ 相机拍照
- ✅ 下拉刷新
- ✅ 全屏显示
- ✅ JavaScript 支持
- ✅ 深色主题

## 配置
修改 `MainActivity.java` 中的 `WEBUI_URL` 变量来设置 WebUI 地址。

## 权限
- 网络访问
- 存储读写
- 相机

## 截图
![Hermes WebUI](screenshots/app.png)
