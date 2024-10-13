package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api.{Containerizer, JavaContainerBuilder}
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.Jib
import mill.scalalib.JavaModule

import scala.jdk.CollectionConverters._
import com.google.cloud.tools.jib.api.MainClassFinder
import java.time.Instant

object JibJavaBuild {

  def useCurrentTimestamp(useCurrentTimestamp: Boolean): Instant =
    if (useCurrentTimestamp) Instant.now() else Instant.EPOCH

  private def isSnapshotDependency(millpath: mill.PathRef) = millpath.path.last.endsWith("-SNAPSHOT.jar")

  def javaBuild(
      dockerSettings: DockerSettings,
      buildSettings: BuildSettings,
      logger: mill.api.Logger,
  )(containerizer: Containerizer) = {

    val baseImageReference   = ImageReference.parse(dockerSettings.baseImage)
    val targetImageReference = ImageReference.parse(dockerSettings.targetImage)

    val baseImage = JibImageWithCredentials.imageFactory(
      baseImageReference,
      buildSettings.baseImageCredentialEnv,
      logger,
    )
    val targetImage = JibImageWithCredentials.imageFactory(
      targetImageReference,
      buildSettings.targetImageCredentialEnv,
      logger,
    )

    val containerizerWithTags = dockerSettings.tags.foldRight(containerizer) { (tag, c) =>
      c.withAdditionalTag(tag)
    }

    val containerizerWithToolSet = containerizerWithTags
      .setAllowInsecureRegistries(buildSettings.setAllowInsecureRegistries)
      .setToolName(JibShared.toolName)
    // TODO: check how we could combine jib and mill caching

    val javaBuilder = JavaContainerBuilder
      .from(baseImage)

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
      val mainfound = MainClassFinder.find(classfiles, JibLogging.getEventLogger(logger))
      logger.info(s"Autodetect Main class: Main class found = ${mainfound.getFoundMainClass()}")
      javaBuilder.setMainClass(mainfound.getFoundMainClass())
    }

    javaBuilder.addJvmFlags(dockerSettings.jvmOptions.asJava)
    val javaBuider = javaBuilder.toContainerBuilder()

    javaBuider.setEnvironment(dockerSettings.envVars.asJava)
    if (!dockerSettings.platforms.isEmpty) {
      javaBuider.setPlatforms(
        dockerSettings.platforms
          .map(p => new com.google.cloud.tools.jib.api.buildplan.Platform(p.architecture, p.os))
          .asJava,
      )
    }
    javaBuider.setLabels(dockerSettings.labels.asJava)
    javaBuider.setUser(dockerSettings.user.orNull)
    javaBuider.setProgramArguments(dockerSettings.args.asJava)
    javaBuider.setFormat(dockerSettings.internalImageFormat match {
      case JibImageFormat.Docker => ImageFormat.Docker
      case JibImageFormat.OCI    => ImageFormat.OCI
    })
    import com.google.cloud.tools.jib.api.buildplan.Port._
    val ports: Set[Port] =
      dockerSettings.exposedPorts.map(p => tcp(p)).toSet ++ dockerSettings.exposedUdpPorts.map(p => udp(p)).toSet
    javaBuider.setExposedPorts(ports.asJava)
    javaBuider.setCreationTime(this.useCurrentTimestamp(buildSettings.useCurrentTimestamp))
    dockerSettings.entrypoint.foreach(entrypoint => javaBuider.setEntrypoint(dockerSettings.entrypoint.asJava))
  }

}
