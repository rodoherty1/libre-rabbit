package com.paddypowerbetfair.librerabbit.api.specs

import org.scalacheck.Prop._
import org.scalacheck.Properties
import com.paddypowerbetfair.librerabbit.examples.model.{Expression, Literal}

object DistributedCalculatorV4Spec extends Properties("DistributedCalculatorV4") {

  import com.paddypowerbetfair.librerabbit.api.generators.CommandGenerator._
  import IntegrationSpecCommon._

  val publishShortExpressionV4 = property("Publish-short-expression-v4") = forAll(fewCommandsGen) { cmds =>
    val result        = publishCommandsAndWaitForReply("v4")(cmds)
    val expected      = cmds.foldLeft[Expression](Literal(0))(_ apply _).toString

    result == expected
  }

  val publishIdempotent = property("Publish-idempotent-v4") = forAll(fewCommandsGen) { cmds =>
    val result       = publishCommandsTwiceAndWaitForOneReply("v4")(cmds)
    val expected     = cmds.foldLeft[Expression](Literal(0))(_ apply _ ).toString

    result == expected
  }

  val publishCommutativeV4 = property("Publish-commutative-v4") = forAll(fewCommandsGen) { cmds =>
    val result        = publishCommandsInReverseOrderAndWaitForReply("v4")(cmds)
    val expected      = cmds.foldLeft[Expression](Literal(0))(_ apply _).toString

    result == expected
  }

  override def main(args:Array[String]):Unit =
    runWithParams(_ => mainRunner(Array("-minSuccessfulTests", "10", "-workers", "10", "-verbosity", "5")))
}
