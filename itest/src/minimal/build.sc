import $file.plugins
import mill._
import mill.scalalib._
import os._
import coursier.maven.MavenRepository

import com.ofenbeck.mill.docker._

/** This is a minimal example of a build.sc file that uses the mill-docker plugin. It uses the JibImage.RegistryImage
  * and JibImage.DockerDaemonImage to build a minimal image. The image is based on alpine:latest and is pushed to the
  * local docker demon as ofenbeck/mill-docker/minimal.
  *
  * Note that the image will still contain a lib layer with the scala-library
  *
  * This image cannot be run as it does not contain a main class/entrypoint and only demonstrates the minimal runnable
  * code
  */

object project extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.RegistryImage("alpine:latest")
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/minimal")
  }
}

def check() = T.command {
  project.docker.buildImage()
}
