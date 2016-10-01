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

package arrow.repr


import java.awt.Desktop
import java.io._

import scalax.collection.edge._
import scalax.collection.io.dot._
import scalax.collection.io.dot.implicits._
import scalax.collection._

object GraphDrawer {
    type GraphT = GraphBase[Processor, LDiEdge]
    type EdgeT = GraphT#EdgeT
    type NodeT = GraphT#NodeT

    val root = DotRootGraph(
        directed = true,
        id = Some("Dot Graph"),
        attrStmts = List(
            DotAttrStmt(Elem.node, List(DotAttr("shape", "circle"))),
            DotAttrStmt(Elem.graph, List(DotAttr("rankdir", "LR")))
        )
    )

    def getProcessorName(processor: Processor): String = {
        val _ = processor match {
            case NodeProcessor(node, id) =>
                s"Node[$id]"

            case Splitter(out) =>
                "S"

            case Joiner(in) =>
                "J"

            case HSplitter(out) =>
                "HS"

            case HJoiner(in) =>
                "HJ"
        }

        processor.toString
    }

    def getProcessorShape(processor: Processor) = {
        processor match {
            case NodeProcessor(node, id) =>
                "circle"

            case Splitter(out) =>
                "record"

            case Joiner(in) =>
                "record"

            case HSplitter(out) =>
                "record"

            case HJoiner(in) =>
                "record"
        }
    }

    def nodeTransformer(innerNode: NodeT): Option[(DotGraph, DotNodeStmt)] = {
        val shape = getProcessorShape(innerNode.value)
        val attrs = List(DotAttr("shape", shape))

        val name = getProcessorName(innerNode.value)

        Some(root, DotNodeStmt(name, attrs))
    }

    def edgeTransformer(innerEdge: EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
        innerEdge.edge match {
            case LDiEdge(source, target, label) =>
                Some(
                    (root, DotEdgeStmt(source.toString, target.toString))
                )
        }
    }

    def draw(processors: Iterable[Processor], subscriptions: Iterable[Subscription]) = {
//        val graph = mutable.Graph.empty[Processor, LDiEdge]

        val builder = StringBuilder.newBuilder

        builder.append(s"""digraph "Graph" {\n""")
        builder.append(" node [shape = circle]\n")
        builder.append(" graph [rankdir = LR]\n")

        for (processor <- processors) {
            builder.append(s""" "${getProcessorName(processor)}" [shape = ${getProcessorShape(processor)}]\n""")
//            graph.add(processor)
        }

        for (subscription <- subscriptions) {
            val (from: Processor, label: String) = subscription.from match {
                case SingleOutputProcessorOut(processor) =>
                    (processor, "")

                case HSplitterHdOut(hSplitter) =>
                    (hSplitter, "hd")

                case HSplitterTlOut(hSplitter) =>
                    (hSplitter, "tl")

                case SplitterOut(splitter, id) =>
                    (splitter, s"$id")
            }

            val to: Processor = subscription.to match {
                case SingleInputProcessorIn(processor) =>
                    processor

                case HJoinerHdIn(hJoiner) =>
                    hJoiner

                case HJoinerTlIn(hJoiner) =>
                    hJoiner

                case JoinerIn(joiner, idx) =>
                    joiner
            }

            builder.append(s""" "${getProcessorName(from)}" -> "${getProcessorName(to)}" [label = "$label"]\n""")

//            graph.add(
//                LDiEdge(from, to)(label)
//            )
        }

        builder.append("}\n")

        import scala.sys.process._

//        val export = new Export(graph)
//        val dot = export.toDot(root, edgeTransformer, cNodeTransformer = Some(nodeTransformer _))

        val dot = builder.mkString
        println(dot)

        val dotFile = new File("graph.dot")
        val dotFileWriter = new PrintWriter(dotFile)
        dotFileWriter.write(dot)
        dotFileWriter.close()

        val dotRet = "dot -Tpdf graph.dot -o graph.pdf" ! ; // this stupid semicolon
        println(dotRet)

        val svgFile = new File("graph.pdf")

        Desktop.getDesktop.browse(svgFile.toURI)
    }
}
