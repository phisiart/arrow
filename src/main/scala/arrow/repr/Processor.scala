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

import arrow._
import shapeless._

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer

/**
  * Every processor will be mapped to a runnable/callable in the backend.
  */
trait Processor {
    def Visit[R](visitor: ProcessorVisitor[R]): R
    val id: Int
}

trait ProcessorVisitor[R] {
    def VisitSourceProcessor[T](processor: SourceProcessor[T]): R
    def VisitDrainProcessor[T](processor: DrainProcessor[T]): R
    def VisitNodeProcessor[I, O](processor: NodeProcessor[I, O]): R
    def VisitSplitter[O, Os](processor: Splitter[O, Os]): R
    def VisitJoiner[I, Is, S[_]](processor: Joiner[I, Is, S]): R
    def VisitHSplitter[OH, OT <: HList, Os <: HList](processor: HSplitter[OH, OT, Os]): R
    def VisitHJoiner[IH, IT <: HList, Is <: HList](processor: HJoiner[IH, IT, Is]): R
}

sealed trait SingleInputProcessor[I] extends Processor {
    val pullFrom: collection.mutable.ArrayBuffer[SubscriptionTo[I]]
}

sealed trait SingleOutputProcessor[O] extends Processor {
    val pushTo: collection.mutable.ArrayBuffer[SubscriptionFrom[O]]
}

/**
  * [[SourceProcessor]]
  */

final case class SourceProcessor[T](stream: Stream[T], id: Int)
    extends SingleOutputProcessor[T] {

    override val pushTo: ArrayBuffer[SubscriptionFrom[T]] =
        collection.mutable.ArrayBuffer.empty[SubscriptionFrom[T]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitSourceProcessor(this)
}

/**
  * [[DrainProcessor]]
  */
final case class DrainProcessor[T](id: Int) extends SingleInputProcessor[T] {
    override val pullFrom: ArrayBuffer[SubscriptionTo[T]] =
        collection.mutable.ArrayBuffer.empty[SubscriptionTo[T]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitDrainProcessor(this)
}

/**
  * [[NodeProcessor]]
  */

final case class NodeProcessor[I, O](node: Node[I, O], id: Int)
    extends SingleInputProcessor[I] with SingleOutputProcessor[O] {

    override def toString: String = s"Node[${this.id}]"

    override val pullFrom: ArrayBuffer[SubscriptionTo[I]] =
        collection.mutable.ArrayBuffer.empty[SubscriptionTo[I]]

    override val pushTo: ArrayBuffer[SubscriptionFrom[O]] =
        collection.mutable.ArrayBuffer.empty[SubscriptionFrom[O]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitNodeProcessor(this)
}

/**
  * [[Splitter]]: An input, a list of outputs
  */

final case class Splitter[O, Os](out: Out[Os], id: Int)
                                (implicit val os: Os <:< Seq[O])
    extends SingleInputProcessor[Os] {

    override def toString: String = s"${this.out}.split"

    def ensureIdx(idx: Int): Unit = {
        if (pushTos.length < idx + 1) {
            pushTos
                .append(
                    Seq.fill(idx + 1 - pushTos.length)
                    (ArrayBuffer.empty[SubscriptionFrom[O]]):_*
                )
        }
    }

    override val pullFrom: ArrayBuffer[SubscriptionTo[Os]] =
        ArrayBuffer.empty[SubscriptionTo[Os]]

    val pushTos: ArrayBuffer[ArrayBuffer[SubscriptionFrom[O]]] =
        ArrayBuffer.empty[ArrayBuffer[SubscriptionFrom[O]]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitSplitter(this)
}

/** [[Joiner]] */

final case class Joiner[I, Is, S[_]]
(in: In[Is], id: Int)
(implicit val is: S[I] =:= Is, val cbf: CanBuildFrom[Nothing, I, S[I]])
    extends SingleOutputProcessor[Is] {

    override def toString: String = s"${this.in}.join"

    def ensureIdx(idx: Int): Unit = {
        if (this.pullFroms.length < idx + 1) {
            this.pullFroms
                .append(
                    Seq.fill(idx + 1 - pullFroms.length)
                    (ArrayBuffer.empty[SubscriptionTo[I]]):_*
                )
        }
    }

    val pullFroms: ArrayBuffer[ArrayBuffer[SubscriptionTo[I]]] =
        ArrayBuffer.empty[ArrayBuffer[SubscriptionTo[I]]]

    override val pushTo: ArrayBuffer[SubscriptionFrom[Is]] =
        ArrayBuffer.empty[SubscriptionFrom[Is]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitJoiner(this)
}

/** [[HSplitter]] */

sealed trait HSplitterOH[OH] {
    val pushToHd: ArrayBuffer[SubscriptionFrom[OH]]
}

sealed trait HSplitterOT[OT <: HList] {
    val pushToTl: ArrayBuffer[SubscriptionFrom[OT]]
}

final case class HSplitter[OH, OT <: HList, Os <: HList]
(out: Out[Os], id: Int)
(implicit val o: Os <:< (OH :: OT))
    extends HSplitterOH[OH] with HSplitterOT[OT] with SingleInputProcessor[Os] {

    override def toString = s"${this.out}.split"

    override val pullFrom: ArrayBuffer[SubscriptionTo[Os]] =
        ArrayBuffer.empty[SubscriptionTo[Os]]

    override val pushToHd: ArrayBuffer[SubscriptionFrom[OH]] =
        ArrayBuffer.empty[SubscriptionFrom[OH]]

    override val pushToTl: ArrayBuffer[SubscriptionFrom[OT]] =
        ArrayBuffer.empty[SubscriptionFrom[OT]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitHSplitter(this)
}

/** [[HJoiner]]
  *
  *     +-------------+
  * --> |             |
  *     | [[HJoiner]] | -->
  * --> |             |
  *     +-------------+
  * */

sealed trait HJoinerIH[IH] {
    val pullFromHd: ArrayBuffer[SubscriptionTo[IH]]
}

sealed trait HJoinerIT[IT <: HList] {
    val pullFromTl: ArrayBuffer[SubscriptionTo[IT]]
}

final case class HJoiner[IH, IT <: HList, Is <: HList]
(in: In[Is], id: Int)
(implicit val i: (IH :: IT) <:< Is)
    extends HJoinerIH[IH] with HJoinerIT[IT] with SingleOutputProcessor[Is] {

    override def toString = s"${this.in}.join"

    override val pullFromHd: ArrayBuffer[SubscriptionTo[IH]] =
        ArrayBuffer.empty[SubscriptionTo[IH]]

    override val pullFromTl: ArrayBuffer[SubscriptionTo[IT]] =
        ArrayBuffer.empty[SubscriptionTo[IT]]

    override val pushTo: ArrayBuffer[SubscriptionFrom[Is]] =
        ArrayBuffer.empty[SubscriptionFrom[Is]]

    override def Visit[R](visitor: ProcessorVisitor[R]): R =
        visitor.VisitHJoiner(this)
}
