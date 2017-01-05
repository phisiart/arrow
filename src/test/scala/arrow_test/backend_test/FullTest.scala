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
import shapeless._

import scala.reflect.ClassTag
import scala.reflect.api.TypeTags

object FullTest {
    def main(args: Array[String]): Unit = {
        val graph = new ArrowGraph
        import graph._

        val id = Node((x: Int) => {
            Thread.sleep(1000L)
            println(s"id($x) = $x")
            x
        })

        val succ = Node((x: Int) => {
            Thread.sleep(1000L)
            println(s"succ($x) = ${x + 1}")
            x + 1
        })

        Stream(1, 2, 3, 4, 5, 6, 7, 8, 9) |> id |> succ

        val future = run(succ)

        println("Started")

        future.get()
    }
}

object JoinerTest {
    def main(args: Array[String]): Unit = {
        val graph = new ArrowGraph
        import graph._

        val inputs = List(Stream(1, 2, 3, 4, 5), Stream(1, 2, 3, 4, 5))
        val id = Node((xs: Vector[Int]) => {
            println(s"${xs}")
            ()
        })

        inputs |> id

        val future = run(id)
    }
}

object SplitterTest {
    def main(args: Array[String]): Unit = {
        val graph = new ArrowGraph
        import graph._

        val splitter = Node((x: Int) => {
            List(x, x)
        })

        val id0 = Node((x: Int) => {
            println(s"$x")
            x
        })
        val id1 = Node(identity[Int])

        val ids = Vector(id0, id1)

        Stream(1, 2, 3, 4, 5) |> splitter |> ids

        val future = run(id0)
    }
}

object HJoinerTest {
    def main(args: Array[String]): Unit = {
        val graph = new ArrowGraph
        import graph._

        val inputs = Stream(1, 2, 3, 4, 5) :: Stream(1f, 2f, 3f, 4f, 5f) :: HNil

        val hJoiner = Node((xs: Int :: Float :: HNil) => {
            println(s"$xs")
            ()
        })

        inputs |> hJoiner

        val future = run(hJoiner)
    }
}

object StupidHListTest {
    def main(args: Array[String]): Unit = {
        final class Fuck[T]

        def Rebuild[HL <: HList, H, T <: HList]
        (hlist: HL)
        (implicit ev: HL <:< (H :: T))
        : H :: T = {
            val hd = hlist.head
            val tl = hlist.tail
            val lst = hd :: tl
            lst
        }

        var original = 1 :: 2f :: HNil

        val testRebuild1 = Rebuild(original)
        original = testRebuild1

        def Rebuilg2[H, T <: HList]
        (f: (H :: T) => Unit, x: H :: T)
        : Unit = {
            val hd = x.head
            val tl = x.tail
            f(hd :: tl)
        }

        val testCall = Rebuilg2((x: Float :: HNil) => (), 1f :: HNil)

        implicit def genFuck[HL <: HList, H, T <: HList]
        (implicit
         ev: HL <:< ::[H, T],
         mani: Manifest[HL],
         tag: ClassTag[HL]
        )
        : Fuck[HL] = {
            println(mani)
            new Fuck[HL]
        }

        val imp = implicitly[Fuck[String :: HNil]]
    }
}