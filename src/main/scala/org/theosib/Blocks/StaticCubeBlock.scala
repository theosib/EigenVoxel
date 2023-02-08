package org.theosib.Blocks

import org.joml.Vector3dc
import org.theosib.Geometry.CollisionShape
import org.theosib.GraphicsEngine.Mesh
import org.theosib.WorldElements.BlockImpl

class StaticCubeBlock(val name: String) extends BlockImpl {
  val default_mesh = new Mesh
  default_mesh.loadMesh(name)

  /**
   * @return Default mesh according to the block config file
   */
  override def getDefaultMesh(): Mesh = default_mesh

  /**
   * @return Default collision shape according to block config file
   */
  override def getDefaultCollision(offset: Vector3dc): CollisionShape = default_mesh.getCollision(offset)

  /**
   * @return Name of the type of block
   */
  override def getName: String = name
}
