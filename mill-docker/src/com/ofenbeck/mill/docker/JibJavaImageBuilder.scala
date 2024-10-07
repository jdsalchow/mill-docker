package com.ofenbeck.mill.docker

import java.io.File
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api.{ Containerizer, JavaContainerBuilder }
/*
class JibJavaImageBuilder {
   def javaBuild(
      targetDirectory: File,
      //configuration: SbtConfiguration,
      jibBaseImageCredentialHelper: Option[String],
      jvmFlags: List[String],
      tcpPorts: List[Int],
      udpPorts: List[Int],
      args: List[String],
      imageFormat: ImageFormat,
      environment: Map[String, String],
      labels: Map[String, String],
      additionalTags: List[String],
      user: Option[String],
      useCurrentTimestamp: Boolean,
      platforms: Set[Platform]
  )(containerizer: Containerizer): Unit = {
    
    val baseImage = JibCommon.baseImageFactory(configuration.baseImageReference)(
      jibBaseImageCredentialHelper,
      configuration.credsForHost,
      configuration.logEvent
    )
    JibCommon.configureContainerizer(containerizer)(
      additionalTags,
      configuration.allowInsecureRegistries,
      configuration.USER_AGENT_SUFFIX,
      configuration.target.toPath
    )
    val javaBuilder = JavaContainerBuilder.from(baseImage)
    JibCommon.prepareJavaContainerBuilder(javaBuilder)(
      configuration.layerConfigurations.external.map(_.data.toPath).toList,
      configuration.layerConfigurations.addToClasspath.map(_.toPath),
      configuration.layerConfigurations.internalDependencies.map(_.data.toPath).toList,
      configuration.layerConfigurations.resourceDirectories.map(_.toPath).toList,
      configuration.layerConfigurations.classes.map(_.toPath).toList,
      Some(configuration.pickedMainClass),
      jvmFlags
    )
    val jibBuilder = javaBuilder.toContainerBuilder
    JibCommon.prepareJibContainerBuilder(jibBuilder)(
      tcpPorts.toSet.map(s => Port.tcp(s)) ++ udpPorts.toSet.map(s => Port.udp(s)),
      args,
      internalImageFormat,
      environment,
      labels,
      user,
      useCurrentTimestamp,
      platforms,
      None
    )
    val container = jibBuilder.containerize(containerizer)
} 
}
*/