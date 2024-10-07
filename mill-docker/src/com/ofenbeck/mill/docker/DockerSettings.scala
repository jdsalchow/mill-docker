package com.ofenbeck.mill.docker

import com.google.cloud.tools.jib.api.ImageReference

final case class DockerSettings(
  val baseImage: String,
  val targetImage: String,
)
object DockerSettings{
  implicit val rw: upickle.default.ReadWriter[DockerSettings] = upickle.default.macroRW
}
