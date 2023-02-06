package org.theosib.WorldElements

import org.theosib.Adaptors.{Disposable, RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.Geometry.GeometryFunctions
import org.theosib.GraphicsEngine.{MeshRenderer, RenderingUtils, Shader}
import org.theosib.Position.BlockPos
import org.theosib.Utils.{Disposer, Facing, WindowDimensions}
import org.joml.{Matrix4f, Matrix4fc, Vector3dc, Vector3f, Vector3fc, Vector4f}

import java.util.concurrent.ConcurrentLinkedDeque
import scala.collection.mutable.ArrayBuffer

class WorldView(val world: World, val camera: CameraModel) extends RenderAgent {
  // XXX keep track of chunk updates and decide not to render when no changes

  def getCamera(): CameraModel = camera

  var blockShader: Shader = null
  var entityShader: Shader = null

  /**
   * Called from UpdateTenderThread, recompute all MeshRenderer objects for all visually updated chunks
   * @param camera
   */
  def computeChunkAndEntityRenders(camera: CameraModel): Unit = {
    // Get the camera position at the time of computing renders. This will get stored in mesh renderers
    // and used when they're drawn. By then, the camera might have moved a bit, but all the rendering has
    // to be computed relative to the position chosen at this time so that everything shows up in the right place.
    val cameraPos = camera.getPos()
    val viewCenter = GeometryFunctions.worldViewCenter(cameraPos)
    val viewMatrix = camera.getViewMatrix(viewCenter)
    val chunks = world.listAllChunks()
    chunks.foreach { chunk =>
      val chunkView = chunk.getChunkView
      chunkView.setProjectionMatrix(projectionMatrix) // Needed for frustum culling
      chunkView.computeVisualUpdates(viewCenter, viewMatrix)
    }

    val entities = world.entityStore
    entities.forEach { entity =>
      entity.setProjectionMatrix(projectionMatrix)
      entity.computeVisualUpdates(viewCenter, viewMatrix)
    }
  }

  def computeEntityRenders(camera: CameraModel): Unit = {

  }

  /**
   * Draw all visible chunks to the screen
   */
  def draw(): Unit = {
    RenderingUtils.disableBlend()

    val chunks = world.listAllChunks()
    chunks.foreach { chunk =>
      chunk.getChunkView.draw(blockShader, camera)
    }

    // Entities

    RenderingUtils.enableBlend()

    drawTrans(chunks)
  }

  def drawEntities(): Unit = {

  }

  /**
   * Draw all translucent blocks in order from far to near the viewer
   * @param chunks
   */
  def drawTrans(chunks: Iterable[Chunk]): Unit = {
    var allTrans = new ArrayBuffer[MeshRenderer]
    // Make one huge list of transparent blocks in the world
    chunks.foreach { chunk =>
      val trans = chunk.getChunkView.getTransRenders()
      trans.foreach { mr =>
        if (mr.isValid) allTrans += mr
      }
    }

    val cameraPos = camera.getPos()

    allTrans = allTrans.sortWith { (a, b) =>
      val da = a.getTransSortPosition.distanceSquared(cameraPos)
      val db = b.getTransSortPosition.distanceSquared(cameraPos)
      db < da
    }

    allTrans.foreach { mr =>
      val viewMatrix = camera.getViewMatrix(mr.getViewCenter)

      // Frustum check??

      blockShader.setMat4("view", viewMatrix);
      mr.loadGLBuffers()
      mr.draw(blockShader)
    }
  }

  var projectionMatrix: Matrix4fc = null

  /**
   * Set/load the projection matrix
   * @param projection
   */
  def setProjection(projection: Matrix4fc): Unit = {
    projectionMatrix = projection
    blockShader.setMat4("projection", projection)
    // entityShader.setMat4("projection", matrix);
    //      placementShader.setMat4("projection", matrix);
  }

  override def destroy(): Unit = {
    Disposer.dispose(blockShader)
    // Other shaders XXX
  }

  var window: Window = null;
  override def create(w: Window): Unit = {
    window = w;
    blockShader = new Shader().setFragmentCodeFile("block_fragment.glsl").setVertexCodeFile("block_vertex.glsl")
  }

  override def willRender(w: Window): Boolean = true // XXX

  override def render(w: Window): Unit = {
    draw()
  }

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    setProjection(w.getProjectionMatrix)
  }

  override def priority(): Int = 0
}

object WorldView {
  /**
   * Compute if a position is outside of the frustum
   * http://web.archive.org/web/20120531231005/http://crazyjoke.free.fr/doc/3D/plane%20extraction.pdf
   * @param transform the product of view and projection matrices
   * @param adustedPos
   * @return Nonzero if the point is outside of the
   */
  def outsideFrustum(transform: Matrix4f, adjustedPos: BlockPos): Int = {
    var result: Int = 0;

    val point = new Vector4f()
    val row1 = new Vector4f()
    val row2 = new Vector4f()
    val row3 = new Vector4f()
    val row4 = new Vector4f()
    point.x = adjustedPos.X
    point.y = adjustedPos.Y
    point.z = adjustedPos.Z
    point.w = 1f
    transform.getRow(0, row1)
    transform.getRow(1, row2)
    transform.getRow(2, row3)
    transform.getRow(3, row4)

    val sum41 = new Vector4f()
    val dif41 = new Vector4f()
    val sum42 = new Vector4f()
    val dif42 = new Vector4f()
    val sum43 = new Vector4f()
    val dif43 = new Vector4f()
    row4.add(row1, sum41)
    row4.sub(row1, dif41)
    row4.add(row2, sum42)
    row4.sub(row2, dif42)
    row4.add(row3, sum43)
    row4.sub(row3, dif43)

    if (0 > point.dot(sum41)) result |= Facing.WEST_MASK
    if (0 > point.dot(dif41)) result |= Facing.EAST_MASK
    if (0 > point.dot(sum42)) result |= Facing.DOWN_MASK
    if (0 > point.dot(dif42)) result |= Facing.UP_MASK
    if (0 > point.dot(sum43)) result |= Facing.SOUTH_MASK
    if (0 > point.dot(dif43)) result |= Facing.NORTH_MASK

    result
  }

}