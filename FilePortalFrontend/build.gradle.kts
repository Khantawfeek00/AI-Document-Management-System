plugins {
    // lightweight gradle plugin to run node tasks
    id("com.github.node-gradle.node") version "5.0.0"
}

node {
    version.set("18.17.1")
    download.set(true)
    // set working directory to frontend
    nodeProjectDir.set(file("."))
}


tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildFrontend") {
    dependsOn("npmInstall")
    args.set(listOf("run", "build"))
}

// NOTE: this is a frontend-only module; it doesn't have the standard 'assemble' lifecycle task
// so we don't hook buildFrontend into assemble here to avoid TaskNotFoundException
