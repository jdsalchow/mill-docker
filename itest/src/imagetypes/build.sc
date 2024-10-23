//tag::replace[]
import $file.plugins
//BE AWARE:  if used outside the test suite this needs to be:
//import $ivy.`com.ofenbeck::mill-docker:<VERSION>`
//end::replace[]
import mill._
import mill.scalalib._

import com.ofenbeck.mill.docker._

/* ================================= From a remote Registry ================================= */
// Note that this will always fetch the image from the registry - even if it is available locally
// This is useful for fetching "latest" images - otherwise you might want to use the local demon
// with a registry fallback

/** From a registry to your local (hopefully running) docker daemon
  */
object registry2demon extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/registry2demon")
  }
}

/** From a registry to a tar file - e.g useful for transfer to secured machines
  */
object registry2tar extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/registry2tar")
  }
}

/** From a registry to another registry Usually you need to login to the target registry - the second parameter is a
  * tuple of the name of environment variables to be used for the username and password - more authentication methods
  * will be added in the near future (there is already jib support for some alternatives)
  */
object registry2registry extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.RegistryImage("ofenbeck/registry2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
  }
}
// tag::snippet[]
/* ================================= From a your local Docker Demon ================================= */

/** From your local demon to your local demon. Note the Fallback settings (by default true). This will fetch from a
  * registry if the local demon cannot serve the image.
  */
object demon2demon extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.DockerDaemonImage(
      "gcr.io/distroless/java:latest",
      useFallBack = true,
      fallBackEnvCredentials = Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")),
    )
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/demon2demon")
  }
}
//end::snippet[]
/** From your local demon to a tar file
  */
object demon2tar extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/demon2tar")
  }
}

/** From your local demon to a registry
  */
object demon2registry extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  object docker extends DockerConfig {
    def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
    def targetImage = JibImage.RegistryImage("ofenbeck/demon2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
  }
}


/* ================================= From a local Tar File ================================= */

// The tar use case can be usefull for debugging and transfering images to secured machines 

object tar2demon extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  override def moduleDeps = Seq(demon2tar)
  object docker extends DockerConfig {
    def sourceImage = JibImage.SourceTarFile(demon2tar.docker.buildImage().path.get)
    def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/tar2demon")
  }
}


object tar2tar extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  override def moduleDeps = Seq(demon2tar)
  object docker extends DockerConfig {
    def sourceImage = JibImage.SourceTarFile(demon2tar.docker.buildImage().path.get)
    def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/tar2demon")
  }
}

object tar2registry extends ScalaModule with DockerJibModule {
  def scalaVersion = "3.3.3"

  override def moduleDeps = Seq(demon2tar)
  object docker extends DockerConfig {
    def sourceImage = JibImage.SourceTarFile(demon2tar.docker.buildImage().path.get)
    def targetImage = JibImage.RegistryImage("ofenbeck/tar2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
  }
}