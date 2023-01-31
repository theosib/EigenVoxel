package org.theosib.WorldElements

import org.theosib.Blocks.Air
import org.theosib.GraphicsEngine.Mesh
import org.theosib.Position.{BlockPos, ChunkPos}
import org.theosib.WorldElements.Chunk.{chunkBlockIndex, chunkStorageSize, indexToTuple}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * This is the container of block data that gets saved to disk
 * 16x16x16 unit of blocks in the world
 */
class Chunk(val world: World, val chunkPos: ChunkPos) {
  @inline
  def getChunkPos() = chunkPos

  // save time info
  private var lastSaveTime: Double = 0
  private var unloadedTime: Double = 0
  private var modified: Boolean = false

  // Mapping from block index to block ID
  var blockStorage: Array[Short] = new Array[Short](chunkStorageSize)

  // Mapping from block index to optional custom mesh
  private var meshes: Array[Mesh] = new Array[Mesh](chunkStorageSize)

  // Mapping from block type ID number to implementation
  private val id2impl: ArrayBuffer[BlockImpl] = new ArrayBuffer[BlockImpl](chunkStorageSize)

  // Mapping from block type name to block type ID number
  private val name2id: mutable.Map[String,Short] = new mutable.HashMap[String,Short]()

  lazy val view: ChunkView = new ChunkView(this)
  def getChunkView: ChunkView = view

  /**
   * Return the mesh for this block. If the block implementation has not set a custom mesh, the
   * default will be returned.
   * @param index block index in chunk
   * @return
   */
  def getMesh(index: Int): Mesh = {
    val mesh = meshes(index)
    if (mesh != null) return mesh
    getDefaultMesh(blockStorage(index))
  }

  /**
   * Get default mesh from implementation of block
   * @param id block type ID
   * @return
   */
  private def getDefaultMesh(id: Int): Mesh = {
    val impl = id2impl(id)
    println(s"Default mesh for id=${id} is ${impl}, texDefault=${impl.getDefaultMesh().getTexture}, texMesh=${impl.getMesh(this).getTexture}")
    if (impl == null) {
      Air.getDefaultMesh()
    } else {
      impl.getMesh(this)
    }
  }

  /**
   * Set a custom mesh for this block
   * @param index
   * @param mesh
   */
  def setMesh(index: Int, mesh: Mesh): Unit = {
    meshes(index) = mesh
    requestVisualUpdate(index)
  }

  /**
   * Compute BlockPos from index
   * @param index
   * @return
   */
  def indexToBlockPos(index: Int): BlockPos = {
    var x = index & 15;
    var z = (index >> 4) & 15;
    var y = (index >> 8) & 15;
    x += chunkPos.X << 4
    y += chunkPos.Y << 4
    z += chunkPos.Z << 4
    new BlockPos(x, y, z)
  }

  /**
   * Each chunk maintains its own mapping from block type to ID so that the numerical representation
   * remains compact. This looks up the ID for a block type, creating a new one if necessary.
   * @param name Name of block type
   * @return
   */
  def getBlockID(name: String): Int = {
    val option = name2id.get(name)
    option match {
      case Some(id) => id
      case None =>
        val impl: BlockImpl = BlockLibrary.lookupBlockType(name)
        val id = id2impl.length
        id2impl.append(impl)
        name2id.put(name, id.toShort)
        id
    }
  }

  /**
   * Each chunk maintains its own mapping from block type to ID so that the numerical representation
   * remains compact. This looks up the ID for a block type, creating a new one if necessary.
   * @param impl Implementation of block type
   * @return
   */
  def getBlockID(impl: BlockImpl): Int = {
    println(s"${impl} ${name2id}")
    val option = name2id.get(impl.getName)
    option match {
      case Some(id) => id
      case None =>
        val id = id2impl.length
        id2impl.append(impl)
        name2id.put(impl.getName, id.toShort)
        id
    }
  }

  /**
   * Return the block type implementation or null for air
   * @param index block in chunk
   * @return
   */
  @inline
  def getBlockImpl(index: Int): BlockImpl = {
    id2impl(blockStorage(index))
  }

  /**
   * Fetch a Block by index
   * @param index
   * @return
   */
  def getBlock(index: Int): Block = {
    val pos = indexToBlockPos(index)
    val impl = getBlockImpl(index)
    new Block(this, pos, index, impl)
  }

  /**
   * Fetch a block by index. Air blocks are optimized out.
   * @param index
   * @return
   */
  def getBlockNullable(index: Int): Block = {
    val impl = getBlockImpl(index)
    if (impl == Air) {
      null
    } else {
      val pos = indexToBlockPos(index)
      new Block(this, pos, index, impl)
    }
  }

  /**
   * Fetch a block by BlockPos
   * @param pos
   * @return
   */
  def getBlock(pos: BlockPos): Block = {
    val index = chunkBlockIndex(pos)
    val impl = getBlockImpl(index)
    new Block(this, pos, index, impl)
  }

  /**
   * Fetch a block by BlockPos. Air blocks are optimized out.
   * @param pos
   * @return
   */
  def getBlockNullable(pos: BlockPos): Block = {
    val index = chunkBlockIndex(pos)
    val impl = getBlockImpl(index)
    if (impl == Air) {
      null
    } else {
      new Block(this, pos, index, impl)
    }
  }

