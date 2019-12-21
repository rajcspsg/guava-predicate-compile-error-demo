allprojects {
  group = "org.example"
  version = "1.0-SNAPSHOT"
}

subprojects {

  apply(plugin = "java")

  repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
  }

  configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
