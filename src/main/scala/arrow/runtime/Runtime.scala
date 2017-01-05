/*
 * The MIT License
 *
 * Copyright (c) 2017 Zhixun Tan
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

import java.util.concurrent._
import java.util.logging.{Level, Logger}

import arrow.repr._
import shapeless._


sealed trait ProcessorInfo

final class SourceInfo[O] extends ProcessorInfo

final class SingleInputInfo[I] extends ProcessorInfo {
    val inputChan: Channel[I] = new Channel[I]
}

final class JoinerInfo[I, Is](numInputs: Int) extends ProcessorInfo {
    val inputChans: IndexedSeq[Channel[I]] =
        IndexedSeq.fill(this.numInputs)(new Channel[I])
}

final class HJoinerInfo[IH, IT <: HList, Is <: HList] extends ProcessorInfo {
    val hdInputChan: Channel[IH] = new Channel[IH]
    val tlInputChan: Channel[IT] = new Channel[IT]
}

class Runtime[RetType](val repr: Repr, val drainProcessor: DrainProcessor[RetType]) {
    private val log = Logger.getLogger("runtime")
    log.setLevel(Level.ALL)

    // Create all the channels
    val processorInfos: Map[Processor, ProcessorInfo] = {
        repr.processors
            .map(processor => (processor, processor.Visit(ProcessorInfoCreator)))
            .toMap
    }

    val pool: ExecutorService = Executors.newCachedThreadPool()

    def getSourceInfo[O](processor: SourceProcessor[O]): SourceInfo[O] =
        this.processorInfos(processor).asInstanceOf[SourceInfo[O]]

    def getSingleInputInfo[I](processor: SingleInputProcessor[I]): SingleInputInfo[I] =
        this.processorInfos(processor).asInstanceOf[SingleInputInfo[I]]

    def getJoinerInfo[I, Is, S[_]](processor: Joiner[I, Is, S]): JoinerInfo[I, Is] =
        this.processorInfos(processor).asInstanceOf[JoinerInfo[I, Is]]

    def getHJoinerInfo[IH, IT <: HList, Is <: HList](processor: HJoiner[IH, IT, Is]): HJoinerInfo[IH, IT, Is] =
        this.processorInfos(processor).asInstanceOf[HJoinerInfo[IH, IT, Is]]

    object ProcessorInfoCreator extends ProcessorVisitor[ProcessorInfo] {
        override def VisitSourceProcessor[T](processor: SourceProcessor[T]): ProcessorInfo = {
            log.info("Created SourceInfo")
            new SourceInfo[T]
        }

        override def VisitDrainProcessor[T](processor: DrainProcessor[T]): ProcessorInfo = {
            log.info("Created SingleInputInfo for DrainProcessor")
            new SingleInputInfo[T]
        }

        override def VisitNodeProcessor[I, O](processor: NodeProcessor[I, O]): ProcessorInfo = {
            log.info("Created SingleInputInfo for NodeProcessor")
            new SingleInputInfo[I]
        }

        override def VisitSplitter[O, Os](processor: Splitter[O, Os]): ProcessorInfo =
            new SingleInputInfo[O]

        override def VisitJoiner[I, Is, S[_]](processor: Joiner[I, Is, S]): ProcessorInfo =
            new JoinerInfo[I, Is](processor.pullFroms.length)

        override def VisitHSplitter[OH, OT <: HList, Os <: HList](processor: HSplitter[OH, OT, Os]): ProcessorInfo =
            new SingleInputInfo[Os]

        override def VisitHJoiner[IH, IT <: HList, Is <: HList](processor: HJoiner[IH, IT, Is]): ProcessorInfo =
            new HJoinerInfo[IH, IT, Is]
    }

    object InToChannel extends InVisitor[Channel] {
        override def VisitSingleInputProcessorIn[I](in: SingleInputProcessorIn[I]): Channel[I] =
            getSingleInputInfo(in.processor).inputChan

        override def VisitHJoinerHdIn[IH, IT <: HList, I <: HList](in: HJoinerHdIn[IH, IT, I]): Channel[IH] =
            getHJoinerInfo(in.hJoiner).hdInputChan

        override def VisitHJoinerTlIn[IH, IT <: HList, I <: HList](in: HJoinerTlIn[IH, IT, I]): Channel[IT] =
            getHJoinerInfo(in.hJoiner).tlInputChan

        override def VisitJoinerIn[I, Is, S[_]](in: JoinerIn[I, Is, S]): Channel[I] =
            getJoinerInfo(in.joiner).inputChans(in.idx)
    }

    object SubscriptionFromToChannelIn extends SubscriptionFromVisitor[ChannelIn] {
        override def VisitSubscriptionImpl[T](subscription: SubscriptionImpl[T]): ChannelIn[T] =
            ChannelInImpl(subscription.to.visit(InToChannel))

        override def VisitSubscriptionRImpl[RT, T](subscription: SubscriptionRImpl[RT, T]): ChannelIn[RT] =
            ChannelInRImpl[T, RT](subscription.to.visit(InToChannel))(subscription.rT)
    }

    class CallableCreator[Type] extends ProcessorVisitor[Option[Future[IndexedSeq[Type]]]] {
        override def VisitSourceProcessor[T](processor: SourceProcessor[T]): Option[Future[IndexedSeq[Type]]] = {
            val outputChannels = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            log.info("Created SourceProcessorRunnable")

            val runnable = SourceProcessorRunnable[T](processor.stream, outputChannels)

            pool.submit(runnable)

            None
        }

        override def VisitDrainProcessor[T](processor: DrainProcessor[T]): Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo(processor).inputChan

            val runnable = DrainProcessorCallable[T](inputChan)

            log.info("Created DrainProcessorCallable")

            Some(pool.submit(runnable).asInstanceOf[Future[IndexedSeq[Type]]])
        }

        override def VisitNodeProcessor[I, O](processor: NodeProcessor[I, O]): Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo[I](processor).inputChan

            val outputChannels = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = NodeRunnable(processor.node, inputChan, outputChannels)

            log.info("Created NodeRunnable")

            pool.submit(runnable)

            None
        }

        override def VisitSplitter[O, Os](processor: Splitter[O, Os])
        : Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo(processor).inputChan

            val outputChans = processor
                .pushTos
                .map(_.map(_.Visit(SubscriptionFromToChannelIn)))

            val runnable = SplitterRunnable[O, Os](inputChan, outputChans)(processor.os)

            log.info("Created SplitterRunnable")

            pool.submit(runnable)

            None
        }

        override def VisitJoiner[I, Is, S[_]](processor: Joiner[I, Is, S])
        : Option[Future[IndexedSeq[Type]]] = {
            val inputChans = getJoinerInfo(processor).inputChans

            val outputChans = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = JoinerRunnable[I, Is, S](inputChans, outputChans)(processor.is, processor.cbf)

            log.info("Created JoinerRunnable")

            pool.submit(runnable)

            None
        }

        override def VisitHSplitter[OH, OT <: HList, Os <: HList](processor: HSplitter[OH, OT, Os]): Option[Future[IndexedSeq[Type]]] = ???

        override def VisitHJoiner[IH, IT <: HList, Is <: HList]
        (processor: HJoiner[IH, IT, Is])
        : Option[Future[IndexedSeq[Type]]] = {
            ???
        }
    }

    def run(): Future[IndexedSeq[RetType]] = {
        val future = repr
            .processors
            .map(_.Visit(new CallableCreator[RetType]))
            .find(_.isDefined)
            .get
            .get

        pool.shutdown()

        future
    }
}
