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

package arrow_test.repr_test

import arrow._
import org.apache.spark.rdd.RDD
import shapeless._

object OneToOneTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: RDD[Int]) => x
        val g = (y: RDD[Int]) => y

        Stream[RDD[Int]]() |> f |> g

        println()
    }
}

object OneToOneRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => Value(x)
        val g = (y: Int) => Value(y)

        f |> g

        println()
    }
}

object BroadcastTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x
        val g = (y: Int) => y
        val gs = List(g, g, g)

        f |> gs

        println()
    }
}

object BroadcastRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => Value(x)
        val g = (y: Int) => y
        val gs = List(g, g, g)

        f |> gs

        println()
    }
}

object MergeTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x
        val fs = List(f, f, f)
        val g = (y: Int) => y

        fs |> g

        println()
    }
}

object HSplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => 1 :: 3.0 :: HNil

        val g0 = identity[Int] _
        val g1 = identity[Double] _
        val gs = g0 :: g1 :: HNil

        f |> gs
        f |> gs

        println()
    }
}

object HJoinTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f0 = (x: Int) => x
        val f1 = (x: Int) => x
        val fs = f0 :: f1 :: HNil

        val g = (xs: Int :: Int :: HNil) => 0

        fs |> g

        println()
    }
}

object SplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => List(x, x, x)

        val g = (x: Int) => x
        val gs = List(g, g, g)

        f |> gs

        println()
    }
}

object JoinTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x
        val fs = List(f, f, f)

        val g = (xs: List[Int]) => 0

        fs |> g

        println()
    }
}
