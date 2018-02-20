package com.ctsi.plugin.webp

import groovy.xml.Namespace
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class WebpOptimizerTask extends DefaultTask {

    static def PNG_TOOL = "pngcrush"
    static def JPG_TOOL = "guetzli"
    static def WEBP_TOOL = "cwebp"


    @Input
    File manifestFile

    //需要打包的资源目录
    @Input
    File res
    // minSdkVersion
    @Input
    int apiLevel



    def webpTool
    def jpgTool
    def pngTool
    String launcher
    String round_launcher

    WebpOptimizerTask() {
        group "webp"
        webpTool = WebpOptimizerUtils.getTool(project, WEBP_TOOL)
        jpgTool = WebpOptimizerUtils.getTool(project, JPG_TOOL)
        pngTool = WebpOptimizerUtils.getTool(project, PNG_TOOL)
    }

    @TaskAction
    def run() {
        project.logger.error "==============================start convert to Webp================================="
        /**
         * 1、解析AndroidManifest.xml获得 app的icon(以及roundIcon)
         */
        project.logger.error "1、parse AndroidManifest.xml."
        // dom sax 解析 xml 这里会用gdk 内的函数解析
        def ns = new Namespace("http://schemas.android.com/apk/res/android", "android")
        Node xml = new XmlParser().parse(manifestFile)
        Node application = xml.application[0]
        //@mipmap/ic_launcher
        launcher = application.attributes()[ns.icon]
        round_launcher = application.attributes()[ns.roundIcon]
        launcher = launcher.substring(launcher.lastIndexOf("/") + 1, launcher.length())
        if (null != round_launcher)
            round_launcher = round_launcher.substring(round_launcher.lastIndexOf("/") + 1, round_launcher.length())
        else round_launcher = ""
       // round_launcher = round_launcher.substring(round_launcher.lastIndexOf("/") + 1, round_launcher.length())


        /**
         * 2、遍历需要打包的res资源目录 获得所有图片资源
         */
        project.logger.error "2、find resdir all picture file."
        //获得待压缩/转换资源
        def pngs = []
        def jpgs = []
        res.eachDir { dir ->
            if (WebpOptimizerUtils.isImgFolder(dir)) {
                dir.eachFile { f ->
                    //launcher就不管
                    if (WebpOptimizerUtils.isPreOptimizeJpg(f) && isNonLauncher(f))
                        jpgs << f
                    if (WebpOptimizerUtils.isPreOptimizePng(f) && isNonLauncher(f))
                        pngs << f
                }
            }
        }
        /**
         * 3、开始压缩/转换
         */
        project.logger.error "3、compress/convert."
        //无效的转换图片(转换后比原图还大)
        def jpegsNotConvert = []
        def pngNotConvert = []
        if (apiLevel >= 14 && apiLevel < 18) {
            //记录不能转换为webp的png图片 用于压缩
            def compress = []
            pngs.each {
                //如果有alpha通道 则压缩
                if (WebpOptimizerUtils.isTransparent(it)) {
                    compress << it
                    project.logger.error "   ${it.name} has alpha channel,don't convert webp"
                } else {
                    //转换webp
                    convertWebp(webpTool, it,pngNotConvert)
                }
            }
            //压缩 png
            compressImg(pngTool, true, compress)
            //jpeg本身就不带alpha 都可以转换为webp
            jpgs.each {
                convertWebp(webpTool, it,jpegsNotConvert)
            }

        } else if (apiLevel >= 18) {
            //能够使用有透明的webp
            pngs.each {
                convertWebp(webpTool, it,pngNotConvert)
            }
            jpgs.each {
                convertWebp(webpTool, it,jpegsNotConvert)
            }
        } else {
            //不能使用webp 进行压缩
            compressImg(pngTool, true, pngs)
            compressImg(jpgTool, false, jpgs)
        }

        //转换无效的再压缩
        compressImg(pngTool, true, pngNotConvert)
        compressImg(pngTool, false, jpegsNotConvert)
    }

    def isNonLauncher(File f) {
        return f.name != "${launcher}.png" && f.name != "${launcher}.jpg" && f.name != "${round_launcher}.png" && f.name != "${round_launcher}.jpg"
    }

    //cwebp  -q quality in.png -o out.webp
    def convertWebp(String tool, File file,def noValidConvert) {
        //转换wenp
        def name = file.name
        name = name.substring(0, name.lastIndexOf("."))
        def output = new File(file.parent, "${name}.webp")
        //google 建议75的质量
        def result = "$tool -q 75 ${file.absolutePath} -o ${output.absolutePath}"
                .execute()
        result.waitFor()
        if (result.exitValue() == 0) {
            def rawlen = file.length()
            def outlen = output.length()
            if (rawlen > outlen) {
                file.delete()
            } else {
                //如果转换后的webp文件比源文件大 ， 后面还可以尝试 压缩
                noValidConvert << file
                output.delete()
                project.logger.error "   convert ${name} bigger than raw"
            }
        } else {
            noValidConvert << file
            project.logger.error "   convert ${file.absolutePath} to webp error"
        }
    }

    //pngcrush  -brute -rem alla -reduce -q in.png out.png
    //guetzli --quality quality  in.jpg out.jpg
    def compressImg(String tool, boolean isPng, def files) {
        files.each {
            File file ->
                def output = new File(file.parent, "temp-preOptimizer-${file.name}")
                def result
                if (isPng)
                    result = "$tool -brute -rem alla -reduce -q ${file.absolutePath}  ${output.absolutePath}"
                            .execute()
                else
                    result = "$tool --quality 84 ${file.absolutePath}  ${output.absolutePath}"
                            .execute()
                result.waitForProcessOutput()
                //压缩成功
                if (result.exitValue() == 0) {
                    def rawlen = file.length()
                    def outlen = output.length()
                    // 压缩后文件确实减小了
                    if (outlen < rawlen) {
                        //删除原图片
                        file.delete()
                        //将压缩后的图片重命名为原图片
                        output.renameTo(file)
                    } else {
                        project.logger.error "   compress ${file.name} bigger than raw"
                        output.delete()
                    }
                } else {
                    project.logger.error "  compress ${file.absolutePath} error"
                }
        }
    }
}