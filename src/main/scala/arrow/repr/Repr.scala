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
import collection.mutable.ArrayBuffer

class Repr {
    def insertSubscription(subscription: Subscription) = {
        subscriptions += subscription
        // subscriptions(subscription) = true
    }

    def draw() = {
        GraphDrawer.draw(this.processors, this.subscriptions)
    }

    val subscriptions = collection.mutable.ArrayBuffer.empty[Subscription]

    private val nodeToId = collection.mutable.Map.empty[NodeUntyped, Int]
    private val processors = ArrayBuffer.empty[Processor]

    /**
      * Given a node, ensure it is recorded in the [[Repr]], and return the
      * [[Processor]].
      */
    private def nodeToProcessor[I, O](node: Node[I, O])
    : NodeProcessor[I, O] = {
        if (this.nodeToId.contains(node)) {
            this.processors(this.nodeToId(node))
                .asInstanceOf[NodeProcessor[I, O]]

        } else {
            val id = this.processors.length
            this.nodeToId(node) = id
            val processor = new NodeProcessor(node, id)
            this.processors.append(processor)
            processor
        }
    }

    private def functionToProcessor[I, O](func: I => O)
    : NodeProcessor[I, O] = {
        this.processors
            .filter(_.isInstanceOf[NodeProcessor[_, _]])
            .map(_.asInstanceOf[NodeProcessor[_, _]])
            .map(x => (x, x.node))
            .filter(_._2.isInstanceOf[FunctionNode[_, _]])
            .map(x => (x._1, x._2.asInstanceOf[FunctionNode[_, _]]))
            .find(_._2.func == func)
        match {
            case Some((nodeProcessor, functionNode)) =>
                nodeProcessor.asInstanceOf[NodeProcessor[I, O]]

            case _ =>
                val node = FunctionNode(func)
                this.nodeToProcessor(node)
        }
    }

    /**
      * Will create subscription and record it.
      */
    def insertSubscription[T](out: Out[T], in: In[T]) = {
        val subscription = new SubscriptionImpl[T](out, in)
        this.subscriptions += subscription

        out.addSubscription(subscription)
        in.addSubscription(subscription)
    }

    /** ========================================================================
      * [[In]] factories
      * ========================================================================
      */
    def makeFunctionIn[I, O](func: I => O): SingleInputProcessorIn[I] = {
        val processor = functionToProcessor(func)
        new SingleInputProcessorIn[I](processor)
    }

    /**
      * Will insert node if not already in the record.
      */
    def makeNodeIn[I, O](node: Node[I, O]): SingleInputProcessorIn[I] = {
        val processor = nodeToProcessor(node)
        new SingleInputProcessorIn[I](processor)
    }

    /**
      * Given an [[In]]<[[IH]] :: [[IT]]>, construct an [[In]]<[[IH]]>
      */
    def makeHJoinerHdIn[IH, IT <: HList, I <: HList]
    (in: In[I])
    (implicit i: I <:< (IH :: IT))
    : HJoinerHdIn[IH, IT, I] = {
        in
            .pullFrom
            .filter(_.isInstanceOf[SubscriptionImpl[I]])
            .map(_.asInstanceOf[SubscriptionImpl[I]])

            /** [[SubscriptionImpl]]<[[I]]> */
            .map(_.from)

            /** [[Out]]<[[I]]> */
            .filter(_.isInstanceOf[SingleOutputProcessorOut[I]])
            .map(_.asInstanceOf[SingleOutputProcessorOut[I]])
            .map(_.processor)

            /** [[SingleOutputProcessor]]<[[I]]> */
            .find(_.isInstanceOf[HJoiner[_, _, I]])
        match {
            case Some(x) =>
                val hJoiner = x.asInstanceOf[HJoiner[IH, IT, I]]
                new HJoinerHdIn(hJoiner)

            case None =>
                // Create a new HJoiner and attach to the input.
                val hJoiner = new HJoiner[IH, IT, I](in)
                this.processors.append(hJoiner)

                val hJoinerOut = new SingleOutputProcessorOut(hJoiner)
                this.insertSubscription(hJoinerOut, in)

                new HJoinerHdIn(hJoiner)
        }
    }

    /**
      * Given an [[In]] of [[IH]] :: [[IT]], construct an [[In]] of [[IT]]
      */
    def makeHJoinerTlIn[IH, IT <: HList, I <: HList]
    (in: In[I])
    (implicit i: I <:< (IH :: IT))
    : HJoinerTlIn[IH, IT, I] = {
        in
            .pullFrom
            .filter(_.isInstanceOf[SubscriptionImpl[I]])
            .map(_.asInstanceOf[SubscriptionImpl[I]])

            /** [[SubscriptionImpl]]<[[I]]> */
            .map(_.from)

            /** [[Out]]<[[I]]> */
            .filter(_.isInstanceOf[SingleOutputProcessorOut[I]])
            .map(_.asInstanceOf[SingleOutputProcessorOut[I]])
            .map(_.processor)

            /** [[SingleOutputProcessor]]<[[I]]> */
            .find(_.isInstanceOf[HJoiner[_, _, I]])
        match {
            case Some(x) =>
                val hJoiner = x.asInstanceOf[HJoiner[IH, IT, I]]
                new HJoinerTlIn(hJoiner)

            case None =>
                // Create a new HJoiner and attach to the input.
                val hJoiner = new HJoiner[IH, IT, I](in)
                this.processors.append(hJoiner)

                val hJoinerOut = new SingleOutputProcessorOut(hJoiner)
                this.insertSubscription(hJoinerOut, in)

                new HJoinerTlIn(hJoiner)
        }
    }

