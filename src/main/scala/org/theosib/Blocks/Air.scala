package org.theosib.Blocks

import org.joml.Vector3dc
import org.theosib.Geometry.CollisionShape
import org.theosib.GraphicsEngine.Mesh
import org.theosib.WorldElements.{BlockImpl, BlockLibrary}

object Air extends BlockImpl {
  val mesh = new Mesh
  val collision = new CollisionShape()

  /**
   * @return Default mesh according to the block config file
   */
  override def getDefaultMesh(): Mesh = mesh

  /**
   * @return Default collision shape according to block config file
   */
  override def getDefaultCollision(offset: Vector3dc): CollisionShape = collision

  /**
   * @return Name of the type of block
   */
  override def getName: String = "Air"
}
