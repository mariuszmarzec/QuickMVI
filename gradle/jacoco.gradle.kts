val kotlinTree = fileTree("${project.buildDir}/tmp/kotlin-classes/stageDebug") {
}

val kotlinSrc = "${project.projectDir}/src/main/java"

tasks.create("jacocoTestReportDebug", JacocoReport::class) {

    reports {
        xml.required.set(false)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }

    classDirectories.setFrom(files(kotlinTree))

    additionalSourceDirs.setFrom(files(kotlinSrc))
    sourceDirectories.setFrom(files(kotlinSrc))
    executionData.setFrom(
        fileTree(
            baseDir = project.projectDir
        ) {
            include(
                "**/*.exec",
                "**/*.ec"
            )
        }
    )
}


tasks.create("jacocoTestCoverageVerification", JacocoCoverageVerification::class) {
    dependsOn("jacocoTestReportDebug")

    violationRules {
        rule {
            element = "CLASS"
            limit {
                counter = "LINE"
                value = "COVERED_RATIO"
                minimum = 0.70.toBigDecimal()
            }
        }
        rule {
            element = "METHOD"
            limit {
                counter = "LINE"
                value = "COVERED_RATIO"
                minimum = 0.70.toBigDecimal()
            }
        }
    }
}


