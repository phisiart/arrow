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

trait NodeUntyped

trait Node[I, O] extends NodeUntyped

case class FunctionNode[I, O](func: I => O) extends Node[I, O]

trait InUntyped

trait In[I] extends InUntyped

// NodeIn
trait NodeInUntyped {
    val node: NodeUntyped

    override def toString = s"<${node.hashCode()}>"
}

class NodeIn[I, O](val node: Node[I, O]) extends In[I] with NodeInUntyped

// InHd
trait InHdUntyped extends InUntyped {
    val in: InUntyped

    override def toString = s"$in.hd"
}

class InHd[IH, I <: HList](val in: In[I])
    extends In[IH] with InHdUntyped

// InTl
trait InTlUntyped extends InUntyped {
    val in: InUntyped

    override def toString = s"$in.tl"
}

class InTl[IT <: HList, I <: HList](val in: In[I])
    extends In[IT] with InTlUntyped

/** [[InList]] */
trait InListUnTyped extends InUntyped {
    val in: InUntyped
    val id: Int

    override def toString = s"$in[$id]"
}

class InList[I, Is](val in: In[Is], val id: Int)
    extends In[I] with InListUnTyped

trait OutUntyped

trait Out[O] extends OutUntyped

/** [[NodeOut]] */
trait NodeOutUntyped {
    val node: NodeUntyped

    override def toString = s"<${node.hashCode()}>"
}

class NodeOut[I, O](val node: Node[I, O])
    extends Out[O] with NodeOutUntyped

/** [[OutHd]] */
trait OutHdUntyped {
    val out: OutUntyped

    override def toString = s"$out.hd"
}

class OutHd[OH, O <: HList](val out: Out[O])
    extends Out[OH] with OutHdUntyped

/** [[OutTl]] */
trait OutTlUntyped {
    val out: OutUntyped

    override def toString = s"$out.tl"
}

class OutTl[OT <: HList, O <: HList](val out: Out[O])
    extends Out[OT] with OutTlUntyped

/** [[OutList]] */
trait OutListUntyped {
    val out: OutUntyped
    val id: Int

    override def toString = s"$out[$id]"
}

class OutList[O, Os](val out: Out[Os], val id: Int)
    extends Out[O] with OutListUntyped
