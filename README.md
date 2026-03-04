# FiaCloud Android

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

FiaCloud Android 是一款基于 Jetpack Compose 开发的 S3 兼容对象存储客户端。它提供简洁直观的界面，让您可以轻松管理存储在 AWS S3、MinIO 或其他 S3 兼容平台上的文件。

## ✨ 功能特性

- 📁 **文件管理**：支持浏览、上传、下载、删除和重命名 S3 存储桶中的文件及文件夹。
- 🔍 **多格式预览**：
    - **Markdown**：支持富文本渲染。
    - **CSV**：表格化展示数据。
    - **媒体文件**：内置图片查看器（支持缩放）、视频播放器和音频播放器。
    - **文本/代码**：内置文本编辑器，支持常见编程语言的语法高亮。
- 🎨 **现代 UI**：采用 Material 3 设计规范，支持深色模式。
- ⚙️ **灵活配置**：轻松配置 S3 端点（Endpoint）、Access Key、Secret Key 及存储桶信息。
- 🚀 **性能优化**：通过代码压缩和资源优化（R8/Proguard）确保应用轻量高效。
- 🔄 **自动更新**：内置版本检查功能，确保您始终使用最新版本。

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose (Material 3)
- **架构**：MVVM (ViewModel, Flow)
- **存储 SDK**：AWS SDK for Java (S3)
- **本地配置**：Jetpack DataStore
- **序列化**：Gson
- **图标**：Material Symbols

## 🚀 快速开始

### 环境要求

- Android 设备或模拟器 (API 33+)
- Android Studio Ladybug 或更高版本
- JDK 11+

### 构建与运行

1. 克隆仓库：
   ```bash
   git clone https://github.com/your-username/FiaCloudAndroid.git
   ```
2. 使用 Android Studio 打开项目。
3. 等待 Gradle 同步完成。
4. 点击 **Run** 按钮部署到您的设备。

## ⚙️ 配置说明

在使用前，请在应用的“设置”页面配置以下信息：
- **Endpoint**: 您的 S3 服务地址（例如 `https://s3.amazonaws.com` 或您的 MinIO 地址）。
- **Region**: 存储桶所在的区域。
- **Bucket**: 存储桶名称。
- **Access Key**: 您的访问密钥。
- **Secret Key**: 您的私有密钥。

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议。

```text
Copyright 2026 FiaCloud

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
