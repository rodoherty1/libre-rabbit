package com.paddypowerbetfair.librerabbit.api.interpreters

import com.paddypowerbetfair.librerabbit.api.model._
import com.paddypowerbetfair.librerabbit.api.validation._
import com.paddypowerbetfair.librerabbit.api.algebra._
import com.paddypowerbetfair.librerabbit.api.dsl._

import scalaz._
import Scalaz._

object validate {

  def topologyOf[A](ba: BrokerIO[A]):ValidationNel[String, Topology] = {

    val (validated, _) = Free.runFC[BrokerF, Valid, A](ba)(new (BrokerF ~> Valid) {

      def apply[A0](value: BrokerF[A0]): Valid[A0] = value match {
        case DecDirExF(name, durable, altEx, v)     => validateDirectExchange           (name, durable, altEx).map(v)
        case DecTopExF(name, durable, altEx, v)     => validateTopicExchange            (name, durable, altEx).map(v)
        case DecFanoutExF(name, durable, altEx, v)  => validateFanoutExchange           (name, durable, altEx).map(v)
        case DecConsHHdrExF(name, dur, h, altEx, v) => validateConsHashHeaderExchange   (name, dur, h, altEx).map(v)
        case DecConsHPrpExF(name, dur, p, altEx, v) => validateConsHashPropertyExchange (name, dur, p, altEx).map(v)
        case DecHdrExF(name, durable, altEx, v)     => validateHeaderExchange           (name, durable, altEx).map(v)
        case DecQF(name, dur, excl, autoDl, dl, v)  => validateQueue                    (name, dur, excl, autoDl, dl).map(v)
        case DecDirBF(d, ex, key, v)                => validateDirectBinding            (d, ex, key).map(v)
        case DecTopBF(d, ex, key, v)                => validateTopicBinding             (d, ex, key).map(v)
        case DecFanoutBF(d, ex, v)                  => validateFanoutBinding            (d, ex).map(v)
        case DecHdrBF(d, ex, key, v)                => validateHeaderBinding            (d, ex, key).map(v)
        case DecConsHashBF(d, ex, key, v)           => validateConsHashBinding          (d, ex, key).map(v)
        case ConsumerF(q, pfch, excl, _, _, v)      => validateConsumer                 (q, pfch, excl).map(v)
        case DirPubF(exch, v)                       => validateDirectPublisher          (exch).map(v)
        case TopPubF(exch, v)                       => validateTopicPublisher           (exch).map(v)
        case FanoutPubF(exch, v)                    => validateFanoutPublisher          (exch).map(v)
        case HdrPubF(exch, v)                       => validateHeaderPublisher          (exch).map(v)
        case ConsHashPubF(exch, v)                  => validateConsHashPublisher        (exch).map(v)
        case DirReqRplyF(e, q, k, p, v)             => validateDirectRequestReply       (e,q,k,p).map(v)
        case TopReqRplyF(e, q, k, p, v)             => validateTopicRequestReply        (e,q,k,p).map(v)
        case FanoutReqRplyF(e, q, k, p, v)          => validateFanoutRequestReply       (e,q,k,p).map(v)
        case HdrReqRplyF(e, q, k, p, v)             => validateHeaderRequestReply       (e,q,k,p).map(v)
        case ConsHashReqRplyF(e, q, k, p, v)        => validateConsHashRequestReply     (e,q,k,p).map(v)
        case DoneF(v)                               => validationDone                   ().map(v)
      }
    }).run(Validated(Topology.emptyTopology, Topology.emptyTopology.successNel[String])).run

    validated.result.map(_ => validated.topology)
  }
}
