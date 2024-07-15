package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import ecscalibur.error.IllegalDefinitionException
import ecscalibur.error.IllegalTypeParameterException

class ErrorTest extends AnyFlatSpec with should.Matchers:

  "IllegalDefinitionException" should "be thrown correctly" in:
    an[IllegalDefinitionException] should be thrownBy(throw IllegalDefinitionException())

  "IllegalTypeParameterException" should "be thrown correctly" in:
    an[IllegalTypeParameterException] should be thrownBy(throw IllegalTypeParameterException())
