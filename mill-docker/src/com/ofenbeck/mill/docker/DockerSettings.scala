package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.ImageReference

sealed trait JibImageFormat
object JibImageFormat {
  case object Docker extends JibImageFormat
  case object OCI    extends JibImageFormat
  implicit val rw: upickle.default.ReadWriter[JibImageFormat] = upickle.default.readwriter[String].bimap(
    {
      case Docker => "Docker"
      case OCI    => "OCI"
    },
    {
      case "Docker" => Docker
      case "OCI"    => OCI
    },
  )
}


final case class Platform(
    val os: String,
    val architecture: String,
)
object Platform {
  implicit val rw: upickle.default.ReadWriter[Platform] = upickle.default.macroRW
}

final case class BuildSettings(
    val baseImageCredentialEnv: (String, String),
    val targetImageCredentialEnv: (String, String),
    val setAllowInsecureRegistries: Boolean = false,
    val useCurrentTimestamp: Boolean = true,
    val upstreamAssemblyClasspath: Seq[mill.PathRef],
    val resourcesPaths: Seq[mill.PathRef],
    val compiledClasses: mill.PathRef,
    val mainClass: Option[String],
    val autoDetectMainClass: Boolean = true,
    val pullBaseImage: Boolean,
)

object BuildSettings {
  implicit val rw: upickle.default.ReadWriter[BuildSettings] = upickle.default.macroRW
}

final case class DockerSettings(
    val baseImage: String,
    val targetImage: String,
    val tags: Seq[String],
    val labels: Map[String, String],
    val jvmOptions: Seq[String],
    val exposedPorts: Seq[Int],
    val exposedUdpPorts: Seq[Int],
    val envVars: Map[String, String],
    val user: Option[String],
    val platforms: Set[Platform],
    val internalImageFormat: JibImageFormat,
    val entrypoint: Seq[String],
    val args: Seq[String],
    // volumes
    // labels
)
object DockerSettings {
  implicit val rw: upickle.default.ReadWriter[DockerSettings] = upickle.default.macroRW
}
