import com.gradle.publish.MavenCoordinates

plugins {
   `java-gradle-plugin`
   kotlin("jvm") version "1.3.41"
   id("com.gradle.plugin-publish") version "0.12.0"
}

group = "us.ihmc"
version = "0.4"

repositories {
   mavenCentral()
   jcenter()
}

dependencies {
   compile(gradleApi())
   compile(kotlin("stdlib"))
}

gradlePlugin {
   plugins {
      register("scsGradlePlugin") {
         id = project.group as String + "." + project.name
         displayName = "SCS Gradle Plugin"
         implementationClass = "us.ihmc.scs.SCSPlugin"
         description = "Property initialization for IHMC's Simulation Construction Set."
      }
   }
}

pluginBundle {
   website = "https://github.com/ihmcrobotics/simulation-construction-set"
   vcsUrl = "https://github.com/ihmcrobotics/simulation-construction-set"
   description = "Runtime configuration for IHMC's Simulation Construction Set."
   tags = listOf("scs", "property", "initialization", "ihmc", "robotics", "simulation")

   plugins.getByName("scsGradlePlugin") {
      id = project.group as String + "." + project.name
      version = project.version as String
      displayName = "SCS Gradle Plugin"
   }

   mavenCoordinates(closureOf<MavenCoordinates> {
      groupId = project.group as String
      artifactId = project.name
      version = project.version as String
   })
}