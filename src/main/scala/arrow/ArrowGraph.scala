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

import java.util.concurrent.Future

import arrow.repr._
import arrow.runtime._
import shapeless._

import scala.collection.Seq
import scala.collection.generic.CanBuildFrom
import scala.language.implicitConversions

class ArrowGraph {
    /** The intermediate representation of the graph. */
    val repr = new Repr

    def draw(): Unit = {
        repr.draw()
    }

    /**
      * The evidence that [[P]] produces an output of type [[O]].
      *
      * Cases:
      *   [[FunIsOut]]
      *   [[NodeIsOut]]
      *   [[OutIsOut]]
      *   [[StreamIsOut]]
      */
    abstract sealed class Outputs[P, O] {
        def apply(p: P): Out[O]
    }

    /**
      * The evidence that [[C]] consumes an input of type [[I]].
      *
      * Cases:
      *   [[FunIsIn]]
      *   [[InIsIn]]
      *   [[NodeIsIn]]
      */
    abstract sealed class Inputs[C, I] {
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

    /**
      * The evidence that function [[I]] => [[O]] has input [[I]].
      */
    class FunIsIn[I, O] extends ((I => O) Inputs I) {
        def apply(c: I => O): In[I] = {
            println("FunctionToIn")

            repr.makeFunctionIn(c)
        }
    }

    implicit def GenFunIsIn[I, O]: ((I => O) Inputs I) =
        new FunIsIn[I, O]

    /**
      * The evidence that function [[I]] => [[O]] has output [[O]].
      */
    class FunIsOut[I, O] extends ((I => O) Outputs O) {
        def apply(p: I => O): Out[O] = {
            println("FunctionToOut")

            repr.getFunctionOut(p)
        }
    }

    implicit def GenFunIsOut[I, O]: ((I => O) Outputs O) =
        new FunIsOut[I, O]

    /**
      * The evidence that [[N]] is a node with input type [[I]].
      */
    class NodeIsIn[I, O, N](implicit n: N <:< Node[I, O]) extends (N Inputs I) {
        def apply(c: N): In[I] = {
            println("NodeIsIn")

            val node = n(c)
            repr.makeNodeIn(node)
        }
    }

    implicit def GenNodeIsIn[I, O, N]
    (implicit n: N <:< Node[I, O])
    : (N Inputs I)
    = new NodeIsIn[I, O, N]

    /**
      * The evidence that [[N]] is a node with output type [[O]].
      */
    class NodeIsOut[I, O, N]
    (implicit n: N <:< Node[I, O]) extends (N Outputs O) {
        def apply(p: N): Out[O] = {
            println("NodeIsOut")

            val node = n(p)
            repr.getNodeOut(node)
        }
    }

    implicit def GenNodeIsOut[I, O, N]
    (implicit n: N <:< Node[I, O])
    : (N Outputs O)
    = new NodeIsOut[I, O, N]

    /**
      * The evidence that [[S]] is a stream with element type [[O]].
      */
    class StreamIsOut[O, S]
    (implicit s: S <:< Stream[O]) extends (S Outputs O) {
        def apply(p: S): Out[O] = {
            println("StreamIsOut")

            val stream = s(p)
            repr.getStreamOut(stream)
        }
    }

    implicit def GenStreamIsOut[O, S]
    (implicit s: S <:< Stream[O])
    : (S Outputs O)
    = new StreamIsOut[O, S]

    case class LinkableWrapper[T](linkable: T) {
        def |>[C](consumer: C)(implicit linkPoly: LinkPoly.Case[T, C]): linkPoly.R = {
            linkPoly.apply(this.linkable, consumer)
        }

        def <|[P](producer: P)(implicit linkPoly: LinkPoly.Case[P, T]): linkPoly.R = {
            linkPoly.apply(producer, this.linkable)
        }
    }

    implicit def GenLinkableWrapper[T](linkable: T): LinkableWrapper[T] =
        LinkableWrapper(linkable)

