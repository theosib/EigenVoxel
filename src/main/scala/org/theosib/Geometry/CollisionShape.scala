package org.theosib.Geometry

import org.joml.Vector3dc
import org.theosib.Utils.SortedArrayBuffer

class CollisionShape(val collision: SortedArrayBuffer[AxisAlignedBox]) extends Iterable[AxisAlignedBox] {
  def this() = {
    this(new SortedArrayBuffer[AxisAlignedBox]())
  }

  def offset(offset: Vector3dc): CollisionShape = {
    val that = new CollisionShape()
    collision.foreach { aab =>
      that.append(aab.offset(offset))
    }
    that
  }

  def insert(axisAlignedBox: AxisAlignedBox): Unit = {
    collision.insert(axisAlignedBox)
  }

  def append(axisAlignedBox: AxisAlignedBox): Unit = {
    collision.append(axisAlignedBox)
  }

  def difference(that: CollisionShape): CollisionShape = {
    new CollisionShape(collision.difference(that.collision))
  }

  def minDistance(box: AxisAlignedBox): Double = {
    box.minDistance(collision)
  }

  def getBuffer = collision
  def getArray = collision.getArray

  def sort(): CollisionShape = {
    collision.sort()
    this
  }

  override def iterator: Iterator[AxisAlignedBox] = collision.iterator
}
