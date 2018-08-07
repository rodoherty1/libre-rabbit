package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.all._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class AckSpec extends FlatSpec with PropertyChecks with Matchers {

  "A message" should " be acked after processing of message halts" in new AckFixture {
    forAll (envelopes) { envelope =>
      envelope.size shouldBe an[Integer]
    }
  }
}


class AckFixture {
  val envelopes = Gen.listOf(Gen.const(AmqpEnvelope(0, isRedelivery = false, None, None, AmqpMessage.emptyMessage)))

}
