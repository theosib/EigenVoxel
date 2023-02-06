package org.theosib.WorldElements

import org.theosib.Geometry.{AxisAlignedBox, CollisionShape, GeometryFunctions}
import org.theosib.Position.{BlockPos, ChunkPos}
import org.theosib.Utils.Facing
import org.joml.{Vector3d, Vector3dc}
import org.w3c.dom.NodeList

import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{CollectionHasAsScala, EnumerationHasAsScala, IterableHasAsJava}

class World {
  // Thread-safe storage of chunks
  val chunkStorage = new ConcurrentHashMap[Long,Chunk]

  // Queue of blocks to update
  var blockUpdateQueueLoad: java.util.Set[BlockPos] = ConcurrentHashMap.newKeySet()
  var blockUpdateQueueNoLoad: java.util.Set[BlockPos] = ConcurrentHashMap.newKeySet()

  // Queue of blocks needing visual update
  var repaintQueue: java.util.Set[BlockPos] = ConcurrentHashMap.newKeySet()

  val entityStore: java.util.Set[Entity] = ConcurrentHashMap.newKeySet()

  /**
   * Queue a block update event for this block
   * @param pos
   * @param noLoad
   */
  def updateBlock(pos: BlockPos, noLoad: Boolean = false): Unit = {
    if (noLoad) {
      blockUpdateQueueNoLoad.add(pos)
    } else {
      blockUpdateQueueLoad.add(pos)
    }
  }

  /**
   * Queue update for list of blocks
   * @param posArr
   * @param noLoad
   */
  def updateBlocks(posArr: Iterable[BlockPos], noLoad: Boolean = false): Unit = {
    if (noLoad) {
      blockUpdateQueueNoLoad.addAll(posArr.asJavaCollection)
    } else {
      blockUpdateQueueLoad.addAll(posArr.asJavaCollection)
    }
  }

  /**
   * Queue a repaint event for this block
   * @param pos
   */
  def repaintBlock(pos: BlockPos): Unit = {
    repaintQueue.add(pos)
  }

  /**
   * Queue a repaint event for list of blocks
   * @param pos
   */
  def repaintBlocks(posArr: Iterable[BlockPos]): Unit = {
    repaintQueue.addAll(posArr.asJavaCollection)
  }

  /**
   * Look up a chunk
   * XXX: Implement caching of most recent chunks
   * @param pos
   * @param noLoad
   * @return
   */
  def getChunk(pos: Any, noLoad: Boolean = false): Option[Chunk] = {
    val packed = pos match {
      case p: ChunkPos => p.packed()
      case p: BlockPos => p.packedAsChunkPos()
    }

    // XXX Implement caching
    val chunk = chunkStorage.get(packed)
    if (chunk != null || noLoad) return Option(chunk)

    val chunkPos = pos match {
      case p: ChunkPos => p
      case p: BlockPos => p.getChunkPos()
    }

    Option(loadChunk(chunkPos))
  }

  /**
   * Get list of chunks corresponding to specified block or chunk positions
   * @param posArr
   * @param noLoad
   * @return
   */
  def getChunks(posArr: Iterable[_], noLoad: Boolean = false): Iterable[Option[Chunk]] = {
    posArr.map(pos => getChunk(pos, noLoad))
  }

  /**
   * Get a block from somewhere in the world
   * @param pos
   * @param noLoad
   * @return
   */
  def getBlock(pos: BlockPos, noLoad: Boolean = false): Option[Block] = {
    getChunk(pos, noLoad).map(_.getBlock(pos))
  }

  /**
   * Get a list of blocks in the world from a list of block positions
   * @param posArr
   * @param noLoad
   * @return
   */
  def getBlocks(posArr: Iterable[BlockPos], noLoad: Boolean = false): Iterable[Option[Block]] = {
    posArr.map(pos => getBlock(pos, noLoad))
  }