    def run[T, P]
    (producer: P)
    (implicit genOut: P Outputs T): Future[IndexedSeq[T]] = {
        val out = genOut(producer)
        val drainProcessor = repr.makeDrainProcessor[T]()
        val in = repr.makeDrainProcessorIn[T](drainProcessor)
        repr.insertSubscription(out, in)

        val runtime = new Runtime[T](this.repr, drainProcessor)

        runtime.run()
    }

    /**
      * Objects encapsulated with [[Flow]] should also be able to link.
      */
    object LinkPoly {
        def DEBUG(x: Any): Unit = println(x)

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
        def DEBUG(x: Any): Unit = println(x)

        abstract class Case[A, B] {
            def apply(a: A, b: B): Unit
        }

        // =====================================================================
        //  One to One
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class OneToOneCase[P, C] extends Case[P, C]

        final class OneToOneCaseImpl[M, P, C]
        (implicit genOut: P Outputs M, genIn: C Inputs M)
        extends OneToOneCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOne]")
                val out = genOut(producer)
                val in = genIn(consumer)
                repr.insertSubscription(out, in)
            }
        }

        implicit def GenOneToOneCase[M, P, C]
        (implicit genOut: P Outputs M, genIn: C Inputs M)
        = new OneToOneCaseImpl[M, P, C]

        /** For [[implicitly]] testing */
        abstract class OneToOneRCase[P, C] extends Case[P, C]

        final class OneToOneRCaseImpl[M, RM, P, C]
        (implicit genIn: C Inputs M, genOut: P Outputs RM, rm: RM <:< R[M])
        extends OneToOneRCase[P, C] {
            def apply(producer: P, consumer: C) {
                DEBUG("[OneToOneR]")

                val out = genOut(producer)
                val in = genIn(consumer)
                repr.insertSubscriptionR(out, in)
            }
        }

        implicit def GenOneToOneRCase[M, RM, P, C]
        (implicit genIn: C Inputs M, genOut: P Outputs RM, rm: RM <:< R[M])
        : OneToOneRCase[P, C]
        = new OneToOneRCaseImpl[M, RM, P, C]

        // =====================================================================
        //  Broadcast
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class BroadcastCase[P, Cs] extends Case[P, Cs]

        final class BroadcastCaseImpl[P, O, Cs, C]
        (implicit cs: Cs <:< Traversable[C], p: P Outputs O, link: LinkPoly.Case[P, C],
         p_mani: Manifest[P], cs_mani: Manifest[Cs], c_mani: Manifest[C])
        extends BroadcastCase[P, Cs] {
            DEBUG("[Broadcast]")
//            DEBUG(s"P = $p_mani, Cs = $cs_mani, c = $c_mani")

            def apply(producer: P, consumers: Cs) {
                cs(consumers).map(consumer => {
                    link.apply(producer, consumer)
                })
            }
        }

        implicit def Broadcast[P, O, Cs, C]
        (implicit cs: Cs <:< Traversable[C], p: P Outputs O, link: LinkPoly.Case[P, C],
         p_mani: Manifest[P], cs_mani: Manifest[Cs], c_mani: Manifest[C])
        = new BroadcastCaseImpl[P, O, Cs, C]

        // =====================================================================
        //  Merge
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class MergeCase[Ps, C] extends Case[Ps, C]

        final class MergeCaseImpl[Ps, P, C, I]
        (implicit ps: Ps <:< Traversable[P], c: C Inputs I, link: LinkPoly.Case[P, C])
        extends MergeCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[Collect]")

                ps(producers).map(producer => {
                    link(producer, consumer)
                })
            }
        }

        implicit def GenMergeCase[Ps, P, C, I]
        (implicit
         ps: Ps <:< Traversable[P],
         c: C Inputs I,
         link: LinkPoly.Case[P, C])
        = new MergeCaseImpl[Ps, P, C, I]

        // =====================================================================
        //  Split
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class SplitCase[P, Cs] extends Case[P, Cs]

        implicit def Split[Os, O, P, I, C, Cs]
        (implicit
         genOut: (P Outputs Os), // Fix Os
         ms: Os <:< Seq[O], // Fix O
         cs: Cs <:< Traversable[C], // Fix C
         link: LinkPoly.Case[Out[O], C]
        ): SplitCase[P, Cs]
        = new SplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[Split]")

                val out = genOut(producer)

                cs(consumers)
                    .toIndexedSeq
                    .zipWithIndex
                    .map { case (consumer, idx) =>
                        val splitterOut = repr.getSplitterOut(out, idx)
                        link(splitterOut, consumer)
                    }
            }
        }

        // =====================================================================
        //  Join
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class JoinCase[Ps, C] extends Case[Ps, C]

        /**
          * Ps < Seq[P]
          */
        implicit def Join[Ps, P, O, Is, S[_], I, C]
        (implicit
         ps: Ps <:< Seq[P], // Fix P
         genIn: (C Inputs Is), // Fix Is
         is: S[I] =:= Is, // Fix I, S
         s: S[_] <:< Seq[_],
         cbf: CanBuildFrom[Nothing, I, S[I]],
         link: LinkPoly.Case[P, In[I]],
         mps: Manifest[Ps],
         ms: Manifest[S[_]]
        ): JoinCase[Ps, C]
        = new JoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG(s"[Join] ${ms}")

                val in = genIn(consumer)

                ps(producers)
                    .toIndexedSeq
                    .zipWithIndex
                    .map { case (producer, idx) =>
                        val joinerIn = repr.makeJoinerIn(in, idx)
                        link(producer, joinerIn)
                    }
            }
        }

        // =====================================================================
        //  Heterogeneous Split
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class HSplitCase[P, Cs <: HList] extends Case[P, Cs]

        // Out[HNil] |> HNil
        implicit def HSplitNil[M <: HNil, P, Cs <: HNil]
        (implicit genOut: (P Outputs M))
        : HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplitNil]")
            }
        }

        /**
          * Out[OH :: OT] |> (In[A] :: IT)
          */
        implicit def HSplit[P, Os <: HList, OH, OT <: HList,
                            Cs <: HList, CH, CT <: HList]
        (implicit
         genOut: (P Outputs Os), // Fix Os
         os: Os <:< (OH :: OT), // Fix OH & OT
         cs: Cs <:< (CH :: CT), // Fix CH & CT
         linkHead: LinkPoly.Case[Out[OH], CH],
         linkTail: LinkPoly.Case[Out[OT], CT])
        : HSplitCase[P, Cs]
        = new HSplitCase[P, Cs] {
            def apply(producer: P, consumers: Cs) {
                DEBUG("[HSplit]")

                val out = genOut(producer)

                val outHd = repr.getHSplitterHdOut(out)
                val outTl = repr.getHSplitterTlOut(out)

                linkHead.apply(outHd, cs(consumers).head)
                linkTail.apply(outTl, cs(consumers).tail)
            }
        }

        // =====================================================================
        //  Heterogeneous Join
        // =====================================================================

        /** For [[implicitly]] testing */
        abstract class HJoinCase[Ps <: HList, C] extends Case[Ps, C]

        /** [[HNil]] |> [[In]]<[[HNil]]> */
        final class HJoinNilCaseImpl[Ps <: HList, C, M <: HNil]
        (implicit genIn: C Inputs M, m: HNil <:< M)
        extends HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoinNil]")

                val in = genIn(consumer)
                val out = repr.getStreamOut[R[M]](Stream(
                    Value[M](m(HNil), reusable = true)
                ))

                repr.insertSubscriptionR(out, in)
            }
        }

        implicit def GenHJoinNilCase[Ps <: HNil, C, M <: HNil]
        (implicit genIn: (C Inputs M), m: HNil <:< M)
        = new HJoinNilCaseImpl[Ps, C, M]

        final class HJoinCaseImpl
        [Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit
         genIn: C Inputs M,
         m: (MH :: MT) <:< M,
         ps: Ps <:< (PH :: PT),
         linkHead: LinkPoly.Case[PH, In[MH]],
         linkTail: LinkPoly.Case[PT, In[MT]])
        extends HJoinCase[Ps, C] {
            def apply(producers: Ps, consumer: C) {
                DEBUG("[HJoin]")

                /** [[in]] outputs [[M]] */
                val in = genIn(consumer)

                val inHd = repr.makeHJoinerHdIn(in)
                val inTl = repr.makeHJoinerTlIn(in)

                linkHead(ps(producers).head, inHd)
                linkTail(ps(producers).tail, inTl)
            }
        }

        implicit def HJoin
        [Ps <: HList, PH, PT <: HList, M <: HList, MH, MT <: HList, C]
        (implicit
         genIn: (C Inputs M),
         whyCantIRemoveThis: M <:< (MH :: MT),
         m: (MH :: MT) <:< M,
         ps: Ps <:< (PH :: PT),
         linkHead: LinkPoly.Case[PH, In[MH]],
         linkTail: LinkPoly.Case[PT, In[MT]],
         mm: Manifest[M],
         mmh: Manifest[MH],
         mmt: Manifest[MT],
         mm2: Manifest[shapeless.::[MH, MT]]
        )
        : HJoinCase[Ps, C]
        = {
            println(mm)
            new HJoinCaseImpl[Ps, PH, PT, M, MH, MT, C]
        }

        // =====================================================================
        //  Heterogeneous Match
        // =====================================================================

        abstract class HMatchCase[Ps <: HList, Cs <: HList] extends Case[Ps, Cs]

        final class MatchHNilCaseImpl[Ps <: HNil, Cs <: HNil]
            extends HMatchCase[Ps, Cs] {

            def apply(producers: Ps, consumers: Cs) {
                DEBUG("[HMatchNil]")
            }
        }

        class MatchHListCaseImpl[PH, PT <: HList, CH, CT <: HList]
        (val linkHead: LinkPoly.Case[PH, CH],
         val linkTail: LinkPoly.Case[PT, CT])
            extends HMatchCase[PH :: PT, CH :: CT] {

            def apply(producers: PH :: PT, consumers: CH :: CT) {
                DEBUG("[HMatch]")

                linkHead.apply(producers.head, consumers.head)
                linkTail.apply(producers.tail, consumers.tail)
            }
        }

        implicit def GenMatchHNilCase[Ps <: HNil, Cs <: HNil]
        : MatchHNilCaseImpl[Ps, Cs]
        = new MatchHNilCaseImpl[Ps, Cs]

        implicit def GenMatchHListCase[PH, PT <: HList, CH, CT <: HList]
        (implicit
         linkHead: LinkPoly.Case[PH, CH],
         linkTail: LinkPoly.Case[PT, CT])
        : MatchHListCaseImpl[PH, PT, CH, CT]
        = new MatchHListCaseImpl[PH, PT, CH, CT](linkHead, linkTail)

        // =====================================================================
        //  Match
        // =====================================================================

        abstract class MatchCase[Ps, Cs] extends Case[Ps, Cs]

        final class MatchCaseImpl[Ps, P, Cs, C]
        (implicit
         ps: Ps <:< Traversable[P],
         cs: Cs <:< Traversable[C],
         link: LinkPoly.Case[P, C]) extends MatchCase[Ps, Cs] {

            DEBUG("[Match]")

            def apply(producers: Ps, consumers: Cs) {
                (ps.apply(producers).toVector zip cs.apply(consumers).toVector)
                    .foreach { case (producer, consumer) => {
                        link.apply(producer, consumer)
                    }}
            }
        }

        implicit def Match[Ps, P, Cs, C]
        (implicit
         ps: Ps <:< Traversable[P],
         cs: Cs <:< Traversable[C],
         link: LinkPoly.Case[P, C])
        : MatchCaseImpl[Ps, P, Cs, C]
        = new MatchCaseImpl[Ps, P, Cs, C]
    }
}
