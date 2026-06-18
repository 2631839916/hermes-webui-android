#!/bin/bash

# 设置环境变量
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 构建项目
echo "开始构建 Hermes WebUI APK..."
cd /data/data/com.termux/files/home/HermesWebUI

# 使用 gradle 构建
gradle assembleDebug 2>&1 | tail -20

echo "构建完成！"
