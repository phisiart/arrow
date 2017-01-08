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

sealed trait R[T]

//sealed trait Reusable[T] extends Inputable[T]

//sealed trait Replaceable[T] extends Inputable[T]

sealed trait Inputable[T] extends R[T] {
    val reusable: Boolean
    val replaceable: Boolean
}

final case class Value[T]
(value: T,
 reusable: Boolean = false,
 replaceable: Boolean = false) extends Inputable[T] {
}

final case class Ignore[T]() extends R[T]

final case class Finish[T]() extends Inputable[T] {
    override val reusable: Boolean = false
    override val replaceable: Boolean = false
}

final case class Break[T]() extends Inputable[T] {
    override val reusable: Boolean = false
    override val replaceable: Boolean = false
}

final case class Empty[T]() extends Inputable[T] {
    override val reusable: Boolean = false
    override val replaceable: Boolean = false
}