    def makeJoinerIn[I, Is]
    (in: In[Is], idx: Int)
    (implicit is: Is <:< Traversable[I])
    : JoinerIn[I, Is] = {
        in
            .pullFrom
            .filter(_.isInstanceOf[SubscriptionImpl[Is]])
            .map(_.asInstanceOf[SubscriptionImpl[Is]])
            .map(_.from)
            .filter(_.isInstanceOf[SingleOutputProcessorOut[Is]])
            .map(_.asInstanceOf[SingleOutputProcessorOut[Is]])
            .map(_.processor)
            .find(_.isInstanceOf[Joiner[_, Is]])
        match {
            case Some(x) =>
                val joiner = x.asInstanceOf[Joiner[I, Is]]
                new JoinerIn[I, Is](joiner, idx)

            case None =>
                val joiner = new Joiner[I, Is](in)
                this.processors.append(joiner)
                val joinerOut = new SingleOutputProcessorOut(joiner)
                this.insertSubscription(joinerOut, in)
                new JoinerIn(joiner, idx)
        }
    }

    /** ========================================================================
      * [[Out]] factories
      * ========================================================================
      */

    def makeNodeOut[I, O](node: Node[I, O]): SingleOutputProcessorOut[O] = {
        val processor = nodeToProcessor(node)
        new SingleOutputProcessorOut[O](processor)
    }

    def makeFunctionOut[I, O](func: I => O): SingleOutputProcessorOut[O] = {
        val processor = this.functionToProcessor(func)
        new SingleOutputProcessorOut[O](processor)
    }

    def makeHSplitterHdOut[OH, OT <: HList, O <: HList]
    (out: Out[O])
    (implicit o: O <:< (OH :: OT))
    : HSplitterHdOut[OH, OT, O] = {
        out
            .pushTo
            .filter(_.isInstanceOf[SubscriptionImpl[O]])
            .map(_.asInstanceOf[SubscriptionImpl[O]])
            .map(_.to)
            .filter(_.isInstanceOf[SingleInputProcessorIn[O]])
            .map(_.asInstanceOf[SingleInputProcessorIn[O]])
            .map(_.processor)
            .find(_.isInstanceOf[HSplitter[_, _, O]])
        match {
            case Some(x) =>
                val hSplitter = x.asInstanceOf[HSplitter[OH, OT, O]]
                new HSplitterHdOut(hSplitter)

            case None =>
                val hSplitter = new HSplitter[OH, OT, O](out)
                this.processors.append(hSplitter)
                val hSplitterIn = new SingleInputProcessorIn(hSplitter)
                this.insertSubscription(out, hSplitterIn)
                new HSplitterHdOut(hSplitter)
        }
    }

    def makeHSplitterTlOut[OH, OT <: HList, O <: HList]
    (out: Out[O])
    (implicit o: O <:< (OH :: OT))
    : HSplitterTlOut[OH, OT, O] = {
        out
            .pushTo
            .filter(_.isInstanceOf[SubscriptionImpl[O]])
            .map(_.asInstanceOf[SubscriptionImpl[O]])
            .map(_.to)
            .filter(_.isInstanceOf[SingleInputProcessorIn[O]])
            .map(_.asInstanceOf[SingleInputProcessorIn[O]])
            .map(_.processor)
            .find(_.isInstanceOf[HSplitter[_, _, O]])
        match {
            case Some(x) =>
                val hSplitter = x.asInstanceOf[HSplitter[OH, OT, O]]
                new HSplitterTlOut(hSplitter)

            case None =>
                val hSplitter = new HSplitter[OH, OT, O](out)
                this.processors.append(hSplitter)
                val hSplitterIn = new SingleInputProcessorIn(hSplitter)
                this.insertSubscription(out, hSplitterIn)
                new HSplitterTlOut(hSplitter)
        }
    }

    def makeSplitterOut[O, Os]
    (out: Out[Os], idx: Int)
    (implicit os: Os <:< Traversable[O])
    : SplitterOut[O, Os] = {
        out
            .pushTo
            .filter(_.isInstanceOf[SubscriptionImpl[Os]])
            .map(_.asInstanceOf[SubscriptionImpl[Os]])
            .map(_.to)
            .filter(_.isInstanceOf[SingleInputProcessorIn[Os]])
            .map(_.asInstanceOf[SingleInputProcessorIn[Os]])
            .map(_.processor)
            .find(_.isInstanceOf[Splitter[_, Os]])
        match {
            case Some(x) =>
                val splitter = x.asInstanceOf[Splitter[O, Os]]
                new SplitterOut(splitter, idx)

            case None =>
                val splitter = new Splitter[O, Os](out)
                this.processors.append(splitter)
                val splitterIn = new SingleInputProcessorIn(splitter)
                this.insertSubscription(out, splitterIn)
                new SplitterOut(splitter, idx)
        }
    }
}

object Repr {
}