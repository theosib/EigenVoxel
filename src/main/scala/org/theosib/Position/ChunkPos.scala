package org.theosib.Position

import org.theosib.Utils.{Facing, Packing}

class ChunkPos(val X: Int, val Y: Int, val Z: Int) extends Ordered[ChunkPos] {
  def this() = this(0, 0, 0)
  def this(blockPos: BlockPos) = {
    this(blockPos.X >> 4, blockPos.Y >> 4, blockPos.Z >> 4)
  }
//  def this(packed: Long) = {
//    this(((packed >> Packing.chunkX_shift) & Packing.chunkX_mask).toInt,
//      ((packed >> Packing.chunkY_shift) & Packing.chunkY_mask).toInt,
//      ((packed >> Packing.chunkZ_shift) & Packing.chunkZ_mask).toInt)
//  }

  override def toString: String = {
    s"ChunkPos(${X},${Y},${Z})"
  }

  def packed(): Long = {
    val x: Long = (X & Packing.chunkX_mask)
    val y: Long = (Y & Packing.chunkY_mask)
    val z: Long = (Z & Packing.chunkZ_mask)
    (x << Packing.chunkX_shift) | (y << Packing.chunkY_shift) | (z << Packing.chunkZ_shift)
  }

  def down(dist: Int = 1): ChunkPos = new ChunkPos(X, Y - dist, Z)
  def up(dist: Int = 1): ChunkPos = new ChunkPos(X, Y + dist, Z)
  def north(dist: Int = 1): ChunkPos = new ChunkPos(X, dist, Z - dist)
  def south(dist: Int = 1): ChunkPos = new ChunkPos(X, dist, Z + dist)
  def west(dist: Int = 1): ChunkPos = new ChunkPos(X - dist, dist, Z)
  def east(dist: Int = 1): ChunkPos = new ChunkPos(X + dist, dist, Z)

  def offset(x: Int, y: Int, z: Int): ChunkPos = new ChunkPos(X + x, Y + y, Z + z);
  def offset(vec: (Int, Int, Int)): ChunkPos = new ChunkPos(X + vec._1, Y + vec._2, Z + vec._3)

  def neighbor(face: Int): ChunkPos = offset(Facing.int_tuple(face))

  override def compare(that: ChunkPos): Int = {
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
      case that: ChunkPos => compare(that) == 0
      case _ => false
    }
  }

  override def hashCode(): Int = {
    val p = packed()
    ((p >>> 32) ^ p).toInt
  }
}
