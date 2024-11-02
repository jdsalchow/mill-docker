package myplugin

import mill.testkit.{TestBaseModule, UnitTester}
import utest._
import com.ofenbeck.mill.docker.DockerJibModule
import mill.scalalib.ScalaModule
import mill.T

object UnitTests extends TestSuite {
  def tests: Tests = Tests {
    test("unit") {
      object build extends TestBaseModule with ScalaModule with DockerJibModule{
        def scalaVersion: T[String] = "2.13."
        def lineCountResourceFileName = "line-count.txt"
      }

      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_,,,DIR"))
      UnitTester(build, resourceFolder / "image-types-project").scoped { eval =>

        // Evaluating tasks by direct reference
        val Right(result) = eval(build.resources)
        assert(
          result.value.exists(pathref =>
            os.exists(pathref.path / "line-count.txt") &&
              os.read(pathref.path / "line-count.txt") == "17"
          )
        )

        // Evaluating tasks by passing in their Mill selector
        val Right(result2) = eval("resources")
        val Seq(pathrefs: Seq[mill.api.PathRef]) = result2.value
        assert(
          pathrefs.exists(pathref =>
            os.exists(pathref.path / "line-count.txt") &&
              os.read(pathref.path / "line-count.txt") == "17"
          )
        )
      }
    }
  }
}
