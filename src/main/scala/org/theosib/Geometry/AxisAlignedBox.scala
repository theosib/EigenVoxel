package org.theosib.Geometry

import org.theosib.Position.BlockPos
import org.theosib.Utils.SortedArrayBuffer
import org.joml.{Vector3d, Vector3dc}

import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

class AxisAlignedBox extends Ordered[AxisAlignedBox] {
  val neg = new Vector3d() // Minimum x,y,z of box
  val pos = new Vector3d() // Maximum x,y,z of box

  /**
   * Make Box from x,y,z values
   * @param x1
   * @param y1
   * @param z1
   * @param x2
   * @param y2
   * @param z2
   */
  def this(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) = {
    this()
    neg.x = x1.min(x2)
    neg.y = y1.min(y2)
    neg.z = z1.min(z2)
    pos.x = x1.max(x2)
    pos.y = y1.max(y2)
    pos.z = z1.max(z2)
  }

  /**
   * Make Box from BlockPos
   * @param blockPos
   */
  def this(blockPos: BlockPos) = {
    this()
    neg.x = blockPos.X
    neg.y = blockPos.Y
    neg.z = blockPos.Z
    pos.x = blockPos.X + 1
    pos.y = blockPos.Y + 1
    pos.z = blockPos.Z + 1
  }

  /**
   * Compute copy of this box translated in space
   * @param motion
   * @return
   */
  def offset(motion: Vector3dc): AxisAlignedBox = {
    val b = new AxisAlignedBox()
    neg.add(motion, b.neg)
    pos.add(motion, b.pos)
    b
  }

  /**
   * Compute copy of this box translated in space
   * @param offsetX
   * @param offsetY
   * @param offsetZ
   * @return
   */
  def offset(offsetX: Double, offsetY: Double, offsetZ: Double): AxisAlignedBox = {
    val b = new AxisAlignedBox()
    b.neg.x = neg.x + offsetX
    b.neg.y = neg.y + offsetY
    b.neg.z = neg.z + offsetY
    b.pos.x = pos.x + offsetX
    b.pos.y = pos.y + offsetY
    b.pos.z = pos.z + offsetY
    b
  }

  /**
   * Compute if this box intersects another box
   * @param that
   * @return
   */
  def intersects(that: AxisAlignedBox): Boolean = {
    if (that.neg.x >= pos.x) return false
    if (that.neg.y >= pos.y) return false
    if (that.neg.z >= pos.z) return false
    if (that.pos.x <= neg.x) return false
    if (that.pos.y <= neg.y) return false
    if (that.pos.z <= neg.z) return false
    true
  }

  /**
   * Compute which integer-aligned block positions intersect with this Box
   * @param arr
   */
  def intersectingBlocks(): SortedArrayBuffer[BlockPos] = {
    val arr = new SortedArrayBuffer[BlockPos]()
    val baseX = neg.x.floor.toInt
    val baseY = neg.y.floor.toInt
    val baseZ = neg.z.floor.toInt
    val endX  = pos.x.ceil.toInt
    val endY  = pos.y.ceil.toInt
    val endZ  = pos.y.ceil.toInt
    for (x <- baseX until endX) {
      for (y <- baseY until endY) {
        for (z <- baseZ until endZ) {
          arr.append(new BlockPos(x, y, z))
        }
      }
    }
    arr.sort()
  }

  /**
   * Compute how far along vector 'path' that 'that' can travel before it collides with 'this'.
   * Note that a margin of 0.001 is enforced.
   * @param that Box that's moving
   * @param path Direction of motion basis vector
   * @return distance
   */
  def collisionDistance(that: AxisAlignedBox, path: Vector3dc): Double = {
    if (intersects(that)) return -1;
    var r: Double = -1

    def accumulate(r1: Double): Unit = {
      if (r1 >= 0) {
        if (r < 0 || r1 < r) r = r1
      }
    }

    // this east or west of that
    if (path.x != 0) {
      accumulate(((that.pos.x + 0.001) - neg.x) / path.x())
      accumulate(((that.neg.x - 0.001) - pos.x) / path.x())
    }

    // this above or below that
    if (path.y != 0) {
      accumulate(((that.pos.y + 0.001) - neg.y) / path.y())
      accumulate(((that.neg.y - 0.001) - pos.y) / path.y())
    }

    // this north or south of that
    if (path.z != 0) {
      accumulate(((that.pos.z + 0.001) - neg.z) / path.z())
      accumulate(((that.neg.z - 0.001) - pos.z) / path.z())
    }

    r
  }


  /**
   * The distance between two boxes along some axis. If there cartesian axis along which one box could move
   * to collide with the other, Double.MaxValue is returned. If they overlap in space, -1 is returned.
   * @param that
   * @return
   */
  def distance(that: AxisAlignedBox): Double = {
    val x_overlap = !(that.neg.x >= pos.x || that.pos.x <= neg.x)
    val y_overlap = !(that.neg.y >= pos.y || that.pos.y <= neg.y)
    val z_overlap = !(that.neg.z >= pos.z || that.pos.z <= neg.z)

    if (x_overlap && y_overlap && z_overlap) return -1

    if (x_overlap && y_overlap) {
      if (that.neg.z > pos.z) {
        return that.neg.z - pos.z
      } else {
        return neg.z - that.pos.z
      }
    }

    if (z_overlap && y_overlap) {
      if (that.neg.x > pos.x) {
        return that.neg.x - pos.x
      } else {
        return neg.x - that.pos.x
      }
    }

    if (x_overlap && z_overlap) {
      if (that.neg.y > pos.y) {
        return that.neg.y - pos.y;
      } else {
        return neg.y - that.pos.y;
      }
    }

    Double.MaxValue
  }

  def minDistance(boxArr: SortedArrayBuffer[AxisAlignedBox]): Double = {
    boxArr.arr.foldLeft(Double.MaxValue)((dist, that) => dist.min(this.distance(that)))
  }





  override def compare(that: AxisAlignedBox): Int = {
    if (neg.y < that.neg.y) return -1
    if (neg.y > that.neg.y) return 1;
    if (neg.x < that.neg.x) return -1;
    if (neg.x > that.neg.x) return 1;
    if (neg.z < that.neg.z) return -1;
    if (neg.z > that.neg.z) return 1;
    0
  }

}
