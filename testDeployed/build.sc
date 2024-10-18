import mill._
import mill.scalalib._

import coursier.maven.MavenRepository
import scala.io.Source

import $ivy.`com.ofenbeck::mill-docker:0.0.2-SNAPSHOT`
import com.ofenbeck.mill.docker._
/*
object project extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  val sonatypeReleases = Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
  )

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ sonatypeReleases
  }
  val jibCore = "0.27.1"

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.google.cloud.tools:jib-core:$jibCore",
    // ivy"com.ofenbeck::mill-docker:0.0.1-SNAPSHOT", //TODO - PR mill - no Snapshot postfix on jar
    ivy"org.scrupal:chill-java:0.7.0-SNAPSHOT",
  )
  object docker extends DockerConfig
}

def check() = T.command {
  println("blub")
  project.docker.buildImage()
}
*/


object minimal extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"
  
  object docker extends DockerConfig {
    def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/minimal")
  }
}

def check() = T.command {
  minimal.docker.buildImage()
}