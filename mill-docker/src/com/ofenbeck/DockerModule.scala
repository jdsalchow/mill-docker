package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties

import mill.scalalib.JavaModule
import os.Shellable.IterableShellable
import com.google.cloud.tools._

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Optional
import java.util.function._
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption
import scala.language.postfixOps

import scala.collection.immutable._

import mill._
import mill.scalalib.ScalaModule

trait DockerModule extends Module { outer: JavaModule =>

  trait DockerConfig extends mill.Module {

    def baseImage: T[String] = "gcr.io/distroless/java:latest"

    def build() = T.task {
      val baseImage = ImageReference.parse("ghcr.io/graalvm/jdk:latest") // imageFactory()
      val regImage  = RegistryImage.named(baseImage)
      val jib = Jib
        .from(regImage)
        .containerize(
          Containerizer.to(
            RegistryImage
              .named("ofenbeck/hello-from-jib")
              .addCredential("ofenbeck", "")
          )
        );

    }

    def imageFactory(
        imageReference: ImageReference,
        credentialsEnv: (String, String),
        credHelper: Option[String],
        credsForHost: String => Option[(String, String)],
        logger: LogEvent => Unit
    ): RegistryImage = {
      val image                      = RegistryImage.named(imageReference)
      val loggerJava                 = new Consumer[LogEvent] { def accept(e: LogEvent): Unit = logger(e) }
      val factory                    = CredentialRetrieverFactory.forImage(imageReference, loggerJava)
      val (usernameEnv, passwordEnv) = credentialsEnv

      // TODO - env credentials
      image.addCredentialRetriever(factory.dockerConfig())
      image.addCredentialRetriever(factory.wellKnownCredentialHelpers())
      image.addCredentialRetriever(factory.googleApplicationDefaultCredentials())

      credHelper.foreach { helper =>
        image.addCredentialRetriever(factory.dockerCredentialHelper(helper))
      }

      image
    }
  }
}

