## 一、webp-plugin 插件

*  **简介 :**
1. 该插件是将PNG、JPG、JPEG 格式的图片转换为Webp格式，减小apk的大小；
2. 在 minSdkVersion 小于18时，带有透明度的PNG 图片无法兼容4.3以下的机型，故此插件不转换，可兼容4.3以下设备；
3. 无法转换的图片进行了，或者是转换之后比原图片还大的图片进行了压缩，PNG图片使用pngcrush工具压缩，JPG、JPEG格式图片使用Google提供的guetzli工具压缩；
4. 该插件工具兼容Windows、Mac、Linux三个平台；
5. 该插件无法兼容AndroidStudio 3.0的buidle构建工具，需要将工具版本设置为小于3.0，或者在项目的gradle.properties中设置：`android.enableAapt2=false`


* **集成步骤**

1.  在项目根目录下buidle.gradle中添加插件
```
 dependencies {
        classpath 'com.ctsi.plugin:webp-plugin:2.0.0'
    }
```

2. 在app module中的buidle.gradle 中添加
```
apply plugin: 'com.ctsi.plugin.webp'
```
3. 在app module中的buidle.gradle 中配置webpConfig,默认开启,
```
webpConfig {
    quality = 75
    webpEnabled = true
}
```
4. 执行打包即可将图片转换为Webp 格式；
