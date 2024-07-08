package ecsutil

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class ExpressionTest extends AnyFlatSpec with should.Matchers:
  "shouldNotBeExecuted" should "throw if executed" in:
    an[IllegalStateException] should be thrownBy(shouldNotBeExecuted)
