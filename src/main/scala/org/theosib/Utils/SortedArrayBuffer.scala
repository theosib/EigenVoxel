package org.theosib.Utils

import scala.collection.mutable.ArrayBuffer

class SortedArrayBuffer[T <: Ordered[T]] {
  val arr = new ArrayBuffer[T]()

  def getArray = arr

  def length = arr.length

  def insert(pos: T): SortedArrayBuffer[T] = {
    val ix = arr.lastIndexWhere(_ < pos)
    arr.insert(ix + 1, pos)
    this
  }

  def append(pos: T): SortedArrayBuffer[T] = {
    arr.append(pos)
    this
  }

  def sort(): SortedArrayBuffer[T] = {
    arr.sortWith((a, b) => a.compare(b) < 0)
    this
  }

  /**
   * Compute the difference between two two lists. This requires that the two arrays be already sorted,
   * and it really only works for integer-aligned boxes.
   * @param that
   * @return
   */
  def difference(that: SortedArrayBuffer[T]): SortedArrayBuffer[T] = {
    val out = new SortedArrayBuffer[T]()

    var i = 0
    var j = 0

    while (i<arr.length && j<that.arr.length) {
      val a = arr(i)
      val b = that.arr(j)
      val cmp = a.compare(b)
      if (cmp < 0) {
        out.append(a)
        i += 1
      } else if (cmp > 0) {
        j += 1
      } else {
        i += 1
        j += 1
      }
    }
    while (i<arr.length) {
      out.append(arr(i))
      i += 1
    }

    out
  }
}
