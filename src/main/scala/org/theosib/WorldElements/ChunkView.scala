package org.theosib.WorldElements

import org.theosib.Adaptors.Disposable
import org.theosib.Camera.CameraModel
import org.theosib.GraphicsEngine.{Mesh, MeshRenderer, Shader, Texture}
import org.theosib.Position.BlockPos
import org.theosib.Utils.{Disposer, Facing}
import org.joml.{Matrix4f, Matrix4fc, Vector3d}
import org.theosib.WorldElements.ChunkView.{tmpFaceList, tmpMeshList, tmpPosList}

import java.util
import scala.collection.mutable.ArrayBuffer

/**
 * This represent the transient visual representations of blocks. All of this gets dynamically reconstructied
 * on chunk load.
 * @param chunk
 */
class ChunkView(val chunk: Chunk) extends Disposable {
  // Have any visual changes occurred at all?
  var chunkVisualModified: Boolean = false

  // Which blocks have been modified?
  val blockVisualModified: Array[Boolean] = new Array[Boolean](Chunk.chunkStorageSize)

  // Which block faces need to be show since they're not hidden by another block?
  val blockShowFaces: Array[Byte] = new Array[Byte](Chunk.chunkStorageSize)
  def visibleFaces(index: Int) = blockShowFaces(index)

  @volatile var renderIsValid: Boolean = false

  // Arrays of mesh renderers
  var render: ArrayBuffer[MeshRenderer] = new ArrayBuffer[MeshRenderer]()
  var render_alt: ArrayBuffer[MeshRenderer] = new ArrayBuffer[MeshRenderer]()

  // Arrays of mesh renderer for translucent blocks
  var trans: ArrayBuffer[MeshRenderer] = new ArrayBuffer[MeshRenderer]()
  var trans_alt: ArrayBuffer[MeshRenderer] = new ArrayBuffer[MeshRenderer]()

  def getTransRenders(): ArrayBuffer[MeshRenderer] = trans

  override def destroy(): Unit = {
    Disposer.dispose(render)
    Disposer.dispose(render_alt)
    Disposer.dispose(trans)
    Disposer.dispose(trans_alt)
  }

  /**
   * Mark a face visible or not
   * @param index which block
   * @param face which face
   * @param visible 1 for visible, 0 for not
   */
  def setShowFace(index: Int, face: Int, visible: Int): Unit = {
    var sf: Int = blockShowFaces(index)
    sf &= ~Facing.bitMask(face)
    sf |= Facing.bitMask(face, visible)
    blockShowFaces(index) = sf.toByte
  }

  /**
   * Indicate that a block has changed in this chunk and needs to be redrawn
   * @param index
   */
  def markBlockVisuallyUpdated(index: Int): Unit = {
    println(s"Marking ${index} needs update")
    blockVisualModified(index) = true
    chunkVisualModified = true
  }

  /**
   * Mark every block in chunk needing to be redrawn
   */
  def markChunkUpdated(): Unit = {
    for (i <- 0 until blockVisualModified.length) blockVisualModified(i) = true
    chunkVisualModified = true
  }

  var projectionMatrix: Matrix4fc = null;
  def setProjectionMatrix(proj: Matrix4fc): Unit = {
    projectionMatrix = proj;
  }

  /**
   * Compute whether or not this chunk is inside the view frustum
   * @param view View matrix from camera
   * @param viewCenter World recentering position
   * @return
   */
  def insideFrustum(view: Matrix4f, viewCenter: BlockPos): Boolean = {
    val corners = chunk.getCorners(viewCenter)
    val xform = new Matrix4f()
    projectionMatrix.mul(view, xform)

    var reduction: Int = -1
    for (i <- 0 until 8) {
      val outside = WorldView.outsideFrustum(xform, corners(i))
      reduction &= outside
      if (reduction == 0) return true
    }

    false
  }

  /**
   * Cause all block faces to be recomputed
   */
  def updateAllBlockFaces(): Unit = {
    for (i <- 0 until Chunk.chunkStorageSize) {
      val pos = chunk.indexToBlockPos(i)
      updateBlockFaces(i, pos)
    }
  }

