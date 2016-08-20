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
import syntax.std.tuple._

object OneToOneTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => x + 1

        val g = (x: Int) => x - 1

        val x = new Node[Int, Int] {
            def apply(x: Int) = x + 1
        }

        val y = new Node[Int, Int] {
            def apply(x: Int) = x - 1
        }

        implicitly[LinkVoidPoly.OneToOneCase[Int => Int, Int => Int]]
        implicitly[LinkVoidPoly.OneToOneCase[Int => Int, Node[Int, Int]]]
        implicitly[LinkVoidPoly.OneToOneCase[Node[Int, Int], Int => Int]]
        implicitly[LinkVoidPoly.OneToOneCase[Node[Int, Int], Node[Int, Int]]]

        f |> g
        f |> y
        x |> g
        x |> y
    }
}

object OneToOneRTest {
    def main(args: Array[String]) {
        val graph = new ArrowGraph
        import graph._

        val f = (x: Int) => Push(x + 1) //.asInstanceOf[R[Int]]

        val g = (x: Int) => x - 1

        val x = new Node[Int, R[Int]] {
            def apply(x: Int) = Push(x + 1)
        }

        val y = new Node[Int, Int] {
            def apply(x: Int) = x - 1
        }

        type T = Int

        implicitly[LinkVoidPoly.OneToOneRCase[T => R[T], T => T]]
        implicitly[LinkVoidPoly.OneToOneRCase[T => R[T], Node[T, T]]]
        implicitly[LinkVoidPoly.OneToOneRCase[Node[T, R[T]], T => T]]
        implicitly[LinkVoidPoly.OneToOneRCase[Node[T, R[T]], Node[T, T]]]

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

        type T = Int

        val f = (x: T) => x

        val gs = Vector(f, f)

        val x = new Node[T, T] {
            def apply(x: T) = f(x)
        }

        val ys = List(x, x)

        implicitly[LinkVoidPoly.BroadcastCase[T => T, Vector[T => T]]]
        implicitly[LinkVoidPoly.BroadcastCase[T => T, List[Node[T, T]]]]
        implicitly[LinkVoidPoly.BroadcastCase[Node[T, T], Vector[T => T]]]
        implicitly[LinkVoidPoly.BroadcastCase[Node[T, T], List[Node[T, T]]]]

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

        type T = Int

        val f = (x: T) => Push(x)

        val gs = Vector(f, f)

        val x = new Node[T, R[T]] {
            def apply(x: T) = f(x)
        }

        val ys = List(x, x)

        implicitly[LinkVoidPoly.BroadcastRCase[T => Push[T], Vector[T => T]]]
        implicitly[LinkVoidPoly.BroadcastRCase[T => Push[T], Vector[Node[T, T]]]]
        implicitly[LinkVoidPoly.BroadcastRCase[T => Push[T], Vector[T => T]]]
        implicitly[LinkVoidPoly.BroadcastRCase[Node[T, Push[T]], Vector[Node[T, T]]]]

        f |> gs
        f |> ys
        x |> gs
        x |> ys
    }
}

object CollectTest {
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

        implicitly[LinkVoidPoly.CollectCase[List[T => T], T => T]]
        implicitly[LinkVoidPoly.CollectCase[List[T => T], Node[T, T]]]
        implicitly[LinkVoidPoly.CollectCase[List[Node[T, T]], T => T]]
        implicitly[LinkVoidPoly.CollectCase[List[T => T], Node[T, T]]]

        // ambiguous: Collect, Join
        fs |> g

        fs |> y

        // ambiguous: Collect, Join
        xs |> g

        xs |> y
    }
}

object CollectRTest {
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

        implicitly[LinkVoidPoly.CollectRCase[List[T => Push[T]], T => T]]
        implicitly[LinkVoidPoly.CollectRCase[List[T => Push[T]], Node[T, T]]]
        implicitly[LinkVoidPoly.CollectRCase[List[Node[T, Push[T]]], T => T]]
        implicitly[LinkVoidPoly.CollectRCase[List[Node[T, Push[T]]], Node[T, T]]]

        // ambiguous: CollectR, Join
        fs |> g

        fs |> y

        // ambiguous: CollectR, Join
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

        implicitly[LinkVoidPoly.SplitCase[T => List[T], Vector[T => T]]]
        implicitly[LinkVoidPoly.SplitCase[T => List[T], Vector[Node[T, T]]]]
        implicitly[LinkVoidPoly.SplitCase[Node[T, List[T]], Vector[T => T]]]
        implicitly[LinkVoidPoly.SplitCase[Node[T, List[T]], Vector[Node[T, T]]]]

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

        val f = (x: T) => List(Push(x), Push(x))

        val g = identity[T] _
        val gs = Vector(g, g)

        val x = new Node[T, IndexedSeq[R[T]]] {
            def apply(x: T) = f(x)
        }

        val y = new Node[T, T] {
            def apply(x: T) = g(x)
        }
        val ys = Seq(y, y)

        implicitly[LinkVoidPoly.SplitRCase[T => List[Push[T]], Vector[T => T]]]
        implicitly[LinkVoidPoly.SplitRCase[T => List[Push[T]], Vector[Node[T, T]]]]
        implicitly[LinkVoidPoly.SplitRCase[Node[T, List[Push[T]]], Vector[T => T]]]
        implicitly[LinkVoidPoly.SplitRCase[Node[T, List[Push[T]]], Vector[Node[T, T]]]]

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

        implicitly[LinkVoidPoly.JoinCase[List[T => T], Vector[T] => T]]
        implicitly[LinkVoidPoly.JoinCase[List[T => T], Node[Vector[T], T]]]
        implicitly[LinkVoidPoly.JoinCase[List[Node[T, T]], Vector[T] => T]]
        implicitly[LinkVoidPoly.JoinCase[List[Node[T, T]], Node[Vector[T], T]]]

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

        implicitly[LinkVoidPoly.JoinRCase[List[T => Push[T]], Vector[T] => T]]
        implicitly[LinkVoidPoly.JoinRCase[List[Node[T, Push[T]]], Node[Vector[T], T]]]
        implicitly[LinkVoidPoly.JoinRCase[List[T => Push[T]], Vector[T] => T]]
        implicitly[LinkVoidPoly.JoinRCase[List[Node[T, Push[T]]], Node[Vector[T], T]]]

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}

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

        val gs = (identity[Int] _) :: (identity[Double] _) :: HNil

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

        val gs = (identity[Int] _) :: (identity[Double] _) :: HNil

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

        fs |> g
        fs |> y
        xs |> g
        xs |> y
    }
}