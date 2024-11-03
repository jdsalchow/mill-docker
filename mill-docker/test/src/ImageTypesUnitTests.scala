package com.ofenbeck.mill.docker

import mill.testkit.{TestBaseModule, UnitTester}
import utest._
import com.ofenbeck.mill.docker.DockerJibModule
import mill.scalalib.ScalaModule
import mill.T

import com.ofenbeck.mill.docker._
import scala.tools.nsc.doc.html.HtmlTags.U

object ImageTypesUnitTests extends TestSuite {

  val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
  def tests: Tests = Tests {
    test("registry2demon") {

      object registry2demon extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object docker extends DockerConfig {
          override def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
          override def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/registry2demon")
        }
      }

      UnitTester(registry2demon, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(registry2demon.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("registry2tar") {

      object registry2tar extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object docker extends DockerConfig {
          def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/registry2tar")
        }
      }

      UnitTester(registry2tar, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(registry2tar.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("registry2registry") {

      object registry2registry extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object docker extends DockerConfig {
          def sourceImage = JibImage.RegistryImage("gcr.io/distroless/java:latest")
          def targetImage =
            JibImage.RegistryImage("ofenbeck/registry2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
        }
      }

      UnitTester(registry2registry, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(registry2registry.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("demon2demon") {
      object demon2demon extends TestBaseModule with ScalaModule with DockerJibModule {
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

      UnitTester(demon2demon, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(demon2demon.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("demon2tar") {
      object demon2tar extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object docker extends DockerConfig {
          def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/demon2tar")
        }
      }

      UnitTester(demon2tar, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(demon2tar.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("demon2registry") {
      object demon2registry extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object docker extends DockerConfig {
          def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
          def targetImage =
            JibImage.RegistryImage("ofenbeck/demon2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
        }
      }

      UnitTester(demon2registry, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(demon2registry.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }

    }

    test("tar2demon") {

      object tar2demon extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object createSourceTar extends DockerConfig {
          def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/demon2tar")
        }

        object docker extends DockerConfig {
          def sourceImage = JibImage.SourceTarFile(tar2demon.createSourceTar.buildImage().path.get)
          def targetImage = JibImage.DockerDaemonImage("ofenbeck/mill-docker/tar2demon")
        }
      }

      UnitTester(tar2demon, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(tar2demon.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("tar2tar") {

      object tar2tar extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"

        object createSourceTar extends DockerConfig {
          def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/demon2tar")
        }
        object docker extends DockerConfig {
          def sourceImage = JibImage.SourceTarFile(createSourceTar.buildImage().path.get)
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/tar2demon")
        }
      }

      UnitTester(tar2tar, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(tar2tar.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }

    test("tar2registry") {

      object tar2registry extends TestBaseModule with ScalaModule with DockerJibModule {
        def scalaVersion = "3.3.3"


        object createSourceTar extends DockerConfig {
          def sourceImage = JibImage.DockerDaemonImage("gcr.io/distroless/java:latest")
          def targetImage = JibImage.TargetTarFile("ofenbeck/mill-docker/demon2tar")
        }
        object docker extends DockerConfig {
          def sourceImage = JibImage.SourceTarFile(createSourceTar.buildImage().path.get)
          def targetImage =
            JibImage.RegistryImage("ofenbeck/tar2registry", Some(("DOCKER_USERNAME", "DOCKER_PASSWORD")))
        }
      }

      UnitTester(tar2registry, resourceFolder / "image-types-project").scoped { eval =>
        val Right(result) = eval(tar2registry.docker.buildImage)
        assert(result.value.imageDigest.contains("sha256"))
      }
    }
  }
}
