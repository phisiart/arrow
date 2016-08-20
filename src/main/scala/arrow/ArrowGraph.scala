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

package arrow

import shapeless._

class ArrowGraph {
    abstract class Outputs[P, O] {
        def apply(p: P): Out[O]
    }

    abstract class Inputs[C, I] {
        def apply(c: C): In[I]
    }

    class InIsIn[I] extends Inputs[In[I], I] {
        def apply(c: In[I]): In[I] = c
    }

    implicit def GenInIsIn[I]: Inputs[In[I], I] =
        new InIsIn[I]

    class OutIsOut[O] extends (Out[O] Outputs O) {
        def apply(p: Out[O]): Out[O] = p
    }

    implicit def GenOutIsOut[O]: (Out[O] Outputs O) =
        new OutIsOut[O]

    class FunIsIn[I, O] extends Inputs[I => O, I] {
        def apply(c: I => O): In[I] = {
            println("FunctionToIn")

            // TODO: implement this
            null
        }
    }

    implicit def GenFunIsIn[I, O]: Inputs[I => O, I] =
        new FunIsIn[I, O]

    class FunIsOut[I, O] extends ((I => O) Outputs O) {
        def apply(p: I => O): Out[O] = {
            println("FunctionToOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenFunIsOut[I, O]: Outputs[I => O, O] =
        new FunIsOut[I, O]

    class NodeIsIn[I, O, N](implicit n: N <:< Node[I, O]) extends Inputs[N, I] {
        def apply(c: N): In[I] = {
            println("NodeIsIn")

            // TODO: implement this
            null
        }
    }

    implicit def GenNodeIsIn[I, O, N](implicit n: N <:< Node[I, O]): Inputs[N, I] =
        new NodeIsIn[I, O, N]

    class NodeIsOut[I, O, N](implicit n: N <:< Node[I, O]) extends Outputs[N, O] {
        def apply(p: N): Out[O] = {
            println("NodeIsOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenNodeIsOut[I, O, N](implicit n: N <:< Node[I, O]): Outputs[N, O] =
        new NodeIsOut[I, O, N]


    case class LinkableWrapper[P](linkable: P) {
        def |>[C](consumer: C)(implicit linkVoidPoly: LinkVoidPoly.Case[P, C]) = {
            linkVoidPoly.apply(this.linkable, consumer)
        }
    }

    implicit def GenLinkableWrapper[T](linkable: T): LinkableWrapper[T] = LinkableWrapper(linkable)

    object LinkVoidPoly {
        def DEBUG(x: Any) = println(x)

        abstract class Case[A, B] {
            def apply(a: A, b: B): Unit
        }

        abstract class OneToOneCase[P, C] extends Case[P, C]

        implicit def OneToOne[M, P, C]
        (implicit ev1: Outputs[P, M], ev2: Inputs[C, M]): OneToOneCase[P, C]
        = new OneToOneCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOne]")
                ev1(producer)
                ev2(consumer)
            }
        }

        abstract class OneToOneRCase[P, C] extends Case[P, C]

        implicit def OneToOneR[M, R_[_] <: R[_], P, C]
        (implicit ev1: Outputs[P, R_[M]], ev2: Inputs[C, M]): OneToOneRCase[P, C]
        = new OneToOneRCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOneR]")
                ev1(producer)
                ev2(consumer)
            }
        }

        abstract class BroadcastCase[P, Cs] extends Case[P, Cs]

