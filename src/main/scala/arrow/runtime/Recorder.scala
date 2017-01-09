/*
 * The MIT License
 *
 * Copyright (c) 2017 Zhixun Tan
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

package arrow.runtime

import java.util.concurrent.locks._

import scala.collection.mutable.ArrayBuffer

final class Recorder {
    private val _lock = new ReentrantLock
    private var _granted = false
    private val _cond = this._lock.newCondition()

    private val _record = ArrayBuffer.empty[Int]

    def lock(id: Int): Unit = {
        this._lock.lock()

        try {
            while (this._granted) {
                this._cond.await()
            }

            Runtime.log.info(s"[$id] acquired recorder lock.")
            this._granted = true

        } finally {
            this._lock.unlock()
        }
    }

    def unlock(id: Int): Unit = {
        this._lock.lock()

        try {
            Runtime.log.info(s"[$id] released recorder lock.")
            this._granted = false
            this._record.append(id)
            this._cond.signal()

        } finally {
            this._lock.unlock()
        }
    }

    def record: Seq[Int] = this._record
}

final class Replayer(val record: BufferedIterator[Int]) {
    private val _lock = new ReentrantLock
    private val _cond = this._lock.newCondition()

    def lock(id: Int): Unit = {
        this._lock.lock()

        try {
            while (this.record.hasNext && this.record.head != id) {
                _cond.await()
            }
            Runtime.log.info(s"[$id] acquired replayer lock.")
        } finally {
            this._lock.unlock()
        }
    }

    def unlock(id: Int): Unit = {
        this._lock.lock()

        try {
            Runtime.log.info(s"[$id] released replayer lock.")
            if (this.record.hasNext) {
                this.record.next()
            }
            this._cond.signalAll()
        } finally {
            this._lock.unlock()
        }
    }
}
