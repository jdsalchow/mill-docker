import mill._
import mill.scalalib._

import coursier.maven.MavenRepository
import contrib.docker.DockerModule
import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import scala.io.Source

import $ivy.`com.ofenbeck::mill-docker:fe13e4d`

object project extends ScalaModule with com.ofenbeck.mill.docker.DockerModule {
  def scalaVersion = "2.13.12"

  val sonatypeReleases = Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
  )

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ sonatypeReleases
  }
  val jibCore = "0.27.1"

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.google.cloud.tools:jib-core:$jibCore",
    // ivy"com.ofenbeck::mill-docker:0.0.1-SNAPSHOT", //TODO - PR mill - no Snapshot postfix on jar
    ivy"org.scrupal:chill-java:0.7.0-SNAPSHOT"
  )
  object docker extends DockerConfig
}

def check() = T.command {
  println("blub")
  project.docker.buildToLocalDockerDemon()
}
