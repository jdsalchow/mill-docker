import $file.plugins
import mill._
import mill.scalalib._
import os._
import coursier.maven.MavenRepository

import com.ofenbeck.mill.docker._

object project extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"
  
  object docker extends DockerConfig {
    def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/minimal")
  }
}

def check() = T.command {
  project.docker.buildImage()
}