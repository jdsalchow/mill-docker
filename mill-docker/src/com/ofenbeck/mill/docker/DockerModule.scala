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
import mill.api.Ctx

trait DockerModule extends Module { outer: JavaModule =>

  trait DockerConfig extends mill.Module {

    def baseImage: T[String] = "gcr.io/distroless/java:latest"

    def pullBaseImage: T[Boolean] = T(baseImage().endsWith(":latest"))

    def targetImage: T[String] = T(outer.artifactName())

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

    /** Commands to add as RUN instructions.
      *
      * See also the Docker docs on [[https://docs.docker.com/engine/reference/builder/#run RUN]] for more information.
      */
    def run: T[Seq[String]] = Seq.empty[String]

    /** Any applicable string to the USER instruction.
      *
      * An empty string will be ignored and will result in USER not being specified. See also the Docker docs on
      * [[https://docs.docker.com/engine/reference/builder/#user USER]] for more information.
      */
    def user: T[Option[String]] = None

    def platforms: T[Set[com.ofenbeck.mill.docker.Platform]] = Set.empty[com.ofenbeck.mill.docker.Platform]

    def internalImageFormat: T[com.ofenbeck.mill.docker.JibImageFormat] = T {
      val x: JibImageFormat = com.ofenbeck.mill.docker.JibImageFormat.Docker
      x
    }

    def entrypoint: T[Seq[String]] = Seq.empty[String]

    def args: T[Seq[String]] = Seq.empty[String]


    def baseImageCredentialEnv: T[(String, String)] = ("", "")
    def targetImageCredentialEnv: T[(String, String)] = ("", "")

    def dockerContainerConfig: T[DockerSettings] = T {
      DockerSettings(
        baseImage = baseImage(),
        targetImage = targetImage(),
        tags = tags(),
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
        baseImageCredentialEnv = baseImageCredentialEnv(),
        targetImageCredentialEnv = targetImageCredentialEnv(),
        setAllowInsecureRegistries = false,
        useCurrentTimestamp = true,
        upstreamAssemblyClasspath = outer.upstreamAssemblyClasspath().toList,
        resourcesPaths = outer.resources(),
        compiledClasses = outer.compile().classes,
        mainClass = None,
        autoDetectMainClass = true,
        pullBaseImage = pullBaseImage(),
      )
    }

    def runit() = T.command {
      val logger = T.ctx().log

      JibJavaBuild.javaBuild(
        buildSettings = buildSettings(),
        dockerSettings = dockerContainerConfig(),
        logger = logger
      )
    }

    /*
    def buildToLocalDockerDemon() = T.task {
      val conf = finalConfig()
      JibBuilds.buildToLocalDockerDemon(conf = conf, T.ctx().log)
    }

    def buildToLocalTarImage() = T.task {
      val conf = finalConfig()
      T.ctx().log.info("info test")
      JibBuilds.buildToLocalTarImage(conf)
    }
     */
    private def isSnapshotDependency(millpath: mill.PathRef) = millpath.path.last.endsWith("-SNAPSHOT.jar")

    def testme() = T.task {
      val conf = dockerContainerConfig()

      val logger = T.ctx().log

      val upstreamAssemblyClasspath = outer.upstreamAssemblyClasspath()
      // val internalDependencies: Set[PathRef] = outer.localRunClasspath().toSet -- outer.resources().toSet

      // val internalDependencies = Agg(outer.compile().classes)

      val dockerCachedImage                      = DockerDaemonImage.named(ImageReference.parse(conf.baseImage))
      val javaBuilder                            = JavaContainerBuilder.from(dockerCachedImage)
      val (upstreamClassSnapShot, upstreamClass) = upstreamAssemblyClasspath.partition(isSnapshotDependency(_))
      // val localClasspath =
      javaBuilder.addDependencies(upstreamClass.map(x => x.path.wrapped).toList.asJava)
      javaBuilder.addSnapshotDependencies(upstreamClassSnapShot.map(_.path.wrapped).toList.asJava)

      outer
        .resources()
        .map(_.path.wrapped)
        .foreach(path => javaBuilder.addResources(path)) // double check this can be called multiple times
      // javaBuilder.addDependencies()
      val internal = outer.compile().classes // internalDependencies

      // (internalDependencies.filter(x => x.path.toIO.isFile).map( x => x.path.wrapped).toList)
      logger.info("internal Dependenicies:")
      // internal.map(x => logger.info(x.toString()))
      logger.info(internal.path.toString())
      logger.info("----------------------")
      // javaBuilder.addProjectDependencies(internalDependencies.map( x => x.path.wrapped).toList.asJava)
      javaBuilder.addClasses(internal.path.wrapped)

      val classfiles = os.walk(internal.path).filter(file => file.toIO.isFile()).map(x => x.wrapped).toList.asJava
      // internal.map( x => x.path.wrapped).asJava)
      // javaBuilder.setMainClass("com.ofenbeck.Demo")
      val mainfound = MainClassFinder.find(classfiles, JibLogging.getEventLogger(logger))
      logger.info(s"Main class found = ${mainfound.getFoundMainClass()}")

      javaBuilder.setMainClass(mainfound.getFoundMainClass())
      val jibBuilder = javaBuilder.toContainerBuilder()

      val container = jibBuilder.containerize(
        Containerizer
          .to(DockerDaemonImage.named(conf.targetImage))
          .addEventHandler(JibLogging.getLogger(logger)),
      )

      /*JibBuilds.buildJavaBuild(
        conf = conf,
        outer = outer,
        //upstreamAssemblyClasspath = outer.upstreamAssemblyClasspath(),
        //internalDependencies = outer.localClasspath(),
        logger = T.ctx().log
      )*/
    }

  }
}
