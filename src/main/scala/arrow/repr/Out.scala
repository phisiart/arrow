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

trait OutUntyped

trait Out[O] extends OutUntyped {
    def addSubscription(subscription: SubscriptionFrom[O])

    def pushTo: ArrayBuffer[SubscriptionFrom[O]]
}

/** All types of [[Out]]: */

final case class SingleOutputProcessorOut[O]
(processor: SingleOutputProcessor[O]) extends Out[O] {
    override def toString = s"${this.processor}.out"

    override def addSubscription(subscription: SubscriptionFrom[O]) = {
        this.processor.pushTo.append(subscription)
    }

    override def pushTo: ArrayBuffer[SubscriptionFrom[O]] = this.processor.pushTo
}

final case class HSplitterHdOut[OH, OT <: HList, Os <: HList]
(hSplitter: HSplitter[OH, OT, Os]) extends Out[OH] {
    override def toString = s"${this.hSplitter}.hd"

    override def addSubscription(subscription: SubscriptionFrom[OH]) = {
        this.hSplitter.pushToHd.append(subscription)
    }

    override def pushTo: ArrayBuffer[SubscriptionFrom[OH]] = this.hSplitter.pushToHd
}

final case class HSplitterTlOut[OH, OT <: HList, O <: HList]
(hSplitter: HSplitter[OH, OT, O]) extends Out[OT] {
    override def toString = s"${this.hSplitter}.tl"

    override def addSubscription(subscription: SubscriptionFrom[OT]) = {
        this.hSplitter.pushToTl.append(subscription)
    }

    override def pushTo: ArrayBuffer[SubscriptionFrom[OT]] = this.hSplitter.pushToTl
}

final case class SplitterOut[O, Os]
(splitter: Splitter[O, Os], idx: Int) extends Out[O] {

    this.splitter.ensureIdx(idx)

    override def toString = s"$splitter.out[$idx]"

    override def addSubscription(subscription: SubscriptionFrom[O]) = {
        this.splitter.pushTos(idx).append(subscription)
    }

    override def pushTo: ArrayBuffer[SubscriptionFrom[O]] = this.splitter.pushTos(idx)
}
