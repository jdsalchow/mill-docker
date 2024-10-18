package com.ofenbeck.mill.docker
import com.google.cloud.tools.jib.api.buildplan._
import com.google.cloud.tools.jib.api._
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.global.JibSystemProperties

import com.google.cloud.tools.jib.api.{RegistryImage => JibRegistryImage}
import com.google.cloud.tools.jib.api.{ImageReference => JibImageReference}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption
import mill.api.PathRef

sealed trait JibImageFormat
object JibImageFormat {
  case object Docker extends JibImageFormat
  case object OCI    extends JibImageFormat
  implicit val rw: upickle.default.ReadWriter[JibImageFormat] = upickle.default
    .readwriter[String]
    .bimap(
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

sealed trait JibSourceImage
sealed trait ImageReference {
  def qualifiedName: String
}

object JibImage {
  case class SourceTarFile(path: mill.api.PathRef) extends JibSourceImage
  case class TargetTarFile(
      qualifiedName: String,
      filename: String = "out.tar",
  ) extends ImageReference
  case class DockerDaemonImage(
      qualifiedName: String,
      useFallBack: Boolean = true,
      fallBackEnvCredentials: Option[(String, String)] = None,
  ) extends ImageReference
      with JibSourceImage
  case class RegistryImage(
      qualifiedName: String,
      credentialsEnvironment: (String, String),
  ) extends ImageReference
      with JibSourceImage

  implicit val rwtarsource: upickle.default.ReadWriter[SourceTarFile]   = upickle.default.macroRW
  implicit val rwtartaget: upickle.default.ReadWriter[TargetTarFile]    = upickle.default.macroRW
  implicit val rwregistry: upickle.default.ReadWriter[RegistryImage]    = upickle.default.macroRW
  implicit val rwdockerd: upickle.default.ReadWriter[DockerDaemonImage] = upickle.default.macroRW
  implicit val rwimage: upickle.default.ReadWriter[ImageReference]      = upickle.default.macroRW
  implicit val rw: upickle.default.ReadWriter[JibSourceImage]           = upickle.default.macroRW
}
