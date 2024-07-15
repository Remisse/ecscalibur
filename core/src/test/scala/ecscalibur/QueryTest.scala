package ecscalibur

import ecscalibur.core.*

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class QueryTest extends AnyFlatSpec with should.Matchers:

  import ecscalibur.testutil.testclasses.C1

  "A QueryBuilder" should "throw when chaining the same method multiple times" in:
    given World = World()
    an[IllegalArgumentException] should be thrownBy(query none C1 none C1)

  "queries.make()" should "successfully return a Query" in:
    noException should be thrownBy(queries.make(() => ()))
    "val q: Query = queries.make(() => ())" should compile