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

import arrow._

import scala.collection.mutable

/**
  * Node[_, T] |> Node[T, _]
  *
  * +------------+  Push(T)  +------------+  Push(T)  +------------+
  * | Node[_, T] | --------> | Channel[T] | --------> | Node[T, _] |
  * +------------+           +------------+           +------------+
  *
  *
  * Node[_, R[T] ] |> Node[T, _]
  *
  * +----------------+  Push(R[T])  +------------+  Push(T)  +------------+
  * | Node[_, R[T] ] | -----------> | Channel[T] | --------> | Node[T, _] |
  * +----------------+              +------------+           +------------+
  *
  */

sealed trait ChannelIn[T] {
    def push(msg: R[T]): Unit
}

final case class ChannelInImpl[T](chan: Channel[T]) extends ChannelIn[T] {
    override def push(msg: R[T]): Unit = {
        chan.push(msg)
    }
}

final case class ChannelInRImpl[T, RT](chan: Channel[T])(implicit rt: RT <:< R[T]) extends ChannelIn[RT] {
    override def push(msg: R[RT]): Unit = {
        // TODO: modify this
        chan.push(msg match {
            case Push(value) =>
                rt(value)

            case Put(value) =>
                rt(value)

            case Finish() =>
                Finish[T]()

            case Empty() =>
                Empty[T]()

            case Break() =>
                Break[T]()
        })
    }
}

class Channel[T] {
    val BUF_SIZE = 100
    val buf = new mutable.Queue[R[T]]()

    def push(msg: R[T]): Unit = synchronized {
        msg match {
            case Push(value) =>
                while (buf.size == BUF_SIZE) {
                    wait()
                }

                buf.enqueue(msg)
                notifyAll()

            case Finish() =>
                buf.enqueue(msg)
                notifyAll()

            case _ =>
                // TODO: not implemented
                throw new NotImplementedError()
        }
    }

    def pull(): R[T] = synchronized {
        while (buf.isEmpty) {
            wait()
        }

        val elem = buf.dequeue()
        notifyAll()

        elem
    }
}
