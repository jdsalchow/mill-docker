package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api.{Containerizer, JavaContainerBuilder}
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api._
import mill.scalalib.JavaModule

import scala.jdk.CollectionConverters._
import com.google.cloud.tools.jib.api._
import java.time.Instant
import com.google.cloud.tools.jib.api.JibContainerBuilder
import os.group.set
import coursier.core.shaded.sourcecode.File

object MDBuild {

  def javaBuild(
      dockerSettings: DockerSettings,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
      javaContainerBuilderHook: Option[JavaContainerBuilder => JavaContainerBuilder],
      jibContainerBuilderHook: Option[
        (JibContainerBuilder, Vector[FileEntriesLayer], Vector[String]) => JibContainerBuilder,
      ],
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
      buildSettings.upstreamAssemblyClasspath.partition(MDShared.isSnapshotDependency(_))

    buildSettings.layerOrder.foreach {
      case DefaultLayers.Dependencies =>
        javaBuilder.addDependencies(upstreamClass.map(x => x.path.wrapped).toList.asJava)
      case DefaultLayers.SnapshotDependencies =>
        javaBuilder.addSnapshotDependencies(upstreamClassSnapShot.map(_.path.wrapped).toList.asJava)
      case DefaultLayers.Resources =>
        buildSettings.resourcesPaths
          .map(_.path.wrapped)
          .foreach(path => javaBuilder.addResources(path)) // TODO: double check this can be called multiple times
      case DefaultLayers.Classes =>
        javaBuilder.addClasses(buildSettings.compiledClasses.path.wrapped)
      case DefaultLayers.ExtraFiles => // TODO: ExtraFiles
    }
    setMainClass(buildSettings, javaBuilder, logger)
    javaBuilder.addJvmFlags(dockerSettings.jvmOptions.asJava)

    val javaBuilderPostHook = javaContainerBuilderHook.map(hook => hook(javaBuilder)).getOrElse(javaBuilder)
    val containerBuilder    = javaBuilderPostHook.toContainerBuilder()

    val jibContainerBuilderPostHook = jibContainerBuilderHook
      .map(hook => customizeLayers(containerBuilder, buildSettings, logger, hook))
      .getOrElse(containerBuilder)
    
    setContainerParams(dockerSettings, buildSettings, logger, jibContainerBuilderPostHook)
  }

  def customizeLayers(
      containerBuilder: JibContainerBuilder,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
      hook: (JibContainerBuilder, Vector[FileEntriesLayer], Vector[String]) => JibContainerBuilder,
  ): JibContainerBuilder = {

    val containerBuildPlan = containerBuilder.toContainerBuildPlan()
    val jiblayers          = containerBuildPlan.getLayers().asScala.toVector

    val jibBuilder = buildSettings.sourceImage match {
      case JibImage.RegistryImage(qualifiedName, credentialsEnvironment) =>
        Jib.from(RegistryImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.DockerDaemonImage(qualifiedName, useFallBack, fallBackEnvCredentials) =>
        Jib.from(DockerDaemonImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.SourceTarFile(path) =>
        Jib.from(TarImage.at(path.path.wrapped))
    }

    val entrypoints = containerBuildPlan.getEntrypoint()
    entrypoints.asScala.foreach(entrypoint => logger.info(s"Entrypoint: $entrypoint"))
    jiblayers.foreach(layer =>
      layer match {
        case fl: FileEntriesLayer =>
          logger.info(s"Layer:  ${fl.getName()}")
          val filelist: List[FileEntry] = fl.getEntries().asScala.toList
          filelist.map(entry =>
            logger.info(s"Layer: ${fl.getName()} Entry: ${entry.getSourceFile()}, ${entry.getExtractionPath()}"),
          )
        case _: LayerObject => logger.error("LayerObject in customizeLayers not supported")
      },
    )

    val fileEntriesLayer = jiblayers.collect { case fl: FileEntriesLayer => fl }
    hook(jibBuilder, fileEntriesLayer, entrypoints.asScala.toVector)
  }

  def setMainClass(buildSettings: BuildSettings, javaBuilder: JavaContainerBuilder, logger: mill.api.Logger): Unit =
    if (buildSettings.mainClass.isDefined) {
      if (buildSettings.autoDetectMainClass) {
        logger.error("Both mainClass and autoDetectMainClass are set. AutoDetectMainClass will be ignored")
      }
      javaBuilder.setMainClass(buildSettings.mainClass.get)
    } else {
      val classfiles =
        os.walk(buildSettings.compiledClasses.path).filter(file => file.toIO.isFile()).map(x => x.wrapped).toList.asJava
      val mainfound = MainClassFinder.find(classfiles, MDLogging.getEventLogger(logger))
      mainfound.getType() match {
        case MainClassFinder.Result.Type.MAIN_CLASS_FOUND =>
          logger.info(s"Main class found = ${mainfound.getFoundMainClass()}")
          javaBuilder.setMainClass(mainfound.getFoundMainClass())
        case MainClassFinder.Result.Type.MAIN_CLASS_NOT_FOUND =>
          logger.error("No main class found")
        case MainClassFinder.Result.Type.MULTIPLE_MAIN_CLASSES =>
          logger.info(
            s"Multiple main classes found - setting the first one `${mainfound.getFoundMainClasses().get(0)}`",
          )
          logger.info("Set MainClass in DockerSettings if you want to select a specific one")
          javaBuilder.setMainClass(mainfound.getFoundMainClasses().get(0))
      }
    }

  def setContainerParams(
      dockerSettings: DockerSettings,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
      containerBuilder: JibContainerBuilder,
  ): JibContainerBuilder = {

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
    containerBuilder.setCreationTime(MDShared.useCurrentTimestamp(buildSettings.useCurrentTimestamp))
    dockerSettings.entrypoint.foreach(entrypoint => containerBuilder.setEntrypoint(dockerSettings.entrypoint.asJava))
    containerBuilder
  }
}
/*
  def buildLayeredContainer(
      dockerSettings: DockerSettings,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
  ): JibContainerBuilder = {

    val jibBuilder = buildSettings.sourceImage match {
      case JibImage.RegistryImage(qualifiedName, credentialsEnvironment) =>
        Jib.from(RegistryImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.DockerDaemonImage(qualifiedName, useFallBack, fallBackEnvCredentials) =>
        Jib.from(DockerDaemonImage.named(ImageReference.parse(qualifiedName)))
      case JibImage.SourceTarFile(path) =>
        Jib.from(TarImage.at(path.path.wrapped))
    }
    // TODO: Layers
    val layers = MDLayers.createDefaultLayers(buildSettings)

    val jibLayers = layers.map { mdlayer =>
      val layerBuilder = FileEntriesLayer.builder()
      mdlayer.entries.foreach { entry =>
        logger.info(s"Adding entry ${entry.sourceFile.path} to layer ${entry.pathInContainer}")
        layerBuilder.addEntry(
          entry.sourceFile.path.wrapped,
          AbsoluteUnixPath.get((entry.pathInContainer.wrapped + "/" + entry.sourceFile.path).toString()),
          entry.permissions,
          entry.modificationTime,
          entry.ownership,
        )
      }
      layerBuilder.build()
    }
    jibBuilder.setFileEntriesLayers(jibLayers.asJava)
    setContainerParams(dockerSettings, buildSettings, logger, jibBuilder)

  }
 */
