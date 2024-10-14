package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties
import scala.jdk.CollectionConverters._

object MDBuilds {
/*
  def buildToLocalDockerDemon(conf: DockerSettings, logger: mill.api.Logger) = {
    val baseImage         = RegistryImage.named(ImageReference.parse(conf.baseImage))
    val targetImage       = RegistryImage.named(ImageReference.parse(conf.targetImage))
    val dockerCachedImage = DockerDaemonImage.named(ImageReference.parse(conf.baseImage))
    val jib = Jib
      // .from(baseImage)
      .from(dockerCachedImage)
      .containerize(
        Containerizer
          .to(DockerDaemonImage.named(conf.targetImage))
          .addEventHandler(MDLogging.getLogger(logger))
      )
  }

  def buildToLocalTarImage(conf: DockerSettings) = {}

  private def isSnapshotDependency(millpath: mill.PathRef) = millpath.path.last.endsWith("-SNAPSHOT.jar")

  def buildJavaBuild(
      conf: DockerSettings,
      upstreamAssemblyClasspath: mill.Agg[mill.PathRef],
      resoures: mill.Agg[mill.PathRef],
      internalDependencies: mill.Agg[mill.PathRef],
      logger: mill.api.Logger
  ) = {
    val dockerCachedImage                      = DockerDaemonImage.named(ImageReference.parse(conf.baseImage))
    val javaBuilder                            = JavaContainerBuilder.from(dockerCachedImage)
    val (upstreamClassSnapShot, upstreamClass) = upstreamAssemblyClasspath.partition(isSnapshotDependency(_))
    /*
    logger.info("snapshot dependencies")
    upstreamClassSnapShot.foreach( file =>
      logger.info(file.path.toString())
    )
    logger.info("regular dependencies")
    upstreamClass.foreach( file =>
      logger.info(file.path.toString())
    )*/
    logger.info("internal dependencies")
    internalDependencies.foreach( file =>
      logger.info(file.path.toString())
    )
    javaBuilder.addDependencies(upstreamClass.map(x => x.path.wrapped).toList.asJava)
    javaBuilder.addSnapshotDependencies(upstreamClassSnapShot.map(_.path.wrapped).toList.asJava)
    javaBuilder.addProjectDependencies(internalDependencies.map(_.path.wrapped).toList.asJava)
    // javaBuilder.addDependencies()
    val jibBuilder = javaBuilder.toContainerBuilder()
    val container = jibBuilder.containerize(

        Containerizer
          .to(DockerDaemonImage.named(conf.targetImage))
          .addEventHandler(MDLogging.getLogger(logger))
    )

    "blub"
  }
    */
}
