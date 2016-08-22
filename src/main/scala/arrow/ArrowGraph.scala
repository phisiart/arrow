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

    implicit def GenNodeIsIn[I, O, N]
    (implicit n: N <:< Node[I, O])
    : (N Inputs I)
    = new NodeIsIn[I, O, N]

    class NodeIsOut[I, O, N]
    (implicit n: N <:< Node[I, O]) extends (N Outputs O) {
        def apply(p: N): Out[O] = {
            println("NodeIsOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenNodeIsOut[I, O, N]
    (implicit n: N <:< Node[I, O])
    : (N Outputs O)
    = new NodeIsOut[I, O, N]

    class StreamIsOut[O, S]
    (implicit s: S <:< Stream[O]) extends (S Outputs O) {
        def apply(p: S): Out[O] = {
            println("StreamIsOut")

            // TODO: implement this
            null
        }
    }

    implicit def GenStreamIsOut[O, S]
    (implicit s: S <:< Stream[O])
    : (S Outputs O)
    = new StreamIsOut[O, S]

    case class LinkableWrapper[T](linkable: T) {
        def |>[C](consumer: C)(implicit linkPoly: LinkPoly.Case[T, C]) = {
            linkPoly.apply(this.linkable, consumer)
        }

        def <|[P](producer: P)(implicit linkPoly: LinkPoly.Case[P, T]) = {
            linkPoly.apply(producer, this.linkable)
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
        (implicit ev: RawLinkPoly.Case[A, B])
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
        (implicit ev: RawLinkPoly.Case[AR, B])
        : CaseAux[Flow[AL, AR], B, Flow[AL, B]]
        = new Case[Flow[AL, AR], B] {
            type R = Flow[AL, B]

            def apply(a: Flow[AL, AR], b: B): Flow[AL, B] = {
                ev.apply(a.right, b)
                new Flow(a.left, b)
            }
        }

        implicit def RawLinksFlowCase[A, BL, BR]
        (implicit ev: RawLinkPoly.Case[A, BL])
        : CaseAux[A, Flow[BL, BR], Flow[A, BR]]
        = new Case[A, Flow[BL, BR]] {
            type R = Flow[A, BR]

            def apply(a: A, b: Flow[BL, BR]): Flow[A, BR] = {
                ev.apply(a, b.left)
                new Flow(a, b.right)
            }
        }

        implicit def FlowLinksFlowCase[AL, AR, BL, BR]
        (implicit ev: RawLinkPoly.Case[AR, BL])
        : CaseAux[Flow[AL, AR], Flow[BL, BR], Flow[AL, BR]]
        = new Case[Flow[AL, AR], Flow[BL, BR]] {
            type R = Flow[AL, BR]

            def apply(a: Flow[AL, AR], b: Flow[BL, BR]): Flow[AL, BR] = {
                ev.apply(a.right, b.left)
                new Flow(a.left, b.right)
            }
        }
    }

    object RawLinkPoly {
        def DEBUG(x: Any) = println(x)

        abstract class Case[A, B] {
            def apply(a: A, b: B): Unit
        }

        // For `implicitly` testing
        abstract class OneToOneCase[P, C] extends Case[P, C]

        implicit def OneToOne[M, P, C]
        (implicit
         out: (P Outputs M),
         in: (C Inputs M)
        ): OneToOneCase[P, C]
        = new OneToOneCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOne]")

                out(producer)
                in(consumer)
            }
        }

        // For `implicitly` testing
        abstract class OneToOneRCase[P, C] extends Case[P, C]

        implicit def OneToOneR[M, RM, P, C]
        (implicit
         in: (C Inputs M),
         out: (P Outputs RM),
         rm: RM <:< R[M]
        ): OneToOneRCase[P, C]
        = new OneToOneRCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOneR]")

                out(producer)
                in(consumer)
            }
        }

        // For `implicitly` testing
        abstract class BroadcastCase[P, Cs] extends Case[P, Cs]

        implicit def Broadcast[P, Cs, C]
        (implicit
         cs: Cs <:< Traversable[C],
         link: LinkPoly.Case[P, C]
        ): BroadcastCase[P, Cs]
        = new BroadcastCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[Broadcast]")

                cs.apply(consumers).map(consumer => {
                    link.apply(producer, consumer)
                })
            }
        }

        // For `implicitly` testing
        abstract class CollectCase[Ps, C] extends Case[Ps, C]

        implicit def Collect[Ps, P, C]
        (implicit
         ps: Ps <:< Traversable[P], // Fix P
         link: LinkPoly.Case[P, C]
        ): CollectCase[Ps, C]
        = new CollectCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[Collect]")

                ps.apply(producers).map(producer => {
                    link(producer, consumer)
                })
            }
        }

        // For `implicitly` testing
        abstract class SplitCase[P, Cs] extends Case[P, Cs]

        implicit def Split[Os, O, P, I, C, Cs]
        (implicit
         out: (P Outputs Os), // Fix Os
         ms: Os <:< Traversable[O], // Fix O
         cs: Cs <:< Traversable[C], // Fix C
         link: LinkPoly.Case[Out[O], C]
        ): SplitCase[P, Cs]
        = new SplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[Split]")

                out(producer)
                cs.apply(consumers) // TODO: one by one
            }
        }

        // For `implicitly` testing
        abstract class JoinCase[Ps, C] extends Case[Ps, C]

        implicit def Join[Ps, P, O, Is, I, C]
        (implicit
         ps: Ps <:< Traversable[P], // Fix P
         in: (C Inputs Is), // Fix Is
         is: Is <:< Traversable[I], // Fix I
         link: LinkPoly.Case[P, In[I]]
        ): JoinCase[Ps, C]
        = new JoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[Join]")

                ps.apply(producers) // TODO: one by one
                in(consumer)
            }
        }