  /**
   * Get list of the six cardinal neighbors of the specified block
   * @param pos center block
   * @param includeSelf include self in the list
   * @param noLoad do loading of any unloaded chunks
   * @return
   */
  def getNeighborBlocks(pos: BlockPos, includeSelf: Boolean = false, noLoad: Boolean = false): Iterable[Option[Block]] = {
    val neighborPos = pos.allNeighbors(includeSelf)
    getBlocks(neighborPos, noLoad)
  }

  /**
   * Get the 26 blocks surrounsing the specified position, 27 if self is included
   * @param pos
   * @param includeSelf
   * @param noLoad
   * @return
   */
  def getSurroundingBlocks(pos: BlockPos, includeSelf: Boolean = false, noLoad: Boolean = false): Iterable[Option[Block]] = {
    val surroundingPos = pos.allSurrounding(includeSelf)
    getBlocks(surroundingPos, noLoad)
  }

  /**
   * Queue update events to the 26 blocks surrounding the specified block
   * @param pos
   * @param noLoad
   */
  def updateSurroundingBlocks(pos: BlockPos, noLoad: Boolean = false): Unit = {
    val surroundingPos = pos.allSurrounding()
    updateBlocks(surroundingPos, noLoad)
  }

  /**
   * Queue visual updates for 26 surrounding blocks
   * @param pos
   */
  def repaintSurroundingBlocks(pos: BlockPos): Unit = {
    val surroundingPos = pos.allSurrounding()
    repaintBlocks(surroundingPos)
  }

  /**
   * Place a block into the world
   * @param pos
   * @param name
   */
  def setBlock(pos: BlockPos, name: String): Unit = {
    val chunk_in = getChunk(pos)
    val chunk = chunk_in match {
      case Some(chunk) => chunk
      case None =>
        // generate
        return
    }
    chunk.setBlock(pos, name)
    updateSurroundingBlocks(pos)
    repaintSurroundingBlocks(pos)
  }

  /**
   * Convenience function to break a block
   * @param pos
   */
  def breakBlock(pos: BlockPos): Unit = {
    setBlock(pos, "Air")
  }

  /**
   * Set blocks in bulk. There may be some optimizations that can be made.
   * @param blocks
   */
  def setBlocks(blocks: Iterable[(BlockPos, String)]): Unit = {
    for ((pos, name) <- blocks) {
      setBlock(pos, name)
      updateSurroundingBlocks(pos)
    }
  }

  def setBlocks(poss: Iterable[BlockPos], names: Iterable[String]): Unit = {
    var bi = poss.iterator
    var ni = names.iterator
    while (bi.hasNext && ni.hasNext) {
      var pos = bi.next()
      var name = ni.next()
      setBlock(pos, name)
      updateSurroundingBlocks(pos)
    }
  }

  def swapBlockUpdateQueueNoLoad(): java.util.Set[BlockPos] = {
    val old = blockUpdateQueueNoLoad
    blockUpdateQueueNoLoad = ConcurrentHashMap.newKeySet()
    old
  }

  def swapBlockUpdateQueueLoad(): java.util.Set[BlockPos] = {
    val old = blockUpdateQueueLoad
    blockUpdateQueueLoad = ConcurrentHashMap.newKeySet()
    old
  }

  def swapRepaintQueue(): java.util.Set[BlockPos] = {
    val old = repaintQueue
    repaintQueue = ConcurrentHashMap.newKeySet()
    old
  }

  /**
   * Do all block updates and visual updates
   */
  def doBlockUpdates(): Unit = {
    val updateNoLoad = swapBlockUpdateQueueNoLoad()
    val updateLoad = swapBlockUpdateQueueLoad()

    updateLoad.forEach { pos => getChunk(pos, false).foreach { _.updateBlock(pos) } }
    updateNoLoad.forEach { pos => getChunk(pos, true).foreach { _.updateBlock(pos) } }
  }

