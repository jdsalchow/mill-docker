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

    def targetImage: T[String] = T(outer.artifactName())

    def finalConfig: T[DockerSettings] = T {
      DockerSettings(
        baseImage = baseImage(),
        targetImage = targetImage()
      )
    }

    def buildToLocalDockerDemon() = T.task {
      val conf = finalConfig()
      JibBuilds.buildToLocalDockerDemon(conf = conf, T.ctx().log)
    }

    def buildToLocalTarImage() = T.task {
      val conf = finalConfig()
      T.ctx().log.info("info test")
      JibBuilds.buildToLocalTarImage(conf)
    }

    private def isSnapshotDependency(millpath: mill.PathRef) = millpath.path.last.endsWith("-SNAPSHOT.jar")

    def testme() = T.task {
      val conf = finalConfig()

      val logger = T.ctx().log

      val upstreamAssemblyClasspath = outer.upstreamAssemblyClasspath()
      val internalDependencies: Set[PathRef] = outer.runClasspath().toSet -- outer.resources().toSet



      val dockerCachedImage                      = DockerDaemonImage.named(ImageReference.parse(conf.baseImage))
      val javaBuilder                            = JavaContainerBuilder.from(dockerCachedImage)
      val (upstreamClassSnapShot, upstreamClass) = upstreamAssemblyClasspath.partition(isSnapshotDependency(_))
      //val localClasspath =
      javaBuilder.addDependencies(upstreamClass.map(x => x.path.wrapped).toList.asJava)
      javaBuilder.addSnapshotDependencies(upstreamClassSnapShot.map(_.path.wrapped).toList.asJava)

      outer.resources().map(_.path.wrapped).foreach(path => javaBuilder.addResources(path))// double check this can be called multiple times
      // javaBuilder.addDependencies()
      val internal = (internalDependencies.filter(x => x.path.toIO.isFile).map( x => x.path.wrapped).toList)
      logger.info("internal Dependenicies:") 
      internal.map(x => logger.info(x.toString()))
      logger.info("----------------------")
      javaBuilder.addProjectDependencies(internalDependencies.filter(x => x.path.toIO.isFile).map( x => x.path.wrapped).toList.asJava)
      
      
      val jibBuilder = javaBuilder.toContainerBuilder()

      val container = jibBuilder.containerize(
        Containerizer
          .to(DockerDaemonImage.named(conf.targetImage))
          .addEventHandler(JibLogging.getLogger(logger))
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
