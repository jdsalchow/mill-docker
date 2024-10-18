import java.nio.file.Path
import $file.plugins
import mill._
import mill.scalalib._
import os._
import coursier.maven.MavenRepository

import com.ofenbeck.mill.docker._

object project extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  val sonatypeReleases = Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
  )

  def repositoriesTask = T.task {
    super.repositoriesTask() ++ sonatypeReleases
  }

  override def resources: T[Seq[PathRef]] = T.sources {
    Seq(
      millSourcePath / "resources",
      millSourcePath / "additional" / "resources",
    ).map(PathRef(_))
  }

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.lihaoyi::scalatags:0.13.1",
    ivy"com.lihaoyi::os-lib:0.11.2",
    ivy"org.scrupal:chill-java:0.7.0-SNAPSHOT", // random snapshot dependency
  )

  override def compileIvyDeps = Agg(
    ivy"com.softwaremill.macwire::macros:2.6.4",
  )

  override def unmanagedClasspath= T {
    super.unmanagedClasspath() ++
      Agg.from(os.list(millSourcePath / "unmanaged")).map(PathRef(_))
    
  }
  


  object docker extends DockerConfig {
    def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/javabuildsettings")
  }
}

def check() = T.command {
  project.docker.buildImage()
}
