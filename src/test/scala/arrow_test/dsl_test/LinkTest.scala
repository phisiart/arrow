/*
 * The MIT License
 *
 * Copyright (c) 2016 Zhixun Tan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package arrow_test.dsl_test

import arrow._
import shapeless._

object OneToOneChainTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x + 1
        val g = (x: Int) => x - 1
        val h = (x: Int) => -x

        (f |> g) |> h

        f |> (g |> h)

        (f |> g) |> (g |> h)
    }
}

object OneToOneRChainTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => Push(x)
        val g = (x: Int) => Put(x)
        val h = (x: Int) => Finish[Int]()

        (f |> g) |> h
        f |> (g |> h)
    }
}

object HSplitChainTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => 1 :: 3.0 :: HNil

        val g0 = ((_: Int) + 1) |> ((_: Int) - 1)
        val g1 = ((_: Double) + 1.0) |> ((_: Double) - 1.0)

        f |> (g0 :: g1 :: HNil)
    }
}

object HMatchChainTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val g0 = ((_: Int) + 1) |> ((_: Int) - 1)
        val g1 = ((_: Double) + 1.0) |> ((_: Double) - 1.0)

        (g0 :: g1 :: HNil) |> (g0 :: g1 :: HNil)
    }
}