//        abstract class MatchCase[Ps, Cs] extends Case[Ps, Cs]
//
//        implicit def Match[Ps, P, Cs, C]
//        (implicit
//         ps: Ps <:< Traversable[P],
//         cs: Cs <:< Traversable[C]
////         ps_m: Manifest[Ps],
////         cs_m: Manifest[Cs]
////         link: LinkPoly.Case[P, C]
//        ): MatchCase[Ps, Cs]
//        = {
////            DEBUG(ps_m)
////            DEBUG(cs_m)
//
//            new MatchCase[Ps, Cs] {
//                def apply(producers: Ps, consumers: Cs) {
//                    DEBUG("[Match]")
//
//    //                (ps.apply(producers).toVector zip cs.apply(consumers).toVector)
//    //                    .foreach { case (producer, consumer) => {
//    //                        link.apply(producer, consumer)
//    //                    }}
//                }
//            }
//        }

        // For `implicitly` testing
        abstract class HSplitCase[P, Cs <: HList] extends Case[P, Cs]

        // Out[HNil] |> HNil
        implicit def HSplitNil[M <: HNil, P, Cs <: HNil]
        (implicit out: (P Outputs M))
        : HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitNil]")

                out(producer)
            }
        }

        // Out[A :: B :: HNil] |> (In[A] :: In[B] :: HNil)
        implicit def HSplit[P, Os <: HList, OH, OT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit
         out: (P Outputs Os), // Fix Os
         os: Os <:< (OH :: OT), // Fix OH & OT
         cs: Cs <:< (CH :: CT), // Fix CH & CT
         linkHead: LinkPoly.Case[Out[OH], CH],
         linkTail: LinkPoly.Case[Out[OT], CT]
        ): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplit]")

                out(producer)
            }
        }

        // For `implicitly` testing
        abstract class HJoinCase[Ps <: HList, C] extends Case[Ps, C]

        implicit def HJoinNil[Ps <: HNil, C, M <: HNil]
        (implicit in: (C Inputs M))
        : HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinNil]")

                in(consumer)
            }
        }

        implicit def HJoin[Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit
         in: (C Inputs M),
         m: M <:< (MH :: MT),
         ps: Ps <:< (PH :: PT),
         linkHead: LinkPoly.Case[PH, In[MH]],
         linkTail: LinkPoly.Case[PT, In[MT]]
        ): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoin]")

                in(consumer)
            }
        }

        // Heterogeneous Match
        // ===================

        abstract class HMatchCase[Ps <: HList, Cs <: HList] extends Case[Ps, Cs]

        class MatchHNilCase[Ps <: HNil, Cs <: HNil] extends HMatchCase[Ps, Cs] {
            def apply(producers: Ps, consumers: Cs) {
                DEBUG("[HMatchNil]")
            }
        }

        class MatchHListCase[PH, PT <: HList, CH, CT <: HList]
        (
            val linkHead: LinkPoly.Case[PH, CH],
            val linkTail: LinkPoly.Case[PT, CT]
        ) extends HMatchCase[PH :: PT, CH :: CT] {
            def apply(producers: PH :: PT, consumers: CH :: CT) {
                DEBUG("[HMatch]")
                linkHead.apply(producers.head, consumers.head)
                linkTail.apply(producers.tail, consumers.tail)
            }
        }

        implicit def MatchHNil[Ps <: HNil, Cs <: HNil]
        : MatchHNilCase[Ps, Cs]
        = new MatchHNilCase[Ps, Cs]

        implicit def MatchHList[PH, PT <: HList, CH, CT <: HList]
        (implicit
         linkHead: LinkPoly.Case[PH, CH],
         linkTail: LinkPoly.Case[PT, CT]
        ): MatchHListCase[PH, PT, CH, CT]
        = new MatchHListCase[PH, PT, CH, CT](linkHead, linkTail)
    }
}
