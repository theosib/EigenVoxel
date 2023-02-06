package org.theosib.Geometry

import org.joml.{Vector3d, Vector3dc}

class EntityBox() {
  var position: Vector3d = new Vector3d()
  var width: Double = 0.0
  var height: Double = 0.0

  def this(pos: Vector3dc, w: Double, h: Double) = {
    this()
    position.set(pos)
    width = w
    height = h
  }

  override def clone(): EntityBox = {
    val that = new EntityBox
    that.position.set(position)
    that.height = height
    that.width = width
    that
  }

  def getAxisAlignedBox: AxisAlignedBox = {
    val halfWidth = width * 0.5
    new AxisAlignedBox(position.x - halfWidth, position.y + height, position.z - halfWidth,
      position.x + halfWidth, position.y, position.z + halfWidth)
  }
}

