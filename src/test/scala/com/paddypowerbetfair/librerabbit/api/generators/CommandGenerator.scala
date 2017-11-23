package com.paddypowerbetfair.librerabbit.api.generators

import org.scalacheck.Gen
import com.paddypowerbetfair.librerabbit.examples.model._

object CommandGenerator {

  val calculationId = Gen.alphaStr

  val publishGen = Gen.const(Publish)

  def seqNumberGen = Gen.const(Iterator.from(0))

  val numberGen = Gen.chooseNum(Long.MinValue, Long.MaxValue)

  val addToGen = numberGen map AddTo

  val subtractByGen = numberGen map SubtractBy

  val multiplyByGen = numberGen map MultiplyBy

  val divideBy = numberGen map (n => (if(n == 0) 1 else n)) map DivideBy

  val commandGen = Gen.oneOf(addToGen, subtractByGen, multiplyByGen, divideBy)

  val fewCommandsGen:Gen[List[Command]] =
    Gen.listOfN(5, commandGen)

  val manyCommandsGen:Gen[List[Command]] =
    Gen.listOfN(100,commandGen)

}
