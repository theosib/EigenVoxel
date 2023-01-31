package org.theosib.Geometry

import org.theosib.Utils.SortedArrayBuffer

class CollisionShape {
  val collision: SortedArrayBuffer[AxisAlignedBox] = new SortedArrayBuffer[AxisAlignedBox]()

  def insert(axisAlignedBox: AxisAlignedBox): Unit = {
    collision.insert(axisAlignedBox)
  }

  def append(axisAlignedBox: AxisAlignedBox): Unit = {
    collision.append(axisAlignedBox)
  }

  def getBuffer = collision
  def getArray = collision.getArray

  def sort(): CollisionShape = {
    collision.sort()
    this
  }
}
