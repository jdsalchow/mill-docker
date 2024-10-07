package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties

object JibBuilds {


    def buildToLocalDockerDemon(conf: DockerSettings) = {
      println("building local image")
      val baseImage = RegistryImage.named(ImageReference.parse(conf.baseImage))
      val targetImage = RegistryImage.named(ImageReference.parse(conf.targetImage))
      val jib = Jib
        .from(baseImage)
        .containerize(
          Containerizer.to(DockerDaemonImage.named(conf.targetImage))
        )
    }
}
