package org.theosib.Images
import org.lwjgl.system.MemoryUtil

import java.nio.ByteBuffer

class MemImageBuffer(val w: Int, val h: Int, val comp: Int = 4) extends ImageBuffer {
  val imageBuffer: ByteBuffer = MemoryUtil.memAlloc(w * comp * h)
  override def getWidth(): Int = w

  override def getHeight(): Int = h

  override def getByteBuffer(): ByteBuffer = imageBuffer

  override def numComponents(): Int = comp

  override def getStride(): Int = w * comp

  override def destroy(): Unit = MemoryUtil.memFree(imageBuffer)

  def clear(): Unit = MemoryUtil.memSet(imageBuffer, 0)
}
