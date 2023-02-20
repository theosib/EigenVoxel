package org.theosib.Images

import org.theosib.Adaptors.Disposable

import java.nio.ByteBuffer

trait ImageBuffer extends Disposable {
  def getWidth(): Int
  def getHeight(): Int
  def getByteBuffer(): ByteBuffer
  def numComponents(): Int
  def getStride(): Int

  def premultiplyAlpha(): Unit = {
    val comp = numComponents()
    if (comp < 4) return
    val w = getWidth()
    val h = getHeight()
    val imageBuf = getByteBuffer()
    val stride = getStride();
    for (y <- 0 until h) {
      for (x <- 0 until w) {
        val i = y * stride + x * 4
        val alpha = (imageBuf.get(i + 3) & 0xFF) / 255.0f
        imageBuf.put(i + 0, math.round((imageBuf.get(i + 0) & 0xFF) * alpha).toByte)
        imageBuf.put(i + 1, math.round((imageBuf.get(i + 1) & 0xFF) * alpha).toByte)
        imageBuf.put(i + 2, math.round((imageBuf.get(i + 2) & 0xFF) * alpha).toByte)
      }
    }
  }
}
