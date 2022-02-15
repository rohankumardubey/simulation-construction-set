plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.6"
   id("us.ihmc.ihmc-cd") version "1.23"
   id("us.ihmc.log-tools-plugin") version "0.6.3"
   id("us.ihmc.scs")
}

ihmc {
   group = "us.ihmc"
   version = "0.21.16"
   vcsUrl = "https://github.com/ihmcrobotics/simulation-construction-set"
   openSource = true
   
   configureDependencyResolution()
   configurePublications()
}

val gui = categories.configure("gui")
gui.minHeapSizeGB = 6
gui.maxHeapSizeGB = 8
gui.doFirst = { scs.showGui() }

val video = categories.configure("video")
video.minHeapSizeGB = 6
video.maxHeapSizeGB = 8
gui.doFirst = { scs.captureVideo() }

mainDependencies {
   api("org.ejml:ejml-core:0.39")
   api("net.jafama:jafama:2.1.0")
   api("org.jfree:jfreechart:1.0.17")
   api("org.apache.xmlgraphics:batik-svggen:1.7")
   api("org.apache.xmlgraphics:batik-dom:1.7")
   api("net.sourceforge.jexcelapi:jxl:2.6.12")
   {
      exclude(group = "log4j", module = "log4j")
   }
   api("com.github.wendykierp:JTransforms:3.1")
   api("org.apache.commons:commons-math3:3.6.1")
   api("org.apache.commons:commons-lang3:3.12.0")
   api("org.apache.pdfbox:pdfbox:1.8.4")
   api("net.sourceforge.jmatio:jmatio:1.0")
   api("xml-apis:xml-apis:2.0.2")
   api("com.google.guava:guava:18.0")
   api("org.easytesting:fest:1.0.16")
   api("org.easytesting:fest-swing:1.2.1")
   {
      exclude(group = "org.easytesting", module = "fest")
   }

   api("us.ihmc:ihmc-yovariables:0.9.11")
   api("us.ihmc:ihmc-video-codecs:2.1.6")
   api("us.ihmc:euclid:0.17.1")
   api("us.ihmc:euclid-frame:0.17.1")
   api("us.ihmc:euclid-shape:0.17.1")
   api("us.ihmc:euclid-geometry:0.17.1")
   api("us.ihmc:ihmc-commons:0.30.5")
   api("us.ihmc:ihmc-jmonkey-engine-toolkit:0.19.7")
   api("us.ihmc:ihmc-robot-description:0.21.3")
   api("us.ihmc:ihmc-graphics-description:0.19.4")
   api("us.ihmc:ihmc-swing-plotting:0.19.3")
   api("us.ihmc:scs2-definition:0.5.0")
   api(ihmc.sourceSetProject("utilities"))
}

testDependencies {
   api("us.ihmc:ihmc-commons-testing:0.30.5")
}

utilitiesDependencies {
   api("us.ihmc:euclid:0.17.1")
   api("us.ihmc:ihmc-yovariables:0.9.11")
}