  def doRepaintEvents(): Unit = {
    val repaint = swapRepaintQueue()
    repaint.forEach { pos => getChunk(pos, true).foreach { _.repaintBlock(pos) } }
  }

  def doGameTickEvents(elapsedTime: Double): Unit = {
    val chunks = listAllChunks()
    chunks.foreach { chunk =>
      chunk.tickAllBlocks()
    }

    entityStore.forEach { entity =>
      entity.gameTick(elapsedTime)
    }
  }

  /**
   * Locate the first block within [limit] distance from [start] in the direction of [forward]
   * @param start
   * @param forward
   * @param limit
   * @return (the desired block, distance from starting position, the face being poitned at)
   */
  def findNearestBlock(start: Vector3dc, forward: Vector3dc, limit: Double): (Block, Double, Int) = {
    var dist: Double = 0
    var enterFace: Int = -1
    val here = new Vector3d(start)
    var target: BlockPos = null

    for (i <- 0 until 100) {
      val (exitFace, r) = GeometryFunctions.exitFace(here, forward);
      if (exitFace<0 || r+dist>limit) {
        return (null, -1, -1)
      }

      dist += r
      enterFace = Facing.oppositeFace(exitFace)
      start.fma(dist, forward, here)
      target = GeometryFunctions.whichBlock(here, forward)

      val block = getBlock(target, true)
      block match {
        case Some(block) => if (!block.isAir()) return (block, dist, enterFace)
        case None =>
      }
    }
    (null, -1, -1)
  }

  /**
   * Returns collection of all collision boxes in the world that intersect with the given box
   * @param focus
   * @return
   */
  def allIntersectingCollisions(focus: AxisAlignedBox): CollisionShape = {
    val focusBlockPos = focus.intersectingBlocks();
    val blocks = getBlocks(focusBlockPos.getArray)
    val result = new CollisionShape
    blocks.foreach {
      case Some(block) => if (!block.isAir()) {
        val collision = block.getCollision()
        collision.getArray.foreach { cb =>
          if (focus.intersects(cb)) {
            result.append(cb)
          }
        }
      }
      case None =>
    }
    result.sort()
  }

  /**
   * Handle right click on block
   * @param pos
   * @param face
   */
  def useAction(pos: BlockPos, face: Int): Unit = {
    val block = getBlock(pos)
    block.foreach { block =>
      val consumed = block.useAction(face)
      if (consumed) return
    }

    // XXX handle placing block
    /*
if (block_to_place.size() > 0) {
        setBlock(pos.neighbor(face), block_to_place, place_rotation);
    }
     */
  }

  /**
   * Handle left click on block
   * @param pos
   * @param face
   */
  def hitAction(pos: BlockPos, face: Int): Unit = {
    val block = getBlock(pos)
    block.foreach { block =>
      val consumed = block.hitAction(face)
      if (consumed) return
    }
    breakBlock(pos)
  }

  def listAllChunks(): Iterable[Chunk] = chunkStorage.values().asScala
  def listAllChunkPos(): Iterable[ChunkPos] = listAllChunks().map(_.getChunkPos())


  /**
   * Load or generate a block that is missing from the world
   * @param pos
   * @return
   */
  def loadChunk(pos: ChunkPos): Chunk = {
    if (pos.Y < 0) return null
    val packed = pos.packed()

    chunkStorage.synchronized {
      // If we're the second to enter this critical section, see if the first created the same chunk
      val already = chunkStorage.get(packed)
      if (already != null) return already

      // XXX Search unload queue

      // XXX Search queue of chunks being generated relative to user position

      // Make a new chunk
      val chunk = new Chunk(this, pos)

      // Load or generate the chunk
      val ok = chunk.load()
      if (!ok) chunk.generate() // XXX Queue to chunk gen thread

      // XXX Modify any caching

      // Wait until the chunk is fully populated before storing it so that another reader doesn't
      // fetch a half-baked chunk
      chunkStorage.put(packed, chunk)
      chunk
    }
  }
}
