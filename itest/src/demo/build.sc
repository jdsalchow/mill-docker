import $file.plugins
import $ivy.`com.google.cloud.tools:jib-core:0.27.1`
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with com.ofenbeck.mill.docker.DockerModule {
  def scalaVersion = "2.13.12"

  object docker extends DockerConfig
}

def check() = T.command {
  project.docker.build()
}