  /**
   * Recompute which faces of a block should be shown
   * @param index which block
   * @param pos coordinates of block
   */
  def updateBlockFaces(index: Int, pos: BlockPos): Unit = {
    if (!blockVisualModified(index)) return;
    blockVisualModified(index) = false;

    val selfBlock = chunk.getBlock(pos)
    if (selfBlock.isAir()) return;

    val neighbors = chunk.world.getNeighborBlocks(pos, false, true).toSeq
    val selfMesh = selfBlock.getMesh()

    for (face <- 0 until Facing.NUM_FACES) {
      val neighborBlock = neighbors(face)
      var visible: Int = 1

      neighborBlock match {
        case Some(neighborBlock) => if (!neighborBlock.isAir()) {
          val neighborMesh = neighborBlock.getMesh()
          val oppositeFace = Facing.oppositeFace(face)
          val selfSolid = selfMesh.faceIsSolid(face)
          val neighborSolid = neighborMesh.faceIsSolid(oppositeFace)

          if (selfSolid && neighborSolid) {
            val selfTrans = selfMesh.isTranslucent
            val neighborTrans = neighborMesh.isTranslucent

            if (selfTrans) {
              if (neighborTrans) {
                if (selfMesh == neighborMesh) visible = 0
              } else {
                visible = 0
              }
            } else {
              if (!neighborTrans) visible = 0
            }
          }
        }
        case None =>
      }

      println(s"index=${index}, face=${face}, visible=${visible}")
      setShowFace(index, face, visible)
    }
  }

  /**
   * Loop over all textures and produce one MeshRenderer per opaque texture
   * @param viewCenter
   */
  def computeAllOpaqueRenders(viewCenter: BlockPos): Unit = {
    val numTex = Texture.numTexures()

    if (render.length < numTex) {
      renderIsValid = false;
      while (render.length < numTex) {
        val index = render.length
        val tex = Texture.getTexture(index)
        render.addOne(new MeshRenderer(tex))
      }
    }

    // Resize the MeshRender backbuffer to contain one per texture
    if (render_alt.length < numTex) {
      // In case update and rendering threads get badly out of sync, just temporarily disable rendering for this chunk
      renderIsValid = false;
      while (render_alt.length < numTex) {
        val index = render_alt.length
        val tex = Texture.getTexture(index)
        render_alt.addOne(new MeshRenderer(tex))
      }
    }

    // Iterate all textures
    for (index <- 0 until render_alt.length) {
      val tex = Texture.getTexture(index)
      if (tex.isValid) {
        // XXX If we ever recycle texture indices, then have to set texture for this MeshRenderer
        iterateOpaqueBlocks(tex, viewCenter)
      } else {
        render_alt(index).clear()
      }
    }
  }

  /**
   * For the given texture, collect all corresponding blocks in the chunk and compute MeshRenderer objects.
   * This gets the MeshRenderer back buffer and fills it with changed blocks and then atomically replaces
   * the front buffer with these new blocks. This should work fine unless the GL thread falls so far behind
   * that it is still rendering blocks from the back buffer when we start replacing it.
   * @param tex
   * @param viewCenter
   */
  def iterateOpaqueBlocks(tex: Texture, viewCenter: BlockPos): Unit = {
    val index = tex.getIndex
    val mr1 = render_alt(index)

    var count: Int = 0

    println(s"Iterating opaque blocks for ${chunk}, vc=${viewCenter}, tex=${tex}")

    for (index <- 0 until Chunk.chunkStorageSize) {
      val blockID = chunk.blockStorage(index)
      if (blockID != 0) {
        val mesh = chunk.getMesh(index)
        println(s"Got block ID=${blockID}, mesh=${mesh}")
        if (!mesh.isTranslucent) {
          val shapeTex = mesh.getTexture
          println(s"shapeTex=${shapeTex}, tex=${tex}")
          if (shapeTex == tex) {
            val blockPos = chunk.indexToBlockPos(index)
            tmpPosList(count) = blockPos
            tmpMeshList(count) = mesh
            tmpFaceList(count) = visibleFaces(index)
            count += 1
          }
        }
      }
    }

    mr1.loadMeshes(tmpMeshList, tmpPosList, tmpFaceList, count, viewCenter)

    val mr2 = render(index)
    render_alt(index) = mr2;
    render(index) = mr1;
  }

