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

import arrow.Node
import shapeless._

import scala.collection.mutable.ArrayBuffer

trait Processor

/** [[NodeProcessor]] */

trait SingleInputProcessor[I] extends Processor {
    val pullFrom: collection.mutable.ArrayBuffer[SubscriptionTo[I]]
}

trait SingleOutputProcessor[O] extends Processor {
    val pushTo: collection.mutable.ArrayBuffer[SubscriptionFrom[O]]
}

final case class NodeProcessor[I, O](node: Node[I, O], id: Int)
    extends SingleInputProcessor[I] with SingleOutputProcessor[O] {

    override def toString: String = s"Node[${this.id}]"

    val pullFrom = collection.mutable.ArrayBuffer.empty[SubscriptionTo[I]]

    val pushTo = collection.mutable.ArrayBuffer.empty[SubscriptionFrom[O]]
}

/** [[Splitter]] */

final case class Splitter[O, Os](out: Out[Os])
    extends SingleInputProcessor[Os] {

    override def toString: String = s"${this.out}.split"

    def ensureIdx(idx: Int) = {
        if (pushTos.length < idx + 1) {
            pushTos
                .append(
                    Seq.fill(idx + 1 - pushTos.length)
                    (ArrayBuffer.empty[SubscriptionFrom[O]]):_*
                )
        }
    }

    val pullFrom = ArrayBuffer.empty[SubscriptionTo[Os]]

    val pushTos = ArrayBuffer.empty[ArrayBuffer[SubscriptionFrom[O]]]
}

/** [[Joiner]] */

final case class Joiner[I, Is](in: In[Is]) extends SingleOutputProcessor[Is] {

    override def toString: String = s"${this.in}.join"

    def ensureIdx(idx: Int) = {
        if (this.pullFroms.length < idx + 1) {
            this.pullFroms
                .append(
                    Seq.fill(idx + 1 - pullFroms.length)
                    (ArrayBuffer.empty[SubscriptionTo[I]]):_*
                )
        }
    }

    val pullFroms = ArrayBuffer.empty[ArrayBuffer[SubscriptionTo[I]]]

    val pushTo = ArrayBuffer.empty[SubscriptionFrom[Is]]
}

/** [[HSplitter]] */

trait HSplitterOH[OH] extends Processor {
    val pushToHd: ArrayBuffer[SubscriptionFrom[OH]]
}

trait HSplitterOT[OT <: HList] extends Processor {
    val pushToTl: ArrayBuffer[SubscriptionFrom[OT]]
}

final case class HSplitter[OH, OT <: HList, Os <: HList]
(out: Out[Os])
(implicit o: Os <:< (OH :: OT))
    extends HSplitterOH[OH] with HSplitterOT[OT] with SingleInputProcessor[Os] {

    override def toString = s"${this.out}.split"

    val pullFrom = ArrayBuffer.empty[SubscriptionTo[Os]]

    val pushToHd = ArrayBuffer.empty[SubscriptionFrom[OH]]
    val pushToTl = ArrayBuffer.empty[SubscriptionFrom[OT]]
}

/** [[HJoiner]]
  *
  *     +-------------+
  * --> |             |
  *     | [[HJoiner]] | -->
  * --> |             |
  *     +-------------+
  * */

trait HJoinerIH[IH] extends Processor {
    val pullFromHd: ArrayBuffer[SubscriptionTo[IH]]
}

trait HJoinerIT[IT <: HList] extends Processor {
    val pullFromTl: ArrayBuffer[SubscriptionTo[IT]]
}

final case class HJoiner[IH, IT <: HList, Is <: HList]
(in: In[Is])
(implicit i: Is <:< (IH :: IT))
    extends HJoinerIH[IH] with HJoinerIT[IT] with SingleOutputProcessor[Is] {
    override def toString = s"${this.in}.join"

    val pullFromHd = ArrayBuffer.empty[SubscriptionTo[IH]]
    val pullFromTl = ArrayBuffer.empty[SubscriptionTo[IT]]

    val pushTo = ArrayBuffer.empty[SubscriptionFrom[Is]]
}