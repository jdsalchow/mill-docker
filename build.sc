import mill.scalalib.publish.Scope.Test
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`com.goyeau::mill-git::0.2.5`

import de.tobiasroeser.mill.integrationtest._
import com.goyeau.mill.git.GitVersionedPublishModule

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil._
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}

val millVersions = Seq("0.12.0") //,"0.12.0-RC3")
//val millVersions = Seq("0.11.12")
val jibCore      = "0.27.1"
//def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)

object `mill-docker` extends Cross[MillDockerCross](millVersions)
trait MillDockerCross extends CrossModuleBase with GitVersionedPublishModule with Cross.Module[String] {
  def millVersion = crossValue

  override def publishVersion: T[String] = "0.0.3-SNAPSHOT"

  override def crossScalaVersion = "2.13.15"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    // ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.google.cloud.tools:jib-core:$jibCore",
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion",
  )

  def pomSettings = PomSettings(
    description = "A docker plugin for Mill build tool",
    organization = "com.ofenbeck",
    url = "https://github.com/georgofenbeck/mill-docker",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("georgofenbeck", "mill-docker"),
    developers = Seq(Developer("georgofenbeck", "Georg Ofenbeck", "https://github.com/georgofenbeck")),
  )
}

object itest extends Cross[ITestCross](millVersions)
trait ITestCross extends MillIntegrationTestModule with Cross.Module[String] {
  def millVersion               = crossValue
  override def millTestVersion  = millVersion
  override def pluginsUnderTest = Seq(`mill-docker`(millVersion))

  override def testInvocations = T {

    testCases().map(pathref =>
      pathref match {
        case p if p.toString.contains("imagetypes") =>
          p -> Seq(
            TestInvocation.Targets(
              Seq(
                "registry2demon.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "registry2tar.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "registry2tar.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "registry2registry.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "demon2demon.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "demon2tar.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "demon2registry.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "tar2demon.docker.buildImage",
              ),
            ), 
            TestInvocation.Targets(
              Seq(
                "tar2tar.docker.buildImage",
              ),
            ),
            TestInvocation.Targets(
              Seq(
                "tar2registry.docker.buildImage",
              ),
            ),
          )
        case path =>
          path -> Seq(
            //TestInvocation.Targets(Seq("project.docker.buildImage")),
            TestInvocation.Targets(Seq("project.docker.buildImage")),
          )
      },
    )

  }

  /* testCases().map(
    _ -> Seq(
      TestInvocation.Targets(Seq("check"))
    )
  )*/
}
