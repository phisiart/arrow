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

package arrow.repr

import shapeless._

import collection.mutable.ArrayBuffer

sealed trait InUntyped

/**
  * [[In]]: An input port
  *
  * Cases:
  *   [[SingleInputProcessorIn]]
  *   [[HJoinerHdIn]]
  *   [[HJoinerTlIn]]
  *   [[JoinerIn]]
  */
sealed trait In[I] extends InUntyped {
    def addSubscription(subscription: SubscriptionTo[I])

    def pullFrom: ArrayBuffer[SubscriptionTo[I]]

    def visit[R[_]](visitor: InVisitor[R]): R[I]

//    def chan: Channel[I]

//    val runtimeInfo: RuntimeInInfo[I]
}

trait InVisitor[R[_]] {
    def VisitSingleInputProcessorIn[I](in: SingleInputProcessorIn[I]): R[I]
    def VisitHJoinerHdIn[IH, IT <: HList, I <: HList](in: HJoinerHdIn[IH, IT, I]): R[IH]
    def VisitHJoinerTlIn[IH, IT <: HList, I <: HList](in: HJoinerTlIn[IH, IT, I]): R[IT]
    def VisitJoinerIn[I, Is, S[_]](in: JoinerIn[I, Is, S]): R[I]
}

/** All types of [[In]]: */

final case class SingleInputProcessorIn[I]
(processor: SingleInputProcessor[I]) extends In[I] {
    override def toString = s"${this.processor}.in"

    override def addSubscription(subscription: SubscriptionTo[I]): Unit = {
        this.processor.pullFrom.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[I]] =
        this.processor.pullFrom

//    override val chan: Channel[I] = processor.inputChan

    // TODO: implement this
//    override val runtimeInfo: SingleInputProcessorInRuntimeInfo[I] = null

    override def visit[R[_]](visitor: InVisitor[R]): R[I] =
        visitor.VisitSingleInputProcessorIn[I](this)
}

final case class HJoinerHdIn[IH, IT <: HList, I <: HList]
(hJoiner: HJoiner[IH, IT, I]) extends In[IH] {
    override def toString = s"$hJoiner.hd"

    override def addSubscription(subscription: SubscriptionTo[IH]): Unit = {
        this.hJoiner.pullFromHd.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[IH]] =
        this.hJoiner.pullFromHd

//    override val chan: Channel[IH] = hJoiner.HInputChan

    // TODO: implement this
//    override val runtimeInfo: HJoinerHdInRuntimeInfo[IH] = null

    override def visit[R[_]](visitor: InVisitor[R]): R[IH] =
        visitor.VisitHJoinerHdIn(this)
}

final case class HJoinerTlIn[IH, IT <: HList, I <: HList]
(hJoiner: HJoiner[IH, IT, I]) extends In[IT] {
    override def toString = s"$hJoiner.tl"

    override def addSubscription(subscription: SubscriptionTo[IT]): Unit = {
        this.hJoiner.pullFromTl.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[IT]] =
        this.hJoiner.pullFromTl

//    override val chan: Channel[IT] = hJoiner.TInputChan

    // TODO: implement this
//    override val runtimeInfo: HJoinerTlInRuntimeInfo[IT] = null

    override def visit[R[_]](visitor: InVisitor[R]): R[IT] =
        visitor.VisitHJoinerTlIn(this)
}

final case class JoinerIn[I, Is, S[_]]
(joiner: Joiner[I, Is, S], idx: Int) extends In[I] {
    this.joiner.ensureIdx(idx)

    override def toString = s"${this.joiner}.in[$idx]"

    override def addSubscription(subscription: SubscriptionTo[I]): Unit = {
        this.joiner.pullFroms(idx).append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[I]] =
        this.joiner.pullFroms(idx)

//    override def chan: Channel[I] = joiner.getInputChans(idx)

    // TODO: implement this
//    override val runtimeInfo: JoinerInRuntimeInfo[I] = null

    override def visit[R[_]](visitor: InVisitor[R]): R[I] =
        visitor.VisitJoinerIn(this)
}
