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

package arrow_test.dsl_test

import arrow._
import shapeless._
import shapeless.syntax.std.tuple._

object OneToOneTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Array[Int]
        val m: M = Array(0, 1, 2)
        type O = Int
        val o = 1

        val f = (_: I) => m

        val g = (_: M) => o

        val x = Node(f)

        val y = Node(g)

        implicitly[RawLinkPoly.OneToOneCase[I => M, M => O]]
        implicitly[RawLinkPoly.OneToOneCase[I => M, Node[M, O]]]
        implicitly[RawLinkPoly.OneToOneCase[Node[I, M], M => O]]
        implicitly[RawLinkPoly.OneToOneCase[Node[I, M], Node[M, O]]]

        f |> g
        f |> y
        x |> g
        x |> y
    }
}

object InputTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val source = Stream(1, 2, 3)
        val f = (x: Int) => x

        f <| f <| source
    }
}

object OneToOneRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Int
        type RM = Value[M]
        val rm = Value(1)
        type O = Int
        val o = 1

        val f = (_: I) => rm

        val g = (_: M) => o

        val x = Node(f)

        val y = Node(g)

        implicitly[RawLinkPoly.OneToOneRCase[I => RM, M => O]]
        implicitly[RawLinkPoly.OneToOneRCase[I => RM, Node[M, O]]]
        implicitly[RawLinkPoly.OneToOneRCase[Node[I, RM], M => O]]
        implicitly[RawLinkPoly.OneToOneRCase[Node[I, RM], Node[M, O]]]

        f |> g
        f |> y
        x |> g
        x |> y
    }
}

object BroadcastTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Vector[Double]
        val m = Vector.empty[Double]
        type O = Int
        val o = 1
        type S[X] = List[X]
        def s[A](elems: A*): S[A] = elems.toList

        val f = (_: I) => m
        val x = Node(f)

        val g = (_: M) => o
        val gs = s(g, g)

        val y = Node(g)
        val ys = s(y, y)

        implicitly[RawLinkPoly.BroadcastCase[I => M, S[M => O]]]
        implicitly[RawLinkPoly.BroadcastCase[I => M, S[Node[M, O]]]]
        implicitly[RawLinkPoly.BroadcastCase[Node[I, M], S[M => O]]]
        implicitly[RawLinkPoly.BroadcastCase[Node[I, M], S[Node[M, O]]]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object EvilBroadcastTest {
    def main(args: Array[String]): Unit = {
        val graph = new ArrowGraph
        import graph._

        val f = identity[Int] _
        val g = List(identity[Int] _)

        f |> g
    }
}

object BroadcastRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Array[Double]
        val m = Array.empty[Double]
        type RM = Value[M]
        def r(m: M): RM = Value(m)
        type S[X] = Vector[X]
        def s[A](elems: A*): S[A] = elems.toVector
        type O = Int
        val o = 1

        type T = Int

        val f = (_: I) => r(m)
        val x = Node(f)

        val g = (_: M) => o
        val gs = Vector(g, g)

        val y = Node(g)
        val ys = s(y, y)

        implicitly[RawLinkPoly.BroadcastCase[I => RM, S[M => O]]]
        implicitly[RawLinkPoly.BroadcastCase[Node[I, RM], S[Node[M, O]]]]
        implicitly[RawLinkPoly.BroadcastCase[I => RM, S[M => O]]]
        implicitly[RawLinkPoly.BroadcastCase[Node[I, RM], S[Node[M, O]]]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object MergeTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Int

        val fs = List(identity[T] _)

        val g = identity[T] _

        val xs = List(
            Node(identity[T])
        )

        val y = Node(g)

        implicitly[RawLinkPoly.MergeCase[List[T => T], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[T => T], Node[T, T]]]
        implicitly[RawLinkPoly.MergeCase[List[Node[T, T]], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[T => T], Node[T, T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object MergeRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Double
        val fun = (x: T) => Value(x)

        val fs = List(fun)

        val g = fun

        val xs = List(Node(fun))

        val y = Node(g)

        implicitly[RawLinkPoly.MergeCase[List[T => Value[T]], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[T => Value[T]], Node[T, T]]]
        implicitly[RawLinkPoly.MergeCase[List[Node[T, Value[T]]], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[Node[T, Value[T]]], Node[T, T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object SplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = String

        val f: T => List[T] = (x: T) => List(x, x)

        val g = identity[T] _
        val gs: Vector[T => T] = Vector(g, g)

        val x = Node(f)

        val y = Node(g)
        val ys = Seq(y, y)

        implicitly[RawLinkPoly.SplitCase[T => List[T], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[T => List[T], Vector[Node[T, T]]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[T]], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[T]], Vector[Node[T, T]]]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object SplitRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Int

        val f = (x: T) => List(Value(x), Finish[T]())

        val g = identity[T] _
        val gs = Vector(g, g)

        val x = Node(f)

        val y = Node(g)
        val ys = Seq(y, y)

        implicitly[RawLinkPoly.SplitCase[T => List[Value[T]], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[T => List[Value[T]], Vector[Node[T, T]]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[Value[T]]], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[Value[T]]], Vector[Node[T, T]]]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object JoinTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Double

        val f = identity[T] _
        val fs = List(f, f)

        val g = identity[Vector[T]] _

        val x = Node(f)
        val xs = List(x, x)

        val y = Node(g)

        implicitly[RawLinkPoly.JoinCase[List[T => T], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[T => T], Node[Vector[T], T]]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, T]], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, T]], Node[Vector[T], T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object JoinRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Double

        val f = (x: T) => Value(x)
        val fs = List(f, f)

        val g = identity[Vector[T]] _

        val x = Node(f)
        val xs = List(x, x)

        val y = Node(g)

        implicitly[RawLinkPoly.JoinCase[List[T => Value[T]], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, Value[T]]], Node[Vector[T], T]]]
        implicitly[RawLinkPoly.JoinCase[List[T => Value[T]], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, Value[T]]], Node[Vector[T], T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object MatchTest2 {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Int
        val m = 1
        type O = Int
        val o = 1
        type S[X] = Vector[X]
        def s[A](elems: A*): S[A] = elems.toVector

        val f = (_: Int) => m
        val fs = Vector(f, f)

        val x = Node(f)
        val xs = Vector(x, x)

        val g = (_: Int) => o
        val gs = Vector(g, g)

        val y = Node(g)
        val ys = s(y, y)

        implicitly[RawLinkPoly.Case[S[I => M], S[M => O]]]
        implicitly[RawLinkPoly.MatchCase[S[Node[I, M]], S[Node[M, O]]]]
        implicitly[RawLinkPoly.MatchCase[S[I => M], S[M => O]]]
        implicitly[RawLinkPoly.MatchCase[S[Node[I, M]], S[Node[M, O]]]]

        fs |> gs
        fs |> ys
        xs |> gs
        xs |> ys
    }
}

object HSplitNilTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Double
        val f = (_: T) => HNil

        val gs = HNil

        val x = Node(f)

        val ys = HNil

        implicitly[RawLinkPoly.HSplitCase[T => HNil, HNil]]
        implicitly[RawLinkPoly.HSplitCase[Node[T, HNil], HNil]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object HSplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (_: Int) => 1 :: 3.0 :: HNil

        val g0 = identity[Int] _
        val g1 = identity[Double] _
        val gs = g0 :: g1 :: HNil

        val x = Node(f)

        val y0 = Node(g0)
        val y1 = Node(g1)
        val ys = y0 :: y1 :: HNil

        implicitly[RawLinkPoly.HSplitCase[Int => (Int :: Double :: HNil), (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Node[Int, Int :: Double :: HNil], Node[Int, Int] :: Node[Double, Double] :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Int => (Int :: Double :: HNil), (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Node[Int, Int :: Double :: HNil], Node[Int, Int] :: Node[Double, Double] :: HNil]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object HSplitRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (_: Int) => Value(1) :: Value(3.0) :: HNil

        val g0 = identity[Int] _
        val g1 = identity[Double] _
        val gs = g0 :: g1 :: HNil

        val x = Node(f)

        val y0 = Node(g0)
        val y1 = Node(g1)
        val ys = y0 :: y1 :: HNil

        implicitly[RawLinkPoly.HSplitCase[Int => (R[Int] :: Double :: HNil), (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Int => (R[Int] :: Double :: HNil), Node[Int, Int] :: Node[Double, Double] :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Int => (R[Int] :: Double :: HNil), (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HSplitCase[Node[Int, R[Int] :: Double :: HNil], Node[Int, Int] :: Node[Double, Double] :: HNil]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object HJoinNil {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val fs = HNil

        val g = identity[HNil] _

        val xs = HNil

        val y = Node(g)

        implicitly[RawLinkPoly.HJoinCase[HNil, HNil => HNil]]
        implicitly[RawLinkPoly.HJoinCase[HNil, Node[HNil, HNil]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object HJoin {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f0 = identity[Int] _
        val fs = f0 :: HNil

        val g = (_: Int :: HNil) => 0

        val x0 = Node(f0)
        val xs = x0 :: HNil

        val y = Node(g)

        implicitly[RawLinkPoly.HJoinCase[(Int => Int) :: HNil, (Int :: HNil) => Int]]
        implicitly[RawLinkPoly.HJoinCase[(Int => Int) :: HNil, Node[Int :: HNil, Int]]]
        implicitly[RawLinkPoly.HJoinCase[Node[Int, Int] :: HNil, (Int :: HNil) => Int]]
        implicitly[RawLinkPoly.HJoinCase[Node[Int, Int] :: HNil, Node[Int :: HNil, Int]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object HJoinR {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f0 = (x: Int) => Value(x)
        val fs = f0 :: HNil

        val g = (_: Int :: HNil) => 0

        val x0 = Node(f0)
        val xs = x0 :: HNil

        val y = Node(g)

        implicitly[RawLinkPoly.HJoinCase[(Int => R[Int]) :: HNil, (Int :: HNil) => Int]]
        implicitly[RawLinkPoly.HJoinCase[(Int => R[Int]) :: HNil, Node[Int :: HNil, Int]]]
        implicitly[RawLinkPoly.HJoinCase[Node[Int, R[Int]] :: HNil, (Int :: HNil) => Int]]
        implicitly[RawLinkPoly.HJoinCase[Node[Int, R[Int]] :: HNil, Node[Int :: HNil, Int]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

object HMatchNilTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val fs = HNil
        val gs = HNil

        implicitly[RawLinkPoly.HMatchCase[HNil, HNil]]

        fs |> gs
    }
}

object HMatchTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f0 = (x: Int) => x
        val f1 = (x: Double) => x
        val fs = f0 :: f1 :: HNil

        val g0 = (x: Int) => x
        val g1 = (x: Double) => x
        val gs = g0 :: g1 :: HNil

        val x0 = Node(f0)
        val x1 = Node(f1)
        val xs = x0 :: x1 :: HNil

        val y0 = Node(g0)
        val y1 = Node(g1)
        val ys = y0 :: y1 :: HNil

        implicitly[RawLinkPoly.HMatchCase[(Int => Int) :: (Double => Double) :: HNil, (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HMatchCase[(Int => Int) :: (Double => Double) :: HNil, Node[Int, Int] :: Node[Double, Double] :: HNil]]
        implicitly[RawLinkPoly.HMatchCase[Node[Int, Int] :: Node[Double, Double] :: HNil, (Int => Int) :: (Double => Double) :: HNil]]
        implicitly[RawLinkPoly.HMatchCase[Node[Int, Int] :: Node[Double, Double] :: HNil, Node[Int, Int] :: Node[Double, Double] :: HNil]]

        fs |> gs
        fs |> ys
        xs |> gs
        xs |> ys
    }
}

object RecursiveHSplitTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => 1 :: (1 :: 1 :: HNil) :: HNil

        val in = (x: Int) => ()
        val gs = in :: (in :: identity[Int] _ :: HNil) :: HNil

        f |> gs
    }
}

object MatchTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = identity[Int] _
        val g = identity[Int] _

        val fs = List(f, f)
        val gs = List(g, g)

        implicitly[RawLinkPoly.MatchCase[List[Int => Int], List[Int => Int]]]

        fs |> gs
    }
}
