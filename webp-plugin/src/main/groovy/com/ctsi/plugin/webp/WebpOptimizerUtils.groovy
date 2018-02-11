package com.ctsi.plugin.webp

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import javax.imageio.ImageIO
import java.awt.image.BufferedImage;

class WebpOptimizerUtils {

    def static final DRAWABLE = "drawable"
    def static final MIPMAP = "mipmap"
    def static final PNG9 = ".9.png"
    def static final PNG = ".png"
    def static final JPG = ".jpg"
    def static final JPEG = ".jpeg"


    def static isImgFolder(File file) {
        return file.name.startsWith(DRAWABLE) || file.name.startsWith(MIPMAP)
    }

    def static isPreOptimizePng(File file) {
        return (file.name.endsWith(PNG) || file.name.endsWith(PNG.toUpperCase())) && !file.name.endsWith(PNG9) && !file.name.endsWith(PNG9.toUpperCase())
    }

    def static isPreOptimizeJpg(File file) {
        return file.name.endsWith(JPG) || file.name.endsWith(JPEG) || file.name.endsWith(JPG.toUpperCase()) || file.name.endsWith(JPEG.toUpperCase())
    }

    def static isTransparent(File file) {
        BufferedImage img = ImageIO.read(file);
        return img.colorModel.hasAlpha()
    }

    def static getTool(Project project, String name) {
        def toolName
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            toolName = "${name}_win.exe"
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            toolName = "${name}_darwin"
        } else {
            toolName = "${name}_linux"
        }
        def path = "${project.buildDir.absolutePath}/tools/$name/$toolName"
        def file = new File(path)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            new FileOutputStream(file).withStream {
                def inputStream = WebpOptimizerUtils.class.getResourceAsStream("/$name/${toolName}")
                it.write(inputStream.getBytes())
            }
        }
        if (file.exists() && file.setExecutable(true)) {
            return file.absolutePath
        }
        throw GradleException("$toolName 工具不存在或者无法执行")
    }

}