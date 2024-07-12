package ecsutil

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class ExpressionTest extends AnyFlatSpec with should.Matchers:
  "shouldNotBeExecuted" should "throw if executed" in:
    an[IllegalStateException] should be thrownBy (shouldNotBeExecuted)
