package org.theosib.Position

import org.theosib.Utils.{Facing, Packing}
import org.joml.{Vector3d, Vector3dc, Vector3f, Vector3fc}

import scala.::

class BlockPos(val X: Int, val Y: Int, val Z: Int) extends Ordered[BlockPos] {
  def this() = this(0, 0, 0)
  def this(v : Vector3dc) = this(v.x().floor.toInt, v.y().floor.toInt, v.z().floor.toInt)
  // def this(packed: Long)
  def this(chunkPos: ChunkPos) = this(chunkPos.X << 4, chunkPos.Y << 4, chunkPos.Z << 4)

  def getChunkPos(): ChunkPos = {
    new ChunkPos(this)
  }

  lazy val toVector3d = new Vector3d(X, Y, Z)

  override def toString: String = {
    s"BlockPos(${X},${Y},${Z})"
  }

  def packed(): Long = {
    val x: Long = (X & Packing.blockX_mask)
    val y: Long = (Y & Packing.blockY_mask)
    val z: Long = (Z & Packing.blockZ_mask)
    (x << Packing.blockX_shift) | (y << Packing.blockY_shift) | (z << Packing.blockZ_shift)
  }

  def packedAsChunkPos(): Long = {
    val x: Long = ((X >> 4) & Packing.chunkX_mask)
    val y: Long = ((Y >> 4) & Packing.chunkY_mask)
    val z: Long = ((Z >> 4) & Packing.chunkZ_mask)
    (x << Packing.chunkX_shift) | (y << Packing.chunkY_shift) | (z << Packing.chunkZ_shift)
  }

  def chunkIndex(): Int = {
    val x = X & 15;
    val y = Y & 15;
    val z = Z & 15;
    x | (z<<4) | (y<<8);
  }

  def down(dist: Int = 1): BlockPos = new BlockPos(X, Y-dist, Z)
  def up(dist: Int = 1): BlockPos = new BlockPos(X, Y + dist, Z)
  def north(dist: Int = 1): BlockPos = new BlockPos(X, dist, Z - dist)
  def south(dist: Int = 1): BlockPos = new BlockPos(X, dist, Z + dist)
  def west(dist: Int = 1): BlockPos = new BlockPos(X - dist, dist, Z)
  def east(dist: Int = 1): BlockPos = new BlockPos(X + dist, dist, Z)

  def offset(x: Int, y: Int, z: Int): BlockPos = new BlockPos(X + x, Y + y, Z + z);
  def offset(vec: (Int, Int, Int)): BlockPos = new BlockPos(X + vec._1, Y + vec._2, Z + vec._3)

  def neighbor(face: Int): BlockPos = offset(Facing.int_tuple(face))

  def allNeighbors(includeSelf: Boolean = false): Array[BlockPos] = {
    val neighbors = Facing.int_tuple.map(t => offset(t))
    if (includeSelf) {
      neighbors :+ this
    } else {
      neighbors
    }
  }

  def allSurrounding(includeSelf: Boolean = false): Array[BlockPos] = {
    val arr = new Array[BlockPos](26 + (if (includeSelf) 1 else 0))
    var i = 0
    for (y <- -1 to 1; z <- -1 to 1; x <- -1 to 1) {
      val skip = x==0 && y==0 && z==0 && !includeSelf
      if (!skip) {
        arr(i) = offset(x, y, z);
        i += 1
      }
    }
    arr
  }

  override def compare(that: BlockPos): Int = {
    if (Y < that.Y) return -1;
    if (Y > that.Y) return 1;
    if (X < that.X) return -1;
    if (X > that.X) return 1;
    if (Z < that.Z) return -1;
    if (Z > that.Z) return 1;
    0
  }

  override def equals(that: Any): Boolean = {
    that match {
      case that: BlockPos => compare(that) == 0
      case _ => false
    }
  }

  override def hashCode(): Int = {
    val p = packed()
    ((p >>> 32) ^ p).toInt
  }
}

object BlockPos {
  def fromVector(vec: Vector3dc) = {
    val x = math.floor(vec.x()).toInt
    val y = math.floor(vec.y()).toInt
    val z = math.floor(vec.z()).toInt
    new BlockPos(x, y, x)
  }
}