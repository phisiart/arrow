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

    final class Flow[L, R](val left: L, val right: R)

//    implicit def FlowLinksObj[L, R, C]
//    (implicit ev: LinkVoidPoly.Case[R, C])
//    : LinkVoidPoly.Case[Flow[L, R], C]
//    = new LinkV

    class InIsIn[I] extends (In[I] Inputs I) {
        def apply(c: In[I]): In[I] = c
    }

    implicit def GenInIsIn[I]: (In[I] Inputs I) =
        new InIsIn[I]

    class OutIsOut[O] extends (Out[O] Outputs O) {
        def apply(p: Out[O]): Out[O] = p
    }

    implicit def GenOutIsOut[O]: (Out[O] Outputs O) =
        new OutIsOut[O]

    class FunIsIn[I, O] extends ((I => O) Inputs I) {
        def apply(c: I => O): In[I] = {
            println("FunctionToIn")

            // TODO: implement this
            null
        }
    }

    implicit def GenFunIsIn[I, O]: ((I => O) Inputs I) =
        new FunIsIn[I, O]

    class FunIsOut[I, O] extends ((I => O) Outputs O) {
        def apply(p: I => O): Out[O] = {
            println("FunctionToOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenFunIsOut[I, O]: ((I => O) Outputs O) =
        new FunIsOut[I, O]

    class NodeIsIn[I, O, N](implicit n: N <:< Node[I, O]) extends (N Inputs I) {
        def apply(c: N): In[I] = {
            println("NodeIsIn")

            // TODO: implement this
            null
        }
    }

    implicit def GenNodeIsIn[I, O, N](implicit n: N <:< Node[I, O]): (N Inputs I) =
        new NodeIsIn[I, O, N]

    class NodeIsOut[I, O, N](implicit n: N <:< Node[I, O]) extends (N Outputs O) {
        def apply(p: N): Out[O] = {
            println("NodeIsOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenNodeIsOut[I, O, N](implicit n: N <:< Node[I, O]): (N Outputs O) =
        new NodeIsOut[I, O, N]


    case class LinkableWrapper[P](linkable: P) {
        def |>[C](consumer: C)(implicit linkVoidPoly: LinkPoly.Case[P, C]) = {
            linkVoidPoly.apply(this.linkable, consumer)
        }
    }

    implicit def GenLinkableWrapper[T](linkable: T): LinkableWrapper[T] =
        LinkableWrapper(linkable)

    object LinkPoly {
        def DEBUG(x: Any) = println(x)

        abstract class Case[A, B] {
            type R
            def apply(a: A, b: B): R
        }

        type CaseAux[A, B, _R] = Case[A, B] { type R = _R }

        implicit def RawCase[A, B]
        (implicit ev: LinkVoidPoly.Case[A, B])
        : CaseAux[A, B, Flow[A, B]]
        = {
            new Case[A, B] {
                type R = Flow[A, B]

                def apply(a: A, b: B): Flow[A, B] = {
                    ev.apply(a, b)
                    new Flow(a, b)
                }
            }
        }

        implicit def FlowLinksRawCase[AL, AR, B]
        (implicit ev: LinkVoidPoly.Case[AR, B])
        : CaseAux[Flow[AL, AR], B, Flow[AL, B]]
        = new Case[Flow[AL, AR], B] {
            type R = Flow[AL, B]

            def apply(a: Flow[AL, AR], b: B): Flow[AL, B] = {
                ev.apply(a.right, b)
                new Flow(a.left, b)
            }
        }

        implicit def RawLinksFlowCase[A, BL, BR]
        (implicit ev: LinkVoidPoly.Case[A, BL])
        : CaseAux[A, Flow[BL, BR], Flow[A, BR]]
        = new Case[A, Flow[BL, BR]] {
            type R = Flow[A, BR]

            def apply(a: A, b: Flow[BL, BR]): Flow[A, BR] = {
                ev.apply(a, b.left)
                new Flow(a, b.right)
            }
        }

        implicit def FlowLinksFlowCase[AL, AR, BL, BR]
        (implicit ev: LinkVoidPoly.Case[AR, BL])
        : CaseAux[Flow[AL, AR], Flow[BL, BR], Flow[AL, BR]]
        = new Case[Flow[AL, AR], Flow[BL, BR]] {
            type R = Flow[AL, BR]

            def apply(a: Flow[AL, AR], b: Flow[BL, BR]): Flow[AL, BR] = {
                ev.apply(a.right, b.left)
                new Flow(a.left, b.right)
            }
        }
    }

    object LinkVoidPoly {
        def DEBUG(x: Any) = println(x)

        abstract class Case[A, B] {
            def apply(a: A, b: B): Unit
        }

        abstract class OneToOneCase[P, C] extends Case[P, C]

        implicit def OneToOne[M, P, C]
        (implicit ev1: (P Outputs M), ev2: (C Inputs M)): OneToOneCase[P, C]
        = new OneToOneCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOne]")
                ev1(producer)
                ev2(consumer)
            }
        }

        abstract class OneToOneRCase[P, C] extends Case[P, C]

        implicit def OneToOneR[M, RM, P, C]
        (implicit in: (C Inputs M), out: (P Outputs RM), rm: RM <:< R[M]): OneToOneRCase[P, C]
        = new OneToOneRCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOneR]")
                out(producer)
                in(consumer)
            }
        }

        abstract class BroadcastCase[P, Cs] extends Case[P, Cs]

        implicit def Broadcast[M, P, S[_] <: Seq[_], C]
        (implicit ev1: (P Outputs M), ev2: (C Inputs M)): BroadcastCase[P, S[C]]
        = new BroadcastCase[P, S[C]] {
            def apply(producer: P, consumers: S[C]) {
                DEBUG("[Broadcast]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class BroadcastRCase[P, Cs] extends Case[P, Cs]

        implicit def BroadcastR[M, _R[_] <: R[_], P, S[_] <: Seq[_], C]
        (implicit ev1: (P Outputs _R[M]), ev2: (C Inputs M)): BroadcastRCase[P, S[C]]
        = new BroadcastRCase[P, S[C]] {
            def apply(producer: P, consumers: S[C]) {
                DEBUG("[BroadcastR]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class CollectCase[Ps, C] extends Case[Ps, C]

        implicit def Collect[M, S[_] <: Seq[_], P, C]
        (implicit ev1: (P Outputs M), ev2: (C Inputs M)): CollectCase[S[P], C]
        = new CollectCase[S[P], C] {
            def apply(producers: S[P], consumer: C) {
                DEBUG("[Collect]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class CollectRCase[Ps, C] extends Case[Ps, C]

        implicit def CollectR[M, _R[_] <: R[_], S[_] <: Seq[_], P, C]
        (implicit ev1: (P Outputs _R[M]), ev2: (C Inputs M)): CollectRCase[S[P], C]
        = new CollectRCase[S[P], C] {
            def apply(producers: S[P], consumer: C) {
                DEBUG("[CollectR]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class SplitCase[P, Cs] extends Case[P, Cs]

        implicit def Split[M, S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: (P Outputs S1[M]), ev2: (C Inputs M)): SplitCase[P, S2[C]]
        = new SplitCase[P, S2[C]] {
            def apply(producer: P, consumers: S2[C]) {
                DEBUG("[Split]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class SplitRCase[P, Cs] extends Case[P, Cs]

        implicit def SplitR[M, _R[_] <: R[_], S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: (P Outputs S1[_R[M]]), ev2: (C Inputs M)): SplitRCase[P, S2[C]]
        = new SplitRCase[P, S2[C]] {
            def apply(producer: P, consumers: S2[C]) {
                DEBUG("[SplitR]")
                ev1(producer)
                consumers.asInstanceOf[Seq[C]].map(ev2.apply)
            }
        }

        abstract class JoinCase[Ps, C] extends Case[Ps, C]

        implicit def Join[M, S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: (P Outputs M), ev2: (C Inputs S2[M])): JoinCase[S1[P], C]
        = new JoinCase[S1[P], C] {
            def apply(producers: S1[P], consumer: C) {
                DEBUG("[Join]")
                producers.asInstanceOf[Seq[P]].map(ev1.apply)
                ev2(consumer)
            }
        }

        abstract class JoinRCase[Ps, C] extends Case[Ps, C]

        implicit def JoinR[M, _R[_] <: R[_], S1[_] <: Seq[_], P, S2[_] <: Seq[_], C]
        (implicit ev1: (P Outputs _R[M]), ev2: (C Inputs S2[M])): JoinRCase[S1[P], C]
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
        (implicit ev1: (P Outputs M)): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitNil]")
                ev1(producer)
            }
        }

        // Out[MH :: MT] |> In[MH] :: CT
        // Requires Out[MT] |> CT
//        implicit def HSplit[P, M <: HList, MH, MT <: HList, Cs <: HList, CH, CT <: HList]
//        (implicit ev1: (P Outputs M), ev0: M <:< (MH :: MT), cs: Cs <:< (CH :: CT), ev2: (CH Inputs MH), tail: HSplitCase[Out[MT], CT]): HSplitCase[P, Cs]
//        = new HSplitCase[P, Cs] {
//            def apply(producer: P, consumers: Cs) {
//                DEBUG("[HSplit]")
//                ev1(producer)
//            }
//        }

        implicit def HSplit[P, M <: HList, MH, MT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit ev1: (P Outputs M), ev0: M <:< (MH :: MT), cs: Cs <:< (CH :: CT), head: LinkPoly.Case[Out[MH], CH], tail: LinkPoly.Case[Out[MT], CT]): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplit]")
                ev1(producer)
            }
        }

//        implicit def HSplitR[P, M <: HList, _R[_] <: R[_], MH, MT <: HList, Cs <: HList, CH, CT <: HList]
//        (implicit ev1: (P Outputs M), ev0: M <:< (_R[MH] :: MT), cs: Cs <:< (CH :: CT), ev2: (CH Inputs MH), tail: HSplitCase[Out[MT], CT]): HSplitCase[P, Cs]
//        = new HSplitCase[P, Cs] {
//            def apply(producer: P, consumers: Cs) {
//                DEBUG("[HSplitR]")
//                ev1(producer)
//            }
//        }

        abstract class HJoinCase[Ps <: HList, C] extends Case[Ps, C]

        implicit def HJoinNil[Ps <: HNil, C, M <: HNil]
        (implicit ev: (C Inputs M)): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinNil]")
            }
        }

//        implicit def HJoin[Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
//        (implicit ev1: (C Inputs M), ev2: M <:< (MH :: MT), ps:  Ps <:< (PH :: PT), ev3: (PH Outputs MH), tail: HJoinCase[PT, In[MT]]): HJoinCase[Ps, C]
//        = new HJoinCase[Ps, C] {
//            def apply(producers: Ps, consumer: C) {
//                DEBUG("[HJoin]")
//            }
//        }

        implicit def HJoin[Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit ev1: (C Inputs M), ev2: M <:< (MH :: MT), ps: Ps <:< (PH :: PT), head: LinkPoly.Case[PH, In[MH]], tail: LinkPoly.Case[PT, In[MT]]): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoin]")
            }
        }

//        implicit def HJoinR[Ps <: HList, PH, PT <: HList, M <: HList, _R[_] <: R[_], MH, MT <: HList, C]
//        (implicit ev1: (C Inputs M), ev2: M <:< (MH :: MT), ps:  Ps <:< (PH :: PT), ev3: (PH Outputs _R[MH]), tail: HJoinCase[PT, In[MT]]): HJoinCase[Ps, C]
//        = new HJoinCase[Ps, C] {
//            def apply(producers: Ps, consumer: C) {
//                DEBUG("[HJoinR]")
//            }
//        }

        abstract class HMatchCase[Ps <: HList, Cs <: HList] extends Case[Ps, Cs]

        implicit def HMatchNil[Ps <: HNil, Cs <: HNil]
        = new HMatchCase[Ps, Cs] {
            def apply(producers: Ps, consumers: Cs) {
                DEBUG("[HMatchNil]")
            }
        }

        implicit def HMatch[Ps <: HList, PH, PT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit ps: Ps <:< (PH :: PT), cs: Cs <:< (CH :: CT), ev1: Case[PH, CH], ev2: Case[PT, CT]): HMatchCase[Ps, Cs]
        = new HMatchCase[Ps, Cs] {
            def apply(producers: Ps, consumers: Cs) {
                DEBUG("[HMatch]")
                ev1.apply(producers.head, consumers.head)
                ev2.apply(producers.tail, consumers.tail)
            }
        }

    }
}