  /**
   * Find all translucent blocks and make one MeshRenderer for each
   * @param viewCenter
   */
  def iterateTransBlocks(viewCenter: BlockPos): Unit = {
    var trans_index = 0
    val mr1 = trans_alt

    for (index <- 0 until Chunk.chunkStorageSize) {
      val blockID = chunk.blockStorage(index)
      if (blockID != 0) {
        val mesh = chunk.getMesh(index)
        if (mesh.isTranslucent) {
          val tex = mesh.getTexture

          val render = if (trans_index >= mr1.length) {
            val render = new MeshRenderer(tex)
            mr1.addOne(render)
            render
          } else {
            val render = mr1(trans_index)
            render.reset(tex)
            render
          }

          val blockpos = chunk.indexToBlockPos(index)
          tmpPosList(0) = blockpos
          tmpMeshList(0) = mesh
          tmpFaceList(0) = visibleFaces(index)
          val transSortPos = new Vector3d(blockpos.X + 0.5, blockpos.Y + 0.5, blockpos.Z + 0.5)
          render.setTransSortPosition(transSortPos)
          render.loadMeshes(tmpMeshList, tmpPosList, tmpFaceList, 1, viewCenter)

          trans_index += 1
        }
      }
    }

    // If the list is larger than the number of translucent blocks, just invalidate the rest
    // Could recycle them, though
    while (trans_index < mr1.length) {
      mr1(trans_index).clear()
      trans_index += 1
    }

    val mr2 = trans
    trans_alt = mr2;
    trans = mr1;
  }

  /**
   * Recompute all MeshRenderer for this chunk
   * @param viewCenter
   * @param viewMatrix
   */
  def computeVisualUpdates(viewCenter: BlockPos, viewMatrix: Matrix4f): Unit = {
    if (chunkVisualModified) {

      println(s"Visual modification for ${chunk}")

      println("Frustum check")
      if (!insideFrustum(viewMatrix, viewCenter)) return;

      chunkVisualModified = false;

      println("Faces")
      updateAllBlockFaces()
      println("opaque")
      computeAllOpaqueRenders(viewCenter)
      println("trans")
      iterateTransBlocks(viewCenter)
      println("done")
      renderIsValid = true;
    }
  }

  /**
   * Draw all opaque blocks in this chunk
   * @param shader
   * @param camera
   */
  def draw(shader: Shader, camera: CameraModel): Unit = {
    var didFrustrumCheck = false;

    if (!renderIsValid) return;

    val numTex = render.length
    for (texIndex <- 0 until numTex) {
      if (!renderIsValid) return

      val mr = render(texIndex)
      if (mr.getTexture.isValid) {
        // Notice that we're fetching the viewCenter we had previously stored in the MeshRenderer.
        // This is because the camera could have moved by the time the chunk is drawn, and that motion
        // would cause things to appear in the wrong place unless we render relative to the original calculated
        // positions of the meshes.
        val viewCenter = mr.getViewCenter
        val view = camera.getViewMatrix(viewCenter)

        if (!didFrustrumCheck) {
          if (!insideFrustum(view, viewCenter)) return
          didFrustrumCheck = true
        }

        shader.setMat4("view", view)
        mr.draw(shader)
      }
    }
  }
}

object ChunkView {
  // Lists of data to be send to loadMesh. As long as there is only ever one visual update thread,
  // this will be safe
  private val tmpMeshList = new Array[Mesh](Chunk.chunkStorageSize)
  private val tmpPosList = new Array[BlockPos](Chunk.chunkStorageSize)
  private val tmpFaceList = new Array[Int](Chunk.chunkStorageSize)
}