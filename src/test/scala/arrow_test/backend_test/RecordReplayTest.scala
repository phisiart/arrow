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

import arrow._

object RecordReplayTest {
    def main(args: Array[String]): Unit = {
        val myRecord = {
            val graph = new ArrowGraph
            import graph._

            val s = Stream(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            val f = Node(identity[Int])
            val g = Node(identity[Int])

            s |> f |> g

            val ret = record(g).get()
            ret._2
        }

        val myRecord2 = {
            val graph = new ArrowGraph
            import graph._

            val s = Stream(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            val f = Node(identity[Int])
            val g = Node(identity[Int])

            s |> f |> g

            val iter = myRecord.toIterator.buffered

            val ret = record(g, replay = Some(iter)).get()
            ret._2
        }

        (myRecord zip myRecord2).foreach(println)
    }
}
