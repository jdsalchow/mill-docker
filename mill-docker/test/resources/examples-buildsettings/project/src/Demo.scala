package com.ofenbeck

import scalatags.Text.all._
import os._
object Demo {
  @main
  def main() = {
    
    val snapshotCode = new com.twitter.chill.config.JavaMapConfig //to ensure that the snapshot dependency is working (on its own layer)
    val tag = html( //regular dependency
      head(
        script(src := "..."),
        script(
          "alert('Hello World')",
        ),
      ),
      body(
        h1(
        os.read(os.resource / "blub.txt") //reading from the resource layer
        ),
        os.read(os.resource / "extra.txt") //reading from the additional resource layer
      ),
    )
    println(tag.render)
    val check = new TaxModule {}
    println(s"Compile time dependencies are working - but not included in the image ${check.taxCalculator(10)}")

    println(com.ofenbeck.unmanaged.HelloTest.unmanagedLibHello()) //checking that unmanaged dependencies are working (added to snapshot layer)
  }
}


// compile time dependencies
import com.softwaremill.macwire.*
import scala.util.Using


class DatabaseAccess()
class TaxDeductionLibrary(databaseAccess: DatabaseAccess)
class TaxCalculator(taxBase: Double, taxDeductionLibrary: TaxDeductionLibrary)

trait TaxModule {
  import com.softwaremill.macwire._

  lazy val theDatabaseAccess      = wire[DatabaseAccess]
  lazy val theTaxDeductionLibrary = wire[TaxDeductionLibrary]
  def taxCalculator(taxBase: Double) = wire[TaxCalculator]
  // or: lazy val taxCalculator = (taxBase: Double) => wire[TaxCalculator]
}

