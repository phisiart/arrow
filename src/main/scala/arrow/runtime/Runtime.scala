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

import akka.pattern._
import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._
import arrow.repr._
import arrow.runtime.MasterWorkerProtocol._
import com.typesafe.config.ConfigFactory
import shapeless._

import scala.concurrent.Await

sealed trait ProcessorInfo

final class SourceInfo[O] extends ProcessorInfo

final class SingleInputInfo[I]
(recorder: Option[Recorder],
 replayer: Option[Replayer]) extends ProcessorInfo {

    val inputChan: Channel[I] = new Channel[I](recorder, replayer)
}

final class JoinerInfo[I, Is]
(numInputs: Int,
 recorder: Option[Recorder],
 replayer: Option[Replayer]) extends ProcessorInfo {

    val inputChans: IndexedSeq[Channel[I]] =
        IndexedSeq.fill(this.numInputs)(new Channel[I](recorder, replayer))
}

final class HJoinerInfo[IH, IT <: HList, Is <: HList]
(recorder: Option[Recorder],
 replayer: Option[Replayer])extends ProcessorInfo {

    val hdInputChan: Channel[IH] = new Channel[IH](recorder, replayer)
    val tlInputChan: Channel[IT] = new Channel[IT](recorder, replayer)
}

class MyArrowWorker(val repr: Repr, masterPath: ActorPath)
    extends AbstractWorker(masterPath) {

    override def work(task: AbstractTask): Any = {
        task match {
            case Task(id, input) =>
                this.repr.processors(id) match {
                    case NodeProcessor(node, _) =>
                        node.apply(input)

                    case _ =>
                        ???
                }
        }
    }
}

class Runtime[RetType]
(val repr: Repr,
 val drainProcessor: DrainProcessor[RetType],
 doRecord: Boolean = true,
 replay: Option[BufferedIterator[Int]] = None,
 val masterHostPort: Option[String] = None
) {
    private val log = Logger.getLogger("runtime")
    log.setLevel(Level.ALL)

    private val recorder = if (doRecord) Some(new Recorder) else None
    private val replayer = replay.map(new Replayer(_))

    private val master = masterHostPort.map(hostPort => {
        implicit val timeout = Timeout(100 seconds)

        val system = Runtime.createRemoteSystem("127.0.0.1", "0")

        val path = ActorPath.fromString(s"akka.tcp://arrow@$hostPort/user/master")

        Await.result(system.actorSelection(path).resolveOne(), 100 seconds)
    })

    // Create all the channels
    val processorInfos2: IndexedSeq[ProcessorInfo] = {
        repr.processors
            .map(_.Visit(ProcessorInfoCreator))
    }

    def processorInfos(processor: Processor): ProcessorInfo = {
        this.processorInfos2(processor.id)
    }

    val executor: ExecutorService = Executors.newCachedThreadPool()

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
            new SingleInputInfo[T](recorder, replayer)
        }

        override def VisitNodeProcessor[I, O](processor: NodeProcessor[I, O]): ProcessorInfo = {
            log.info("Created SingleInputInfo for NodeProcessor")
            new SingleInputInfo[I](recorder, replayer)
        }

        override def VisitSplitter[O, Os](processor: Splitter[O, Os]): ProcessorInfo = {
            log.info("Created SingleInputInfo for Splitter")
            new SingleInputInfo[O](recorder, replayer)
        }

        override def VisitJoiner[I, Is, S[_]](processor: Joiner[I, Is, S]): ProcessorInfo = {
            log.info("Created JoinerInfo")
            new JoinerInfo[I, Is](processor.pullFroms.length, recorder, replayer)
        }

        override def VisitHSplitter[OH, OT <: HList, Os <: HList]
        (processor: HSplitter[OH, OT, Os]): ProcessorInfo = {
            log.info("Created HSplitterInfo")
            new SingleInputInfo[Os](recorder, replayer)
        }

        override def VisitHJoiner[IH, IT <: HList, Is <: HList]
        (processor: HJoiner[IH, IT, Is]): ProcessorInfo = {
            log.info("Created HJoinerInfo")
            new HJoinerInfo[IH, IT, Is](recorder, replayer)
        }
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

            val runnable = SourceProcessorRunnable[T](processor.id, processor.stream, outputChannels)

            executor.submit(runnable)

            None
        }

        override def VisitDrainProcessor[T](processor: DrainProcessor[T]): Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo(processor).inputChan

            val runnable = DrainProcessorCallable[T](processor.id, inputChan)

            log.info("Created DrainProcessorCallable")

            Some(executor.submit(runnable).asInstanceOf[Future[IndexedSeq[Type]]])
        }

        override def VisitNodeProcessor[I, O](processor: NodeProcessor[I, O]): Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo[I](processor).inputChan

            val outputChannels = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = master match {
                case Some(m) =>
                    DistributedNodeRunnable(processor.id, processor.node, inputChan, outputChannels, m)

                case None =>
                    LocalNodeRunnable(processor.id, processor.node, inputChan, outputChannels)
            }
