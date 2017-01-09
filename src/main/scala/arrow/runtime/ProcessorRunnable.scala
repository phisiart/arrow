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

import arrow._
import shapeless._

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer

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

final case class NodeRunnable[I, O]
(id: Int,
 node: Node[I, O],
 inputChannel: Channel[I],
 outputChannels: IndexedSeq[ChannelIn[O]]) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val input = this.inputChannel.pull(id)

            val msg: R[O] = input match {
                case Value(value, _, _) =>
                    val output = this.node.apply(value)
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
            input match {
                case Value(value, _, _) =>
                    val outputs = os(value)

                    outputChannals
                        .zip(outputs)
                        .foreach(
                        {
                            case (channelIn, output) =>
                                channelIn.foreach(_.push(Value(output), id))
                        })

                case Finish() =>
                    outputChannals
                        .foreach(_.foreach(_.push(Finish(), id)))
                    running = false

                case Empty() =>
                    outputChannals
                        .foreach(_.foreach(_.push(Empty(), id)))

                case Break() =>
                    outputChannals
                        .foreach(_.foreach(_.push(Break(), id)))
            }
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

//                    hOutputChans.foreach(_.push(Value(head), id))
//                    tOutputChans.foreach(_.push(Value(tail), id))

                case Finish() =>
                    running = false
                    (Finish(), Finish())

//                    hOutputChans.foreach(_.push(Finish()))
//                    tOutputChans.foreach(_.push(Finish()))

                case Break() =>
                    (Break(), Break())

//                    hOutputChans.foreach(_.push(Break()))
//                    tOutputChans.foreach(_.push(Break()))

                case Empty() =>
                    (Empty(), Empty())

//                    hOutputChans.foreach(_.push(Empty()))
//                    tOutputChans.foreach(_.push(Empty()))
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
//                    outputChans.foreach(_.push(Value(output)))

                case (Finish(), _) | (_, Finish()) =>
//                    outputChans.foreach(_.push(Finish()))
                    running = false
                    Finish()

                case (Break(), _) | (_, Break()) =>
                    Break()
//                    outputChans.foreach(_.push(Break()))

                case (Empty(), _) | (_, Empty()) =>
                    Empty()
//                    outputChans.foreach(_.push(Break()))
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
