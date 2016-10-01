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
import arrow.repr._
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

        val f = (x: I) => m

        val g = (x: M) => o

        val x = new Node[I, M] {
            def apply(x: I) = f(x)
        }

        val y = new Node[M, O] {
            def apply(x: M) = g(x)
        }

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
        type RM = Push[M]
        val rm = Push(1)
        type O = Int
        val o = 1

        val f = (x: I) => rm

        val g = (x: M) => o

        val x = new Node[I, RM] {
            def apply(x: I) = f(x)
        }

        val y = new Node[M, O] {
            def apply(x: M) = o
        }

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

        val f = (x: I) => m

        val x = new Node[I, M] {
            def apply(x: I) = f(x)
        }

        val g = (x: M) => o
        val gs = s(g, g)

        val y = new Node[M, O] {
            def apply(x: M) = g(x)
        }
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

object BroadcastRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type I = Int
        type M = Array[Double]
        val m = Array.empty[Double]
        type RM = Push[M]
        def r(m: M): RM = Push(m)
        type S[X] = Vector[X]
        def s[A](elems: A*): S[A] = elems.toVector
        type O = Int
        val o = 1

        type T = Int

        val f = (x: I) => r(m)
        val x = new Node[I, RM] {
            def apply(x: I) = f(x)
        }

        val g = (x: M) => o
        val gs = Vector(g, g)

        val y = new Node[M, O] {
            def apply(x: M) = g(x)
        }
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
            new Node[T, T] {
                def apply(x: T) = x
            }
        )

        val y = new Node[T, T] {
            def apply(x: T) = g(x)
        }

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
        val fun = (x: T) => Push(x)

        val fs = List(fun)

        val g = fun

        val xs = List(
            new Node[T, R[T]] {
                def apply(x: T) = fun(x)
            }
        )

        val y = new Node[T, Push[T]] {
            def apply(x: T) = g(x)
        }

        implicitly[RawLinkPoly.MergeCase[List[T => Push[T]], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[T => Push[T]], Node[T, T]]]
        implicitly[RawLinkPoly.MergeCase[List[Node[T, Push[T]]], T => T]]
        implicitly[RawLinkPoly.MergeCase[List[Node[T, Push[T]]], Node[T, T]]]

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

        val x = new Node[T, IndexedSeq[T]] {
            def apply(x: T) = f(x)
        }

        val y = new Node[T, T] {
            def apply(x: T) = g(x)
        }
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

        val f = (x: T) => List(Push(x), Put(x))

        val g = identity[T] _
        val gs = Vector(g, g)

        val x = new Node[T, IndexedSeq[R[T]]] {
            def apply(x: T) = f(x)
        }

        val y = new Node[T, T] {
            def apply(x: T) = g(x)
        }
        val ys = Seq(y, y)

        implicitly[RawLinkPoly.SplitCase[T => List[Push[T]], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[T => List[Push[T]], Vector[Node[T, T]]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[Push[T]]], Vector[T => T]]]
        implicitly[RawLinkPoly.SplitCase[Node[T, List[Push[T]]], Vector[Node[T, T]]]]

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

        val f = (x: T) => x
        val fs = List(f, f)

        val g = (x: Vector[T]) => x

        val x = new Node[T, T] {
            def apply(x: T) = f(x)
        }
        val xs = List(x, x)

        val y = new Node[Vector[T], Vector[T]] {
            def apply(x: Vector[T]) = g(x)
        }

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

        val f = (x: T) => Push(x)
        val fs = List(f, f)

        val g = (x: Vector[T]) => x

        val x = new Node[T, R[T]] {
            def apply(x: T) = f(x)
        }
        val xs = List(x, x)

        val y = new Node[Vector[T], Vector[T]] {
            def apply(x: Vector[T]) = g(x)
        }

        implicitly[RawLinkPoly.JoinCase[List[T => Push[T]], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, Push[T]]], Node[Vector[T], T]]]
        implicitly[RawLinkPoly.JoinCase[List[T => Push[T]], Vector[T] => T]]
        implicitly[RawLinkPoly.JoinCase[List[Node[T, Push[T]]], Node[Vector[T], T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

//object MatchTest {
//    def main(args: Array[String]) {
//        val graph = new ArrowGraph
//        import graph._
//
//        type I = Int
//        type M = Int
//        val m = 1
//        type O = Int
//        val o = 1
//        type S[X] = Vector[X]
//        def s[A](elems: A*): S[A] = elems.toVector
//
//        val f = (x: Int) => m
//        val fs = Vector(f, f)
//
//        val x = new Node[I, M] {
//            def apply(x: I) = f(x)
//        }
//        val xs = Vector(x, x)
//
//        val g = (x: Int) => o
//        val gs = Vector(g, g)
//
//        val y = new Node[M, O] {
//            def apply(x: M) = g(x)
//        }
//        val ys = s(y, y)
//
//        implicitly[RawLinkPoly.Case[S[I => M], S[M => O]]]
//        implicitly[RawLinkPoly.MatchCase[S[Node[I, M]], S[Node[M, O]]]]
//        implicitly[RawLinkPoly.MatchCase[S[I => M], S[M => O]]]
//        implicitly[RawLinkPoly.MatchCase[S[Node[I, M]], S[Node[M, O]]]]
//
//        fs |> gs
//        fs |> ys
//        xs |> gs
//        xs |> ys
//    }
//}

object HSplitNilTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        type T = Double
        val f = (x: T) => HNil

        val gs = HNil

        val x = new Node[T, HNil] {
            def apply(x: T) = f(x)
        }

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

        val f = (x: Int) => 1 :: 3.0 :: HNil

        val g0 = identity[Int] _
        val g1 = identity[Double] _
        val gs = g0 :: g1 :: HNil

        val x = new Node[Int, Int :: Double :: HNil] {
            def apply(x: Int) = f(x)
        }

        val y0 = new Node[Int, Int] {
            def apply(x: Int) = x
        }
        val y1: Node[Double, Double] = new Node[Double, Double] {
            def apply(x: Double) = x
        }
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

        val f = (x: Int) => Push(1) :: 3.0 :: HNil

        val g0 = identity[Int] _
        val g1 = identity[Double] _
        val gs = g0 :: g1 :: HNil

        val x = new Node[Int, R[Int] :: R[Double] :: HNil] {
            def apply(x: Int) = f(x)
        }

        val y0 = new Node[Int, Int] {
            def apply(x: Int) = x
        }
        val y1: Node[Double, Double] = new Node[Double, Double] {
            def apply(x: Double) = x
        }
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

        val y = new Node[HNil, HNil] {
            def apply(x: HNil) = g(x)
        }

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

        val g = (x: Int :: HNil) => 0

        val x0 = new Node[Int, Int] {
            def apply(x: Int) = f0(x)
        }
        val xs = x0 :: HNil

        val y = new Node[Int :: HNil, Int] {
            def apply(x: Int :: HNil) = g(x)
        }

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

        val f0 = (x: Int) => Push(x)
        val fs = f0 :: HNil

        val g = (x: Int :: HNil) => 0

        val x0 = new Node[Int, R[Int]] {
            def apply(x: Int) = f0(x)
        }
        val xs = x0 :: HNil

        val y = new Node[Int :: HNil, Int] {
            def apply(x: Int :: HNil) = g(x)
        }

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

        val x0 = new Node[Int, Int] {
            def apply(x: Int) = f0(x)
        }
        val x1 = new Node[Double, Double] {
            def apply(x: Double) = f1(x)
        }
        val xs = x0 :: x1 :: HNil

        val y0 = new Node[Int, Int] {
            def apply(x: Int) = g0(x)
        }
        val y1 = new Node[Double, Double] {
            def apply(x: Double) = g1(x)
        }
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
    def DummyIn[I](): In[I] = new In[I] {
        override def pullFrom = null
        override def addSubscription(subscription: SubscriptionTo[I]) {}
    }

    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => 1 :: (1 :: 1 :: HNil) :: HNil

        val in = DummyIn[Int]()
        val gs = in :: (in :: identity[Int] _ :: HNil) :: HNil

        f |> gs
    }
}

object MatchTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x
        val g = (x: Int) => x

        val fs = List(f, f)
        val gs = List(g, g)

        implicitly[RawLinkPoly.MatchCase[List[Int => Int], List[Int => Int]]]


        fs |> gs
    }
}