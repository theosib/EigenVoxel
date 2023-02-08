package org.theosib.WorldElements

import org.joml.Vector3dc
import org.theosib.Geometry.CollisionShape
import org.theosib.GraphicsEngine.Mesh

/**
 * Base class of all block types in the world. All default block attributes are retrieved from a subclass of this.
 * This is where we implement the core functionality of every block of a given type, and there is only one for
 * all blocks of the same type. There is also a Block class whose instances are used to represent a real block in the
 * world with location and state.
 *
 * Return values of action events indicate that the action has been consumed.
 */
abstract class BlockImpl {
  /**
   * @return Default mesh according to the block config file
   */
  def getDefaultMesh(): Mesh

  /**
   * Override this for dynamic appearance
   * @return Customized mesh
   */
  def getMesh(chunk: Chunk): Mesh = {
    getDefaultMesh()
  }

  /**
   * @return Default collision shape according to block config file
   */
  def getDefaultCollision(offset: Vector3dc): CollisionShape

  /**
   * Override tis for dynamic collision shape
   * @return
   */
  def getCollision(block: Block): CollisionShape = {
    getDefaultCollision(block.getBlockPos().toVector3d)
  }

  /**
   * @return Default hitbox according to block config file
   */
  def getDefaultHitbox(offset: Vector3dc): CollisionShape = {
    getDefaultCollision(offset)
  }

  /**
   * Override this for dynamic hitbox
   * @return
   */
  def getHitbox(block: Block): CollisionShape = {
    getDefaultHitbox(block.getBlockPos().toVector3d)
  }

  /**
   * Left click action
   * @param block Block location and state
   * @param face Which face of the block was clicked
   * @return Action consumed
   */
  def hitAction(block: Block, face: Int): Boolean = false

  /**
   * Right click action
   * @param block
   * @param face
   * @return Action consumed
   */
  def useAction(block: Block, face: Int): Boolean = false

  /**
   * Game tick event
   * @param block
   * @param tickTypes
   */
  def tickEvent(block: Block, tickTypes: Int): Unit = {}

  /**
   * Handle block being broken. The job of this is to drop an item if appropriate
   * @param block
   */
  def breakEvent(block: Block): Unit = {}

  /**
   * Handle block being placed. The block gets created and placed first, then the event is generated.
   * @param block
   */
  def placeEvent(block: Block): Unit = {}

  /**
   * Block update that occurs when neighboring blocks perform actions
   * @param block
   */
  def updateEvent(block: Block): Unit = {}

  /**
   * Handle any reshaping that might occur when neighbors are broken, places, or reshaped
   * @param block
   */
  def repaintEvent(block: Block): Unit = {}

  def wantsGameTicks(): Boolean = false

  /**
   * @return Name of the type of block
   */
  def getName: String

  override def toString: String = getName
}
