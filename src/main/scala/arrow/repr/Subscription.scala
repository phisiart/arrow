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

import arrow._

sealed trait Subscription {
    val from: OutUntyped
    val to: InUntyped
}

sealed trait SubscriptionFrom[T] extends Subscription
sealed trait SubscriptionTo[T] extends Subscription

final class SubscriptionImpl[T](val from: Out[T], val to: In[T])
    extends SubscriptionFrom[T] with SubscriptionTo[T] {

    override def toString = s"$from -> $to"
}

final class SubscriptionRImpl[RT, T](val from: Out[RT], val to: In[T])
                                    (implicit rT: RT <:< R[T])
    extends SubscriptionFrom[RT] with SubscriptionTo[T] {

    override def toString = s"$from ->R $to"
}
