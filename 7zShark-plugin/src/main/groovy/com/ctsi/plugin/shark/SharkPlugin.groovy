package com.ctsi.plugin.shark

import org.gradle.api.Plugin
import org.gradle.api.Project

class SharkPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('testTask') << {
            println "Hello gradle plugin"
        }
    }
}
