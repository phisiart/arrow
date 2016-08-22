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

        implicit def Broadcast[P, Cs, C]
        (implicit
         cs: Cs <:< Seq[C],
         link: LinkPoly.Case[P, C]
        ): BroadcastCase[P, Cs]
        = new BroadcastCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[Broadcast]")
                consumers.asInstanceOf[Seq[C]].map(consumer => {
                    link.apply(producer, consumer)
                })
            }
        }

        abstract class CollectCase[Ps, C] extends Case[Ps, C]

        implicit def Collect[Ps, P, C]
        (implicit
         ps: Ps <:< Seq[P], // Fix P
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

        abstract class SplitCase[P, Cs] extends Case[P, Cs]

        implicit def Split[Os, O, P, I, C, Cs]
        (implicit
         out: (P Outputs Os), // Fix Os
         ms: Os <:< Seq[O], // Fix O
         cs: Cs <:< Seq[C], // Fix C
         link: LinkPoly.Case[Out[O], C]
        ): SplitCase[P, Cs]
        = new SplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[Split]")
                out(producer)
                consumers.asInstanceOf[Seq[C]] // TODO: one by one
            }
        }

        abstract class JoinCase[Ps, C] extends Case[Ps, C]

        implicit def Join[Ps, P, O, Is, I, C]
        (implicit
         ps: Ps <:< Seq[P], // Fix P
         in: (C Inputs Is), // Fix Is
         is: Is <:< Seq[I], // Fix I
         link: LinkPoly.Case[P, In[I]]
        ): JoinCase[Ps, C]
        = new JoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[Join]")
                producers.asInstanceOf[Seq[P]] // TODO: one by one
                in(consumer)
            }
        }

        abstract class HSplitCase[P, Cs <: HList] extends Case[P, Cs]

        // Out[HNil] |> HNil
        implicit def HSplitNil[M <: HNil, P, Cs <: HNil]
        (implicit out: (P Outputs M)): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitNil]")
                out(producer)
            }
        }

        // Out[A :: B :: HNil] |> (In[A] :: In[B] :: HNil)
        implicit def HSplit[P, Os <: HList, OH, OT <: HList, Cs <: HList, CH, CT <: HList]
        (implicit
         ev1: (P Outputs Os), // Fix Os
         ev0: Os <:< (OH :: OT), // Fix OH & OT
         cs: Cs <:< (CH :: CT), // Fix CH & CT
         linkHead: LinkPoly.Case[Out[OH], CH],
         linkTail: LinkPoly.Case[Out[OT], CT]
        ): HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplit]")
                ev1(producer)
            }
        }

        abstract class HJoinCase[Ps <: HList, C] extends Case[Ps, C]

        implicit def HJoinNil[Ps <: HNil, C, M <: HNil]
        (implicit ev: (C Inputs M)): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinNil]")
            }
        }

        implicit def HJoin[Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit
         in: (C Inputs M),
         ev2: M <:< (MH :: MT),
         ps: Ps <:< (PH :: PT),
         linkHead: LinkPoly.Case[PH, In[MH]],
         linkTail: LinkPoly.Case[PT, In[MT]]
        ): HJoinCase[Ps, C]
        = new HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoin]")
            }
        }

        abstract class HMatchCase[Ps <: HList, Cs <: HList] extends Case[Ps, Cs]

        implicit def HMatchNil[Ps <: HNil, Cs <: HNil]
        = new HMatchCase[Ps, Cs] {
            def apply(producers: Ps, consumers: Cs) {
                DEBUG("[HMatchNil]")
            }
        }

        implicit def HMatch[PH, PT <: HList, CH, CT <: HList]
        (implicit
         linkHead: LinkPoly.Case[PH, CH],
         linkTail: LinkPoly.Case[PT, CT]
        ): HMatchCase[PH :: PT, CH :: CT]
        = new HMatchCase[PH :: PT, CH :: CT] {
            def apply(producers: PH :: PT, consumers: CH :: CT) {
                DEBUG("[HMatch]")
                linkHead.apply(producers.head, consumers.head)
                linkTail.apply(producers.tail, consumers.tail)
            }
        }

    }
}

