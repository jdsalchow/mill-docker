package com.ofenbeck.mill.docker
import mill.testkit.ExampleTester
import utest._

object CustomJibBuildExample extends TestSuite {

  def tests: Tests = Tests {
    test("image types") {
      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))

      ExampleTester.run(
        clientServerMode = true,
        workspaceSourcePath = resourceFolder / "examples-customJibBuild",
        millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
      )
    }
  } 
}
  
