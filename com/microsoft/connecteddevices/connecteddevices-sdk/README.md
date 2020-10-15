# Project Rome - Maven Repository for Android SDK

Project Rome SDK v1 for Android will be hosted on GitHub starting 2021. This branch, 'mvn-repo',
is created for hosting the SDK artifacts.

### How to use

The changes required are simple if your android project is configured by Gradle.
Please add the following snippet to your build.gradle script at the project/module level.
This resolves the dependency by treating this GitHub branch as a Maven repository.

```groovy
allprojects {
  repositories {
    google()
    jcenter()
    maven {
      url "https://raw.github.com/microsoft/project-rome/mvn-repo/"
    }
  }
}
```