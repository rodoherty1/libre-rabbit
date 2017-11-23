package com.paddypowerbetfair.librerabbit.api.interpreters

import com.rabbitmq.client.{Channel, Connection}
import com.paddypowerbetfair.librerabbit.api.algebra._
import com.paddypowerbetfair.librerabbit.api.dsl._
import com.paddypowerbetfair.librerabbit.api.channel._

import scalaz._
import scalaz.syntax.monad._

object connect {

  type ConnectRes[A] = com.paddypowerbetfair.librerabbit.api.channel.internal.Result[A]

  def declare[A](ba: BrokerIO[A])(conn:Connection):ConnectRes[A] = {

    val declaration:BrokerIO[A] = ba <* Free.liftFC(DoneF(identity))

    Free.runFC[BrokerF, ConnectRes, A](declaration)(new (BrokerF ~> ConnectRes) {

      val adminCh:Channel = conn.createChannel()

      def apply[A0](value: BrokerF[A0]): ConnectRes[A0] = value match {
        case DecQF(name, dur, excl, autDel, dl, v)  => mkQueue                  (name, dur, excl, autDel, dl).map(v).run(adminCh)
        case DecDirExF(name, dur, altEx, v)         => mkDirectExchange         (name, dur, altEx).map(v).run(adminCh)
        case DecTopExF(name, dur, altEx, v)         => mkTopicExchange          (name, dur, altEx).map(v).run(adminCh)
        case DecFanoutExF(name, dur, altEx, v)      => mkFanoutExchange         (name, dur, altEx).map(v).run(adminCh)
        case DecConsHHdrExF(n, dur, h, altEx, v)    => mkConsHashHeaderExchange (n, dur, h, altEx).map(v).run(adminCh)
        case DecConsHPrpExF(n, dur, p, altEx, v)    => mkConsHashPropExchange   (n, dur, p, altEx).map(v).run(adminCh)
        case DecHdrExF(name, dur, altEx, v)         => mkHeaderExchange         (name, dur, altEx).map(v).run(adminCh)
        case DecDirBF(d, ex, key, v)                => mkDirectBinding          (d, ex, key).map(v).run(adminCh)
        case DecTopBF(d, ex, key, v)                => mkTopicBinding           (d, ex, key).map(v).run(adminCh)
        case DecHdrBF(d, ex, key, v)                => mkHeaderBinding          (d, ex, key).map(v).run(adminCh)
        case DecConsHashBF(d, ex, key, v)           => mkConsHashBinding        (d, ex, key).map(v).run(adminCh)
        case DecFanoutBF(d, ex, v)                  => mkFanoutBinding          (d, ex).map(v).run(adminCh)
        case ConsumerF(q, pfet, excl, rec, fail, v) => mkConsumer               (q, pfet, excl)(rec)(fail).map(v).run(conn.createChannel())
        case DirPubF(exch, v)                       => mkDirectPublisher        (exch).map(v).run(conn.createChannel())
        case TopPubF(exch, v)                       => mkTopicPublisher         (exch).map(v).run(conn.createChannel())
        case FanoutPubF(exch, v)                    => mkFanoutPublisher        (exch).map(v).run(conn.createChannel())
        case HdrPubF(exch, v)                       => mkHeaderPublisher        (exch).map(v).run(conn.createChannel())
        case ConsHashPubF(exch, v)                  => mkConsHashPublisher      (exch).map(v).run(conn.createChannel())
        case DirReqRplyF(exch, q, k, p, v)          => mkDirectReqRply          (exch, q, k, p).map(v).run(conn.createChannel())
        case TopReqRplyF(exch, q, k, p, v)          => mkTopicReqRply           (exch, q, k, p).map(v).run(conn.createChannel())
        case FanoutReqRplyF(exch, q, k, p, v)       => mkFanoutReqRply          (exch, q, k, p).map(v).run(conn.createChannel())
        case HdrReqRplyF(exch, q, k, p, v)          => mkHeaderReqRply          (exch, q, k, p).map(v).run(conn.createChannel())
        case ConsHashReqRplyF(exch, q, k, p, v)     => mkConsHashReqRply        (exch, q, k, p).map(v).run(conn.createChannel())
        case DoneF(v)                               => closeCurrentChannel      .map( fn => v(fn(()))).run(adminCh)
      }
    })
  }
}
