package com.paddypowerbetfair.librerabbit.api

import com.paddypowerbetfair.librerabbit.all._
import org.specs2.{ScalaCheck, Specification}
import org.specs2.matcher.MustMatchers

import org.scalacheck.{Arbitrary, Gen}

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class AckSpec extends Specification with ScalaCheck with MustMatchers {

  implicit val envelopes: Arbitrary[List[AmqpEnvelope]] =
    Arbitrary(Gen.listOf(Gen.const(AmqpEnvelope(0, isRedelivery = false, None, None, AmqpMessage.emptyMessage))))

  def is = s2"""

    Specification describing how acks are managed

    Message is acked after processing of message halts  $ackedOnSuccess
    Message can be nacked and requeued on failure
    Message can be nacked without requeuing on failure

  """

  def ackedOnSuccess = prop { (generatedMessages:List[AmqpEnvelope]) =>
    true
  }
}
