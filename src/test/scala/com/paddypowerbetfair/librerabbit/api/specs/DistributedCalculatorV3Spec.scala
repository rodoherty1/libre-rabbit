package com.paddypowerbetfair.librerabbit.api.specs

import org.scalacheck.Prop._
import org.scalacheck.Properties
import com.paddypowerbetfair.librerabbit.examples.model.{Expression, Literal}

object DistributedCalculatorV3Spec extends Properties("DistributedCalculatorV3") {

  import com.paddypowerbetfair.librerabbit.api.generators.CommandGenerator._
  import IntegrationSpecCommon._

  val publishShortExpressionV1 = property("Publish-short-expression-v3") = forAll(fewCommandsGen) { cmds =>
    val result        = publishCommandsAndWaitForReply("v3")(cmds)
    val expected      = cmds.foldLeft[Expression](Literal(0))(_ apply _).toString

    result == expected
  }

  override def main(args:Array[String]):Unit =
    runWithParams(_ => mainRunner(Array("-minSuccessfulTests", "10", "-workers", "10", "-verbosity", "5")))
}
