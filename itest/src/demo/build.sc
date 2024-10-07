import $file.plugins
import mill._
import mill.scalalib._
import os._


object project extends ScalaModule with com.ofenbeck.mill.docker.DockerModule {
  def scalaVersion = "2.13.12"

  object docker extends DockerConfig
}

def check() = T.command {
  project.docker.buildToLocalDockerDemon()
}

