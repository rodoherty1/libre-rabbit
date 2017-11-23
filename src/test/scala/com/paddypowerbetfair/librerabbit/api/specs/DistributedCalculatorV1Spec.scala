package com.paddypowerbetfair.librerabbit.api.specs

import org.scalacheck.Prop._
import org.scalacheck.Properties
import com.paddypowerbetfair.librerabbit.examples.model.{Expression, Literal}

object DistributedCalculatorV1Spec extends Properties("DistributedCalculatorV1") {

  import com.paddypowerbetfair.librerabbit.api.generators.CommandGenerator._, IntegrationSpecCommon._

  val publishShortExpressionV1 = property("Publish-short-expression-v1") = forAll(fewCommandsGen) { cmds =>
    val result        = publishCommandsAndWaitForReply("v1")(cmds)
    val expected      = cmds.foldLeft[Expression](Literal(0))(_ apply _).toString

    result == expected
  }

  override def main(args:Array[String]):Unit =
    runWithParams(_ => mainRunner(Array("-minSuccessfulTests", "2", "-workers", "1", "-verbosity", "5")))
}