//            val runnable = LocalNodeRunnable(processor.id, processor.node, inputChan, outputChannels)

            log.info("Created NodeRunnable")

            executor.submit(runnable)

            None
        }

        override def VisitSplitter[O, Os](processor: Splitter[O, Os])
        : Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo(processor).inputChan

            val outputChans = processor
                .pushTos
                .map(_.map(_.Visit(SubscriptionFromToChannelIn)))

            val runnable = SplitterRunnable[O, Os](processor.id, inputChan, outputChans)(processor.os)

            log.info("Created SplitterRunnable")

            executor.submit(runnable)

            None
        }

        override def VisitJoiner[I, Is, S[_]](processor: Joiner[I, Is, S])
        : Option[Future[IndexedSeq[Type]]] = {
            val inputChans = getJoinerInfo(processor).inputChans

            val outputChans = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = JoinerRunnable[I, Is, S](
                processor.id, inputChans, outputChans
            )(processor.is, processor.cbf)

            log.info("Created JoinerRunnable")

            executor.submit(runnable)

            None
        }

        override def VisitHSplitter[OH, OT <: HList, Os <: HList]
        (processor: HSplitter[OH, OT, Os])
        : Option[Future[IndexedSeq[Type]]] = {
            val inputChan = getSingleInputInfo(processor).inputChan

            val hOutputChans = processor
                .pushToHd.map(_.Visit(SubscriptionFromToChannelIn))
            val tOutputChans = processor
                .pushToTl.map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = HSplitterRunnable[OH, OT, Os](
                processor.id, inputChan, hOutputChans, tOutputChans
            )(processor.o)

            log.info("Created HSplitterRunnable")

            executor.submit(runnable)

            None
        }

        override def VisitHJoiner[IH, IT <: HList, Is <: HList]
        (processor: HJoiner[IH, IT, Is])
        : Option[Future[IndexedSeq[Type]]] = {
            val hJoinerInfo = getHJoinerInfo(processor)

            val hInputChan = hJoinerInfo.hdInputChan
            val tInputChan = hJoinerInfo.tlInputChan

            val outputChans = processor
                .pushTo
                .map(_.Visit(SubscriptionFromToChannelIn))

            val runnable = HJoinerRunnable(processor.id, hInputChan, tInputChan, outputChans)(processor.i)

            log.info("Created HJoinerRunnable")

            executor.submit(runnable)

            None
        }
    }

    def record(): Future[(IndexedSeq[RetType], Seq[Int])] = {
        val future = repr
            .processors
            .map(_.Visit(new CallableCreator[RetType]))
            .find(_.isDefined)
            .get
            .get

        val ret = executor.submit(new Callable[(IndexedSeq[RetType], Seq[Int])] {
            override def call(): (IndexedSeq[RetType], Seq[Int]) = {
                val ret = future.get()
                (ret, recorder.get.record)
            }
        })

        executor.shutdown()

        ret
    }

    def run(): Future[IndexedSeq[RetType]] = {
        val future = repr
            .processors
            .map(_.Visit(new CallableCreator[RetType]))
            .find(_.isDefined)
            .get
            .get

        executor.shutdown()

        future
    }
}

object Runtime {
    val BUF_SIZE = 1
    val log: Logger = Logger.getLogger("runtime")
    log.setLevel(Level.ALL)

    def createRemoteSystem(host: String, port: String): ActorSystem = {
        val configString = s"""
          akka {
            loglevel = "INFO"
            actor {
              provider = remote
            }
            remote {
              enabled-transports = ["akka.remote.netty.tcp"]
              netty.tcp {
                hostname = "$host"
                port = $port
              }
              log-sent-messages = on
              log-received-messages = on
            }
          }"""

        val config = ConfigFactory.parseString(configString)

        ActorSystem("arrow", config)
    }
}
