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

package arrow_test.backend_test

import java.util.concurrent.Executors

import arrow._
import arrow.runtime._

object RunnableTests {
    def main(args: Array[String]): Unit = {
        val inputStream = Stream(1, 2, 3, 4)
        val inputChan = new Channel[Int]
        val inputRunnable = SourceProcessorRunnable(
            inputStream,
            IndexedSeq(ChannelInImpl(inputChan))
        )
        val succNode = new Node[Int, Int] {
            override def apply(input: Int): Int = input + 1
        }
        val outputChan = new Channel[Int]
        val succProcessorRunnable = NodeRunnable(
            succNode,
            inputChan,
            IndexedSeq(ChannelInImpl(outputChan))
        )
        val drainCallable = DrainProcessorCallable(
            outputChan
        )

        val pool = Executors.newFixedThreadPool(4)
        pool.submit(inputRunnable)
        pool.submit(succProcessorRunnable)
        val outputFuture = pool.submit(drainCallable)

        pool.shutdown()

        val output = outputFuture.get()
        println(output)
    }
}
