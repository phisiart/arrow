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

import scala.collection.mutable.ArrayBuffer

final case class SourceProcessorRunnable[T]
(stream: Stream[T], outputChannels: IndexedSeq[ChannelIn[T]]) extends Runnable {
    override def run(): Unit = {
        var curr = stream
        while (curr.nonEmpty) {
            outputChannels.foreach(_.push(Push[T](curr.head)))
            curr = curr.tail
        }
        outputChannels.foreach(_.push(Finish[T]()))
    }
}

final case class NodeRunnable[I, O]
(node: Node[I, O],
 inputChannel: Channel[I],
 outputChannels: IndexedSeq[ChannelIn[O]]) extends Runnable {

    override def run(): Unit = {
        var running = true

        while (running) {
            val input = this.inputChannel.pull()
            input match {
                case Push(value) =>
                    val output = this.node.apply(value)
                    outputChannels.foreach(_.push(Push(output)))

                case Finish() =>
                    outputChannels.foreach(_.push(Finish()))
                    running = false

                case _ =>
                    throw new NotImplementedError()
            }
        }
    }
}

final case class SplitterRunnable[O, Os]() {

}

final case class DrainProcessorCallable[T](inputChannel: Channel[T])
    extends Callable[IndexedSeq[T]] {

    val buf: ArrayBuffer[T] = ArrayBuffer.empty[T]

    override def call(): IndexedSeq[T] = {
        var running = true

        while (running) {
            val elem = inputChannel.pull()
            elem match {
                case Push(value) =>
                    println(s"New output value: $elem.")
                    buf.append(value)

                case Finish() =>
                    println("Finished.")
                    running = false

                case _ =>
                    throw new NotImplementedError()
            }
        }

        buf
    }
}
