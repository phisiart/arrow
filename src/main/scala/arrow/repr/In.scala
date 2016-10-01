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

trait InUntyped

trait In[I] extends InUntyped {
    def addSubscription(subscription: SubscriptionTo[I])

    def pullFrom: ArrayBuffer[SubscriptionTo[I]]
}

/** All types of [[In]]: */

final case class SingleInputProcessorIn[I]
(processor: SingleInputProcessor[I]) extends In[I] {
    override def toString = s"${this.processor}.in"

    override def addSubscription(subscription: SubscriptionTo[I]) = {
        this.processor.pullFrom.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[I]] = this.processor.pullFrom
}

final case class HJoinerHdIn[IH, IT <: HList, I <: HList]
(hJoiner: HJoiner[IH, IT, I]) extends In[IH] {
    override def toString = s"$hJoiner.hd"

    override def addSubscription(subscription: SubscriptionTo[IH]) = {
        this.hJoiner.pullFromHd.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[IH]] = this.hJoiner.pullFromHd
}

final case class HJoinerTlIn[IH, IT <: HList, I <: HList]
(hJoiner: HJoiner[IH, IT, I]) extends In[IT] {
    override def toString = s"$hJoiner.tl"

    override def addSubscription(subscription: SubscriptionTo[IT]) = {
        this.hJoiner.pullFromTl.append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[IT]] = this.hJoiner.pullFromTl
}

final case class JoinerIn[I, Is]
(joiner: Joiner[I, Is], idx: Int) extends In[I] {
    this.joiner.ensureIdx(idx)

    override def toString = s"${this.joiner}.in[$idx]"

    override def addSubscription(subscription: SubscriptionTo[I]) = {
        this.joiner.pullFroms(idx).append(subscription)
    }

    override def pullFrom: ArrayBuffer[SubscriptionTo[I]] = this.joiner.pullFroms(idx)
}