  /**
   * Place a block in the chunk.
   * @param pos position
   * @param name block type
   */
  def setBlock(pos: BlockPos, name: String): Unit = {
    val oldBlock = getBlock(pos)
    oldBlock.breakEvent()
    // XXX data containers

    val index = oldBlock.getStorageIndex()
    val blockID = getBlockID(name)

    blockStorage(index) = blockID.toShort
    meshes(index) = null;

    if (blockID != 0) {
      val impl = getBlockImpl(index)
      val block = new Block(this, pos, index, impl)
      block.placeEvent()
      block.repaintEvent()
    }

    requestVisualUpdate(index)
    modified = true
  }

  /**
   * Send update event to block. Optimizes out air blocks.
   * @param pos
   */
  def updateBlock(pos: BlockPos): Unit = {
    val block = getBlockNullable(pos)
    if (block != null) block.updateEvent()
  }

  /**
   * Queue all blocks for update
   * @param noLoad
   */
  def updateAllBlocks(noLoad: Boolean): Unit = {
    val nonAirBlocks = new ArrayBuffer[BlockPos]()
    for (index <- 0 until chunkStorageSize) {
      val id = blockStorage(index)
      if (id != 0) nonAirBlocks.addOne(indexToBlockPos(index))
    }
    world.updateBlocks(nonAirBlocks, noLoad)
  }

  /**
   * Send repaint event to block. Optimizes out air blocks.
   * This is the actual repaint action.
   * @param pos
   */
  def repaintBlock(pos: BlockPos): Unit = {
    val block = getBlockNullable(pos)
    if (block != null) {
      block.repaintEvent()
      println(s"Marking block ${block} as visually updated")
      requestVisualUpdate(block.getStorageIndex())
    }
  }

  /**
   * Queue all blocks for repaint
   */
  def repaintAllBlocks(): Unit = {
    val nonAirBlocks = new ArrayBuffer[BlockPos]()
    for (index <- 0 until chunkStorageSize) {
      val id = blockStorage(index)
      if (id != 0) nonAirBlocks.addOne(indexToBlockPos(index))
    }
    println(s"repainting ${nonAirBlocks}")
    world.repaintBlocks(nonAirBlocks)
  }

  /**
   * For load and worldgen, set a block. Repaint and update will occur later in bulk.
   * @param pos
   * @param name
   */
  def genBlock(pos: BlockPos, name: String): Unit = {
    val index = chunkBlockIndex(pos)
    val blockID = getBlockID(name);
    println(s"ID(${name})=${blockID}")
    blockStorage(index) = blockID.toShort
    modified = true
  }

  /**
   * Convenience method to replace block with air
   * @param pos
   */
  def breakBlock(pos: BlockPos): Unit = {
    setBlock(pos, "Air")
  }

  def requestVisualUpdate(pos: BlockPos): Unit = {
    view.markBlockVisuallyUpdated(chunkBlockIndex(pos))
  }

  def requestVisualUpdate(index: Int): Unit = {
    view.markBlockVisuallyUpdated(index)
  }

  /**
   * Try to load block from disk
   * @return
   */
  def load(): Boolean = false

  def generate(): Unit = {
    for (x <- 0 until 4; z <- 0 until 4) {
      genBlock(new BlockPos(x, 0, z), "cobble")
    }
//    genBlock(new BlockPos(0, 0, 0), "wood")
    repaintAllBlocks()
  }

  /**
   * Get the corners of chunk, relative to the camera view center. This is used for frustum culling.
   * @param center
   * @return
   */
  def getCorners(center: BlockPos): Array[BlockPos] = {
    val x = (chunkPos.X << 4) - center.X
    val y = (chunkPos.Y << 4) - center.Y
    val z = (chunkPos.Z << 4) - center.Z
    val arr = new Array[BlockPos](8)
    arr(0) = new BlockPos(x, y, z)
    arr(1) = new BlockPos(x + 16, y, z)
    arr(2) = new BlockPos(x, y + 16, z)
    arr(3) = new BlockPos(x + 16, y + 16, z)
    arr(4) = new BlockPos(x, y, z + 16)
    arr(5) = new BlockPos(x + 16, y, z + 16)
    arr(6) = new BlockPos(x, y + 16, z + 16)
    arr(7) = new BlockPos(x + 16, y + 16, z + 16)
    arr
  }

  override def toString: String = {
    s"Chunk(${chunkPos})"
  }

  /* Initialization */
  // Get Air registered as block ID zero
  getBlockID(Air)
}

object Chunk {
  val chunkStorageSize: Int = 16 * 16 * 16;

  /**
   * Decompose chunk block index into in-chunk coordinates
   * @param index
   * @return
   */
  def indexToTuple(index: Int): (Int, Int, Int) = {
    val x = index & 15;
    val z = (index >> 4) & 15;
    val y = (index >> 8) & 15;
    (x, y, z)
  }

  /**
   * Convert a BlockPos to in-chunk storage index
   * @param pos
   * @return
   */
  @inline
  def chunkBlockIndex(pos: BlockPos): Int = pos.chunkIndex()
}