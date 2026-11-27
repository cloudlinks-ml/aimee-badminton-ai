# Aimee Badminton AI

纯 Android 本地羽毛球辅助记分原型。当前里程碑 M1：相机采集与性能基准测试。

## M1 功能
- CameraX 后置摄像头实时预览
- YUV 图像分析帧尺寸与 FPS
- 单帧分析耗时、Java Heap、系统热状态展示
- 不上传视频、不调用云端服务

## 构建
GitHub Actions 会在推送到 main 或手动触发时构建 Debug APK，并上传名为 `aimee-badminton-m1-debug-apk` 的构建产物。

## 安装
下载 Actions 运行产物 ZIP，解压后安装 `app-debug.apk`。首次启动需授予相机权限。

## 当前限制
本版本尚未包含羽毛球检测模型、轨迹识别或自动计分。FPS 指标为图像分析管线处理帧率，不等同于传感器原生输出 FPS。