        implicit def Broadcast[M, P, S[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, M], ev2: Inputs[C, M]): BroadcastCase[P, S[C]]
        = new BroadcastCase[P, S[C]] {
            def apply(producer: P, consumers: S[C]) {
                DEBUG("[Broadcast]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class BroadcastRCase[P, Cs] extends Case[P, Cs]

        implicit def BroadcastR[M, R_[_] <: R[_], P, S[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, R_[M]], ev2: Inputs[C, M]): BroadcastRCase[P, S[C]]
        = new BroadcastRCase[P, S[C]] {
            def apply(producer: P, consumers: S[C]) {
                DEBUG("[BroadcastR]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class CollectCase[Ps, C] extends Case[Ps, C]

        implicit def Collect[M, S[_] <: Seq[_], P, C]
        (implicit ev1: Outputs[P, M], ev2: Inputs[C, M]): CollectCase[S[P], C]
        = new CollectCase[S[P], C] {
            def apply(producers: S[P], consumer: C) {
                DEBUG("[Collect]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class CollectRCase[Ps, C] extends Case[Ps, C]

        implicit def CollectR[M, R_[_] <: R[_], S[_] <: Seq[_], P, C]
        (implicit ev1: Outputs[P, R_[M]], ev2: Inputs[C, M]): CollectRCase[S[P], C]
        = new CollectRCase[S[P], C] {
            def apply(producers: S[P], consumer: C) {
                DEBUG("[CollectR]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class SplitCase[P, Cs] extends Case[P, Cs]

        implicit def Split[M, S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, S1[M]], ev2: Inputs[C, M]): SplitCase[P, S2[C]]
        = new SplitCase[P, S2[C]] {
            def apply(producer: P, consumers: S2[C]) {
                DEBUG("[Split]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class SplitRCase[P, Cs] extends Case[P, Cs]

        implicit def SplitR[M, _R[_] <: R[_], S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, S1[_R[M]]], ev2: Inputs[C, M]): SplitRCase[P, S2[C]]
        = new SplitRCase[P, S2[C]] {
            def apply(producer: P, consumers: S2[C]) {
                DEBUG("[SplitR]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class JoinCase[Ps, C] extends Case[Ps, C]

        implicit def Join[M, S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, M], ev2: Inputs[C, S2[M]],
         m: Manifest[M], s1: Manifest[S1[P]], s2: Manifest[S2[M]], c: Manifest[C], mev2: Manifest[C => In[S2[M]]]): JoinCase[S1[P], C]
        = new JoinCase[S1[P], C] {
            def apply(producers: S1[P], consumer: C) {
                //                DEBUG(typeTag[M])
                //                DEBUG(typeTag[P])
                //                DEBUG(typeTag[C])
                //                val p = manifest[P]
                //                DEBUG(p)
                DEBUG("[Join]")
                DEBUG(m)
                DEBUG(s1)
                DEBUG(s2)
                DEBUG(c)
                DEBUG(mev2)
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class JoinRCase[Ps, C] extends Case[Ps, C]

        implicit def JoinR[M, R_[_] <: R[_], S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: Outputs[P, R_[M]], ev2: Inputs[C, S2[M]]): JoinRCase[S1[P], C]
        = new JoinRCase[S1[P], C] {
            def apply(producers: S1[P], consumer: C) {
                DEBUG("[JoinR]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class HSplitCase[P, Cs <: HList] extends Case[P, Cs]

        // Out[HNil] |> HNil
        implicit def HSplitNil[M <: HNil, P, Cs <: HNil]
        (implicit ev1: P Outputs M): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitNil]")
                ev1(producer)
            }
        }

        // Out[MH :: MT] |> In[MH] :: CT
        // Requires Out[MT] |> CT
        implicit def HSplit[P, M <: HList, MH, MT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit ev1: P Outputs M, ev0: M <:< (MH :: MT), cs: Cs <:< (CH :: CT), ev2: CH Inputs MH, tail: HSplitCase[Out[MT], CT]): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplit]")
                ev1(producer)
            }
        }


        implicit def HSplitR[P, M <: HList, _R[_] <: R[_], MH, MT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit ev1: P Outputs M, ev0: M <:< (_R[MH] :: MT), cs: Cs <:< (CH :: CT), ev2: CH Inputs MH, tail: HSplitCase[Out[MT], CT]): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitR]")
                ev1(producer)
            }
        }


        abstract class HJoinCase[Ps <: HList, C] extends Case[Ps, C]

        implicit def HJoinNil[Ps <: HNil, C, M <: HNil]
        (implicit ev: C Inputs M): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinNil]")
            }
        }

        implicit def HJoin[Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit ev1: C Inputs M, ev2: M <:< (MH :: MT), ps:  Ps <:< (PH :: PT), ev3: PH Outputs MH, tail: HJoinCase[PT, In[MT]]): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoin]")
            }
        }


        implicit def HJoinR[Ps <: HList, PH, PT <: HList, M <: HList, _R[_] <: R[_], MH, MT <: HList, C]
        (implicit ev1: C Inputs M, ev2: M <:< (MH :: MT), ps:  Ps <:< (PH :: PT), ev3: PH Outputs _R[MH], tail: HJoinCase[PT, In[MT]]): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinR]")
            }
        }

    }
}

