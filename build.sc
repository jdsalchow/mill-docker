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

val millVersions = Seq("0.11.0")
//def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)

object `mill-docker` extends Cross[MillDockerCross](millVersions)
trait MillDockerCross extends CrossModuleBase with GitVersionedPublishModule with Cross.Module[String] {
  def millVersion = crossValue
  def jibCore     = "0.27.1"

  override def crossScalaVersion = "2.13.15"
  // override def artifactSuffix    = s"_mill${millBinaryVersion(millVersion)}" + super.artifactSuffix()

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion",
    ivy"com.google.cloud.tools:jib-core:$jibCore"
  )

  def pomSettings = PomSettings(
    description = "A docker plugin for Mill build tool",
    organization = "com.ofenbeck",
    url = "https://github.com/georgofenbeck/mill-docker",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("georgofenbeck", "mill-docker"),
    developers = Seq(Developer("georgofenbeck", "Georg Ofenbeck", "https://github.com/georgofenbeck"))
  )
}

/*
object itest extends Cross[ITestCross](millVersions: _*)
class ITestCross(millVersion: String) extends MillIntegrationTestModule {
  override def millTestVersion  = millVersion
  override def pluginsUnderTest = Seq(`mill-docker`(millVersions))

  override def testInvocations = Seq[(PathRef, Seq[TestInvocation.Targets])](
    PathRef(sources().head.path / "demo") -> Seq(
      TestInvocation.Targets(Seq("check"))
    )
  )
}
*/

object itest extends Cross[ITestCross](millVersions)
trait ITestCross extends MillIntegrationTestModule with Cross.Module[String] {
  def millVersion = crossValue
  override def millTestVersion  = millVersion 
  override def pluginsUnderTest = Seq(`mill-docker`(millVersion))
  
  
  override def testInvocations = testCases().map(
    _ -> Seq(
      TestInvocation.Targets(Seq("check")),
    )
  )
}