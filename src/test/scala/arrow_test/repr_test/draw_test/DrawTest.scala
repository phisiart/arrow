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

package arrow_test.repr_test.draw_test

import arrow._
import shapeless._

object OneToOneTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x
        val g = (y: Int) => y
        val h = (z: Int) => z

        f |> g |> h

        draw()
    }
}

object BroadcastTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x

        val g1 = (y: Int) => y
        val g2 = (y: Int) => y

        val gs = List(g1, g2)

        f |> gs

        draw()
    }
}

object MergeTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f1 = (x: Int) => x
        val f2 = (x: Int) => x

        val fs = List(f1, f2)

        val g = (y: Int) => y

        fs |> g

        draw()
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

        draw()
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

        draw()
    }
}

object SplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => List(x, x, x)

        val g1 = (x: Int) => x
        val g2 = (x: Int) => x
        val g3 = (x: Int) => x
        val gs = List(g1, g2, g3)

        f |> gs

        draw()
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

        draw()
    }
}