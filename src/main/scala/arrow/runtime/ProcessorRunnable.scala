/*
 * The MIT License
 *
 * Copyright (c) 2016 Zhixun Tan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package arrow.runtime

import java.util.concurrent.Callable

import akka.actor.{ActorPath, ActorRef, ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import arrow._
import arrow.runtime.MasterWorkerProtocol.Task
import shapeless._

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await

final case class SourceProcessorRunnable[T]
(id: Int,
 stream: Stream[T],
 outputChannels: IndexedSeq[ChannelIn[T]]) extends Runnable {
    override def run(): Unit = {
        var curr = stream
        while (curr.nonEmpty) {
            val output = curr.head
            curr = curr.tail

            outputChannels.foreach(_.push(Value[T](output), id))
        }
        outputChannels.foreach(_.push(Finish[T](), id))
    }
}

abstract class AbstractNodeRunnable[I, O] extends Runnable {
    val id: Int
    val node: Node[I, O]
    val inputChannel: Channel[I]
    val outputChannels: IndexedSeq[ChannelIn[O]]

    def apply(input: I): O

    override def run(): Unit = {
        var running = true

        while (running) {
            val input = this.inputChannel.pull(id)

            val msg: R[O] = input match {
                case Value(value, _, _) =>
                    val output = this.apply(value)
                    Value(output)


                case Finish() =>
                    running = false
                    Finish()

                case Empty() =>
                    Empty()

                case Break() =>
                    Break()
            }

            outputChannels.foreach(_.push(msg, id))
        }
    }
}

final case class LocalNodeRunnable[I, O]
(id: Int,
 node: Node[I, O],
 inputChannel: Channel[I],
 outputChannels: IndexedSeq[ChannelIn[O]]) extends AbstractNodeRunnable[I, O] {
    override def apply(input: I): O = this.node.apply(input)
}

final case class DistributedNodeRunnable[I, O]
(id: Int,
 node: Node[I, O],
 inputChannel: Channel[I],
 outputChannels: IndexedSeq[ChannelIn[O]],
 master: ActorRef) extends AbstractNodeRunnable[I, O] {
    implicit val askTimeout = Timeout(10000000 second)

//    val m = ActorSystem.

    override def apply(input: I): O = {
        Await.result(master ? Task(id, input), 1000000 seconds).asInstanceOf[O]
    }
}

/**
  *  Os  +----------+  Seq[O]
  * ---> | Splitter | ------->
  *      +----------+
  */
final case class SplitterRunnable[O, Os]
(id: Int,
 inputChannel: Channel[Os],
 outputChannals: IndexedSeq[IndexedSeq[ChannelIn[O]]])
(implicit val os: Os <:< Seq[O]) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val input = this.inputChannel.pull(id)

            val outputs: Seq[R[O]] = input match {
                case Value(value, _, _) =>
                    os(value).map(Value(_))

                case Finish() =>
                    running = false
                    Seq.fill(this.outputChannals.size)(Finish())

                case Empty() =>
                    Seq.fill(this.outputChannals.size)(Empty())

                case Break() =>
                    Seq.fill(this.outputChannals.size)(Break())
            }

            (outputChannals zip outputs)
                .foreach(
                    {
                        case (channelIn, output) =>
                            channelIn.foreach(_.push(output, id))
                    }
                )
        }
    }
}

final case class JoinerRunnable[I, Is, S[_]]
(id: Int,
 inputChannels: IndexedSeq[Channel[I]],
 outputChannels: IndexedSeq[ChannelIn[Is]])
(implicit
 val is: S[I] =:= Is,
 val cbf: CanBuildFrom[Nothing, I, S[I]]) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val inputs = inputChannels.map(_.pull(id))

            var hasFinish = false
            var hasBreak = false
            var hasEmpty = false
            inputs.foreach(
                {
                    case Value(_, _, _) =>
                    case Finish() => hasFinish = true
                    case Break() => hasBreak = true
                    case Empty() => hasEmpty = true
                }
            )

            val output: R[Is] =
                if (hasFinish) {
                    running = false
                    Finish()

                } else if (hasBreak) {
                    Break()

                } else if (hasEmpty) {
                    Empty()

                } else {
                    Value[Is](
                        is(
                            inputs
                                .map(_.asInstanceOf[Value[I]].value)
                                .to[S]
                        )
                    )
                }

            outputChannels.foreach(_.push(output, id))
        }
    }
}

final case class HSplitterRunnable[H, T <: HList, L <: HList]
(id: Int,
 inputChan: Channel[L],
 hOutputChans: IndexedSeq[ChannelIn[H]],
 tOutputChans: IndexedSeq[ChannelIn[T]])
(implicit val l: L <:< (H :: T)) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val input = inputChan.pull(id)

            val (hOutput: R[H], tOutput: R[T]) = input match {
                case Value(value, _, _) =>
                    val head = value.head
                    val tail = value.tail

                    (Value(head), Value(tail))

                case Finish() =>
                    running = false
                    (Finish(), Finish())

                case Break() =>
                    (Break(), Break())

                case Empty() =>
                    (Empty(), Empty())

            }

            hOutputChans.foreach(_.push(hOutput, id))
            tOutputChans.foreach(_.push(tOutput, id))
        }
    }
}

final case class HJoinerRunnable[H, T <: HList, L <: HList]
(id: Int,
 hInputChan: Channel[H],
 tInputChan: Channel[T],
 outputChans: IndexedSeq[ChannelIn[L]])
(implicit val l: (H :: T) <:< L) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val hd = hInputChan.pull(id)
            val tl = tInputChan.pull(id)

            val msg: R[L] = (hd, tl) match {
                case (Value(head, _, _), Value(tail, _, _)) =>
                    val input = head :: tail
                    val output = l(input)

                    Value(output)

                case (Finish(), _) | (_, Finish()) =>
                    running = false
                    Finish()

                case (Break(), _) | (_, Break()) =>
                    Break()

                case (Empty(), _) | (_, Empty()) =>
                    Empty()
            }

            outputChans.foreach(_.push(msg, id))
        }
    }
}

final case class DrainProcessorCallable[T]
(id: Int,
 inputChannel: Channel[T]) extends Callable[IndexedSeq[T]] {

    val buf: ArrayBuffer[T] = ArrayBuffer.empty[T]

    override def call(): IndexedSeq[T] = {
        var running = true
        var replaceable = false

        while (running) {
            val elem = inputChannel.pull(id)

            elem match {
                case Value(value, _, _) =>
                    if (replaceable) {
                        buf(buf.length - 1) = value
                    } else {
                        buf.append(value)
                    }

                    replaceable = elem.replaceable

                case Empty() =>
                    // Ignore

                case Break() =>
                    // Ignore

                case Finish() =>
                    running = false
            }
        }

        buf
    }
}
