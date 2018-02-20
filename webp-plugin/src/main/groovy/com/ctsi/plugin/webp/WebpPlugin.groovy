package com.ctsi.plugin.webp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

class WebpPlugin implements Plugin<Project> {
    static final String SHRINK_CONFIG = "webpConfig"

    @Override
    void apply(Project project) {

        // 分析完成了 build.gradle 文件之后 再执行
        def config = project.extensions.create(SHRINK_CONFIG, WebpOptimizerExtensions)
        project.logger.error "测试"
//        if (!config.webpEnabled)
//            return

        project.afterEvaluate {
            project.android.applicationVariants.all {
                    //debug release
                BaseVariant variant ->
                    //配置开关
//                    def config = project.extensions.create(SHRINK_CONFIG, WebpOptimizerExtensions)
//                    if (config.webpEnabled) {
                    def task = project.tasks.create("webp${variant.name.capitalize()}", WebpOptimizerTask) {
                        //获得处理完成之后 确定的manifest文件
                        def processManifest = variant.outputs.first().processManifest
                        def processResources = variant.outputs.first().processResources
                        //android的gradle插件 2.3.3能直接拿到 3.0.0弃用
                        if (processManifest.properties['manifestOutputFile'] != null) {
                            manifestFile = processManifest.manifestOutputFile
                        } else if (processResources.properties['manifestFile'] != null) {
                            manifestFile = processResources.manifestFile
                        }
                        // build/intermediates/res/merged/debug [release]
                        res = variant.mergeResources.outputDir
                        apiLevel = variant.mergeResources.minSdk
                    }
//                    project.logger.error "quality:${project.optimizer.quality}"
                    //将android插件的处理资源任务依赖于自定义任务
                    variant.outputs.first().processResources.dependsOn task
                    task.dependsOn variant.outputs.first().processManifest
            }
//            }
        }
    }

}