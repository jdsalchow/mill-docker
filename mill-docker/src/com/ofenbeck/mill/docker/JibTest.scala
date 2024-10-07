package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties

object JibTest extends App {

    

    def build() =  {
      val baseImage = ImageReference.parse("ghcr.io/graalvm/jdk:latest") // imageFactory()
      val regImage  = RegistryImage.named(baseImage)
      val jib = Jib
        .from(regImage)
        .containerize(
          Containerizer.to(
/*            RegistryImage
              .named("ofenbeck/hello-from-jib")
              .addCredential("ofenbeck", "")
*/
            DockerDaemonImage
              .named("ofenbeck/hello-from-jib")
          )
        );

          println("hello")
    }
    
    println("debug")
    val settings = DockerSettings("ghcr.io/graalvm/jdk:latest","ofenbeck/local:latest")
    JibBuilds.buildToLocalDockerDemon(settings)
}
