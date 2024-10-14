package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api.{Containerizer, JavaContainerBuilder}
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api._
import mill.scalalib.JavaModule

import scala.jdk.CollectionConverters._
import com.google.cloud.tools.jib.api.MainClassFinder
import java.time.Instant
import com.google.cloud.tools.jib.api.JibContainerBuilder

object MDJavaBuild {

  def useCurrentTimestamp(useCurrentTimestamp: Boolean): Instant =
    if (useCurrentTimestamp) Instant.now() else Instant.EPOCH

  private def isSnapshotDependency(millpath: mill.PathRef) = millpath.path.last.endsWith("-SNAPSHOT.jar")

  def javaBuild(
      dockerSettings: DockerSettings,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
  ): JibContainerBuilder = {

    val javaBuilder = buildSettings.sourceImage match {
      case JibImage.RegistryImage(qualifiedName, credentialsEnvironment) =>
        JavaContainerBuilder.from(RegistryImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.DockerDaemonImage(qualifiedName, useFallBack, fallBackEnvCredentials) =>
        JavaContainerBuilder.from(DockerDaemonImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.SourceTarFile(path) =>
        JavaContainerBuilder.from(TarImage.at(path.path.wrapped))
    }

    // Create all the layers for the container
    val (upstreamClassSnapShot, upstreamClass) =
      buildSettings.upstreamAssemblyClasspath.partition(isSnapshotDependency(_))

    javaBuilder.addDependencies(upstreamClass.map(x => x.path.wrapped).toList.asJava)
    javaBuilder.addSnapshotDependencies(upstreamClassSnapShot.map(_.path.wrapped).toList.asJava)

    buildSettings.resourcesPaths
      .map(_.path.wrapped)
      .foreach(path => javaBuilder.addResources(path)) // TODO: double check this can be called multiple times

    javaBuilder.addClasses(buildSettings.compiledClasses.path.wrapped)

    if (buildSettings.mainClass.isDefined) {
      if (buildSettings.autoDetectMainClass) {
        logger.error("Both mainClass and autoDetectMainClass are set. AutoDetectMainClass will be ignored")
      }
      javaBuilder.setMainClass(buildSettings.mainClass.get)
    } else {
      val classfiles =
        os.walk(buildSettings.compiledClasses.path).filter(file => file.toIO.isFile()).map(x => x.wrapped).toList.asJava
      val mainfound = MainClassFinder.find(classfiles, MDLogging.getEventLogger(logger))
      logger.info(s"Autodetect Main class: Main class found = ${mainfound.getFoundMainClass()}")
      javaBuilder.setMainClass(mainfound.getFoundMainClass())
    }

    javaBuilder.addJvmFlags(dockerSettings.jvmOptions.asJava)
    val containerBuilder = javaBuilder.toContainerBuilder()

    containerBuilder.setEnvironment(dockerSettings.envVars.asJava)
    if (!dockerSettings.platforms.isEmpty) {
      containerBuilder.setPlatforms(
        dockerSettings.platforms
          .map(p => new com.google.cloud.tools.jib.api.buildplan.Platform(p.architecture, p.os))
          .asJava,
      )
    }
    containerBuilder.setLabels(dockerSettings.labels.asJava)
    containerBuilder.setUser(dockerSettings.user.orNull)
    containerBuilder.setProgramArguments(dockerSettings.args.asJava)
    containerBuilder.setFormat(dockerSettings.internalImageFormat match {
      case JibImageFormat.Docker => ImageFormat.Docker
      case JibImageFormat.OCI    => ImageFormat.OCI
    })
    import com.google.cloud.tools.jib.api.buildplan.Port._
    val ports: Set[Port] =
      dockerSettings.exposedPorts.map(p => tcp(p)).toSet ++ dockerSettings.exposedUdpPorts.map(p => udp(p)).toSet
    containerBuilder.setExposedPorts(ports.asJava)
    containerBuilder.setCreationTime(this.useCurrentTimestamp(buildSettings.useCurrentTimestamp))
    dockerSettings.entrypoint.foreach(entrypoint => containerBuilder.setEntrypoint(dockerSettings.entrypoint.asJava))
    containerBuilder
  }

}
