# WBXposed

Forked from: [wangyuan0217/MyXposed](https://github.com/wangyuan0217/MyXposed)

仅保留微博轻享版（国际版，包名 `com.weico.international`）去广告，使用 [libxposed API 102](https://github.com/libxposed/api)。没有桌面入口、图标或设置页面，不修改暗黑模式和首页发布按钮。

## 功能

* 拦截开屏及回到前台时的广告。
* 拦截时间线广告请求及相关展示入口。
* 屏蔽原有 Hook 涉及的视频广告配置。

## 使用要求

* Android 8.0（API 26）及以上，与 libxposed API 102 的最低要求一致。
* 需要支持 **libxposed API 102** 的 LSPosed 或兼容框架；旧版仅支持传统 Xposed API 的框架无法加载此版本。
* 安装模块后在框架中启用，作用域仅选择微博轻享版/国际版，然后强行停止微博并重新启动。
* 不支持常规微博客户端 `com.sina.weibo`。

广告方法映射保留原项目从 6.1.7 起的版本分支，包括 6.9.8 起使用 `queryUveAdRequest$lambda$21` 的修正。映射不代表所有后续微博版本均经真机验证；升级后如有失效，可查看框架内 `WBXposed` 日志。

## 构建

* `compileSdk` / `targetSdk`：36；`minSdk`：26。
* Android Gradle Plugin：9.4.0；Gradle Wrapper：9.7.1。
* 构建 JDK：Temurin 25 LTS；Java 源码与字节码级别：17。
* Android SDK Platform 36、Build Tools 36.0.0。
* 仅编译依赖 `io.github.libxposed:api:102.0.0`，不将框架 API 打包进 APK。

```sh
./gradlew assembleDebug assembleRelease lintRelease --stacktrace --console=plain
```

无发布签名配置时生成未签名 Release APK，便于本地和 Pull Request 构建验证。正式发布仍使用原有 `signing.properties` 或 `STORE_FILE`、`STORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 环境变量。

GitHub Actions 使用 Node.js 24 版 checkout、setup-java 和 upload-artifact。主分支构建沿用仓库的 `SIGNING_KEY`、`KEY_STORE_PASSWORD`、`ALIAS` Secrets 签名，产物位于每次构建的 **Release** 附件。

模块入口、作用域和 API 要求位于 `app/src/main/resources/META-INF/xposed/`，使用新版入口协议，不再包含旧 `assets/xposed_init` 或 Xposed Manifest 元数据。
