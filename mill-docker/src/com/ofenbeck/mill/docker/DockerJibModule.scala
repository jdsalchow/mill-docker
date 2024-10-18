package com.ofenbeck.mill.docker

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
import mill.api.Ctx

import com.ofenbeck.mill.{docker => md}
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.RegistryImage

trait DockerJibModule extends Module { outer: JavaModule =>

  trait DockerConfig extends mill.Module {

    def labels: T[Map[String, String]] = Map.empty[String, String]

    def tags: T[Seq[String]] = T(List(outer.artifactName()))

    /** JVM runtime options. Each item of the Seq should consist of an option and its desired value, like
      * {{{
      * def jvmOptions = Seq("-Xmx1024M", "-agentlib:jdwp=transport=dt_socket,server=y,address=8000", …)
      * }}}
      * For a full list of options consult the official documentation at
      * [[https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#overview-of-java-options]]
      */
    def jvmOptions: T[Seq[String]] = Seq.empty[String]

    /** TCP Ports the container will listen to at runtime.
      *
      * See also the Docker docs on [[https://docs.docker.com/engine/reference/builder/#expose ports]] for more
      * information.
      */
    def exposedPorts: T[Seq[Int]] = Seq.empty[Int]

    /** UDP Ports the container will listen to at runtime.
      *
      * See also the Docker docs on [[https://docs.docker.com/engine/reference/builder/#expose ports]] for more
      * information.
      */
    def exposedUdpPorts: T[Seq[Int]] = Seq.empty[Int]

    /** The names of mount points.
      *
      * See also the Docker docs on [[https://docs.docker.com/engine/reference/builder/#volume volumes]] for more
      * information.
      */
    def volumes: T[Seq[String]] = Seq.empty[String]

    /** Environment variables to be set in the container.
      *
      * See also the Docker docs on [[https://docs.docker.com/engine/reference/builder/#env ENV]] for more information.
      */
    def envVars: T[Map[String, String]] = Map.empty[String, String]

    /** Any applicable string to the USER instruction.
      *
      * An empty string will be ignored and will result in USER not being specified. See also the Docker docs on
      * [[https://docs.docker.com/engine/reference/builder/#user USER]] for more information.
      */
    def user: T[Option[String]] = None

    def platforms: T[Set[md.Platform]] = Set.empty[md.Platform]

    def internalImageFormat: T[md.JibImageFormat] = T {
      md.JibImageFormat.Docker: JibImageFormat
    }

    def entrypoint: T[Seq[String]] = Seq.empty[String]

    def args: T[Seq[String]] = Seq.empty[String]

    def sourceImage: T[md.JibSourceImage]

    def targetImage: T[md.ImageReference]


    def dockerContainerConfig: T[DockerSettings] = T {
      DockerSettings(
        labels = labels(),
        jvmOptions = jvmOptions(),
        exposedPorts = exposedPorts(),
        exposedUdpPorts = exposedUdpPorts(),
        envVars = envVars(),
        user = user(),
        platforms = platforms(),
        internalImageFormat = internalImageFormat(),
        entrypoint = entrypoint(),
        args = args(),
      )
    }

    def buildSettings: T[BuildSettings] = T {
      BuildSettings(
        sourceImage = sourceImage(),
        targetImage = targetImage(),
        setAllowInsecureRegistries = false,
        useCurrentTimestamp = true,
        upstreamAssemblyClasspath = outer.upstreamAssemblyClasspath().toList,
        resourcesPaths = outer.resources(),
        compiledClasses = outer.compile().classes,
        mainClass = None,
        autoDetectMainClass = true,
        tags = tags(),
      )
    }

    def jibContainerBuilderHook
        : T[Option[(JibContainerBuilder, Vector[FileEntriesLayer], Vector[String]) => JibContainerBuilder]] = None
    
    def javaContainerBuilderHook: T[Option[JavaContainerBuilder => JavaContainerBuilder]] = None

    def buildImage() = T.command {

      val logger     = T.ctx().log
      val dockerConf = dockerContainerConfig()
      val buildConf  = buildSettings()

      val jibContainerBuilder = MDBuild.javaBuild(
        buildSettings = buildConf,
        dockerSettings = dockerConf,
        logger = logger,
        javaContainerBuilderHook = javaContainerBuilderHook(),
        jibContainerBuilderHook = jibContainerBuilderHook(),
      )

      val containerizer = buildConf.targetImage match {
        case md.JibImage.DockerDaemonImage(qualifiedName, _, _) =>
          Containerizer.to(DockerDaemonImage.named(qualifiedName))
        case md.JibImage.RegistryImage(qualifiedName, credentialsEnvironment) =>
          Containerizer.to(RegistryImage.named(qualifiedName))
        case md.JibImage.TargetTarFile(qualifiedName, filename) =>
          Containerizer.to(TarImage.at((T.dest / filename).wrapped).named(qualifiedName))
      }

      val containerizerWithLogs = containerizer.addEventHandler(MDLogging.getLogger(logger))

      val containerizerWithTags = buildConf.tags.foldRight(containerizerWithLogs) { (tag, c) =>
        c.withAdditionalTag(tag)
      }
      val containerizerWithToolSet = containerizerWithTags
        .setAllowInsecureRegistries(buildConf.setAllowInsecureRegistries)
        .setToolName(MDShared.toolName)
      // TODO: check how we could combine jib and mill caching
      val container = jibContainerBuilder.containerize(containerizerWithToolSet)
    }
  }
}