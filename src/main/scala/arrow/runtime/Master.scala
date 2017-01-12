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

import akka.actor._

import scala.collection.mutable

object MasterWorkerProtocol {
    final case object WorkerCreated
    final case class Result(value: Any)

    abstract class AbstractTask
    final case class Task(id: Int, input: Any) extends AbstractTask
}

class Master extends Actor with ActorLogging {
    import MasterWorkerProtocol._

    abstract class WorkerInfo
    final object Idle extends WorkerInfo
    final case class Working(sender: ActorRef, task: AbstractTask)
        extends WorkerInfo

    log.info("Master created.")

    val workers: mutable.Map[ActorRef, WorkerInfo] = mutable.Map.empty
    val tasks: mutable.Queue[(ActorRef, AbstractTask)] = mutable.Queue.empty

    def tryAssign(): Unit = {
        for (worker <- this.workers.keys) {
            if (this.tasks.isEmpty) {
                return
            }
            this.workers(worker) match {
                case Working(_, _) => // Ignore

                case Idle =>
                    val (sender, task) = this.tasks.dequeue()
                    log.info(s"Sending $task to $worker.")
                    this.workers(worker) = Working(sender, task)
                    worker ! task
            }
        }
    }

    override def receive: Receive = {
        // New worker joins.
        case WorkerCreated =>
            val worker = sender()
            log.info(s"New worker created: $worker.")

            this.workers += (worker -> Idle)
            this.context.watch(worker)
            this.tryAssign()

        // New task comes.
        case task: AbstractTask =>
            log.info(s"New task: ${sender()} -> $task.")

            this.tasks.enqueue(sender() -> task)
            this.tryAssign()

        // A task is completed.
        case Result(value) =>
            val worker = sender()
            log.info(s"New result from worker $worker.")

            if (!this.workers.contains(worker)) {
                // Ignore
            } else {
                this.workers(worker) match {
                    case Idle => // Ignore
                    case Working(taskSender, _) =>
                        this.workers(worker) = Idle
                        taskSender ! value
                }
            }

        // A worker is down.
        case Terminated(worker) =>
            if (!this.workers.contains(worker)) {
                // Ignore
            } else {
                this.workers(worker) match {
                    case Idle => // Ignore
                    case Working(sender, task) =>
                        // Resend task
                        self.tell(task, sender)
                }
                this.workers -= worker
            }
    }
}
