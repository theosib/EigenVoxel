package org.theosib.WorldElements

import jdk.javadoc.internal.doclint.HtmlTag.BlockType
import org.theosib.Geometry.CollisionShape
import org.theosib.GraphicsEngine.Mesh
import org.theosib.Position.BlockPos

/**
 * This is the class for an object that is created whenever information about a real block
 * from anywhere in the world is requested.
 */
class Block(private val chunk: Chunk, private val pos: BlockPos, private val storageIndex: Int, private val impl: BlockImpl) {
//  var chunk: Chunk = null    // Chunk this block belongs to
//  var pos: BlockPos = null        // Coordinates of block in world
//  var storageIndex: Int = 0      // Index of block in subchunk
//  var impl: BlockImpl = null    // Implementation of this type of block

  def getBlockImpl(): BlockImpl = impl
  def getStorageIndex(): Int = storageIndex
  def getChunk(): Chunk = chunk
  def getBlockPos(): BlockPos = pos

  /**
   * @return The visible faces according to the containing chunk
   */
  def getVisibleFaces(): Int = chunk.getChunkView.visibleFaces(storageIndex)

  // XXX Some of these things will later come from the chunk
  def getMesh(): Mesh = chunk.getMesh(storageIndex)
  def getCollision(): CollisionShape = getMesh().getCollision()
//  def getHitbox(): CollisionShape = getMesh().getHitbox(chunk)

  def hitAction(face: Int): Boolean = impl.hitAction(this, face)
  def useAction(face: Int): Boolean = impl.useAction(this, face)
  def tickEvent(tickTypes: Int): Unit = impl.tickEvent(this, tickTypes)
  def updateEvent(): Unit = impl.updateEvent(this)
  def repaintEvent(): Unit = impl.repaintEvent(this)
  def placeEvent(): Unit = impl.placeEvent(this)
  def breakEvent(): Unit = impl.breakEvent(this)

  def isAir(): Boolean = (impl == null)

  override def toString: String = {
    s"Block(${pos})"
  }
}
