package org.theosib.Files

import org.theosib.Utils.IOUtil
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.theosib.Adaptors.Disposable

import java.nio.ByteBuffer
import scala.util.Using

class Image(imagePath: String) extends Disposable {
  var imageBuf: ByteBuffer = null
  var w: Int = 0
  var h: Int = 0
  var comp: Int = 0

  println("Loading image: " + imagePath)

  {
    var imageBuffer: ByteBuffer = null
    try {
      imageBuffer = IOUtil.ioResourceToByteBuffer(imagePath, 8 * 1024)
    } catch {
      case e: Exception =>
        throw new RuntimeException(e)
    }

    Using(MemoryStack.stackPush()) { stack =>
      val w = stack.mallocInt(1)
      val h = stack.mallocInt(1)
      val comp = stack.mallocInt(1)
      STBImage.stbi_set_flip_vertically_on_load(true);
      // Use info to read image metadata without decoding the entire image.
      // We don't need this for this demo, just testing the API.
      if (!STBImage.stbi_info_from_memory(imageBuffer, w, h, comp)) {
        throw new RuntimeException("Failed to read image information: " + STBImage.stbi_failure_reason)
      } else {
        System.out.println("OK with reason: " + STBImage.stbi_failure_reason)
      }
      System.out.println("Image width: " + w.get(0))
      System.out.println("Image height: " + h.get(0))
      System.out.println("Image components: " + comp.get(0))
      System.out.println("Image HDR: " + STBImage.stbi_is_hdr_from_memory(imageBuffer))
      // Decode the image
      imageBuf = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 0)
      if (imageBuf == null) throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason)
      this.w = w.get(0)
      this.h = h.get(0)
      this.comp = comp.get(0)
    }
  }

  override def destroy(): Unit = {
    STBImage.stbi_image_free(imageBuf)
  }

  def premultiplyAlpha(): Unit = {
    if (comp < 4) return
    val stride = w * 4
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
