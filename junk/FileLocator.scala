package org.theosib.Files

import scala.reflect.io.File


trait FileCategory {
  def subdir: String;

  case class Texture() extends FileCategory {
    override def subdir: String = "textures"
  }

  case class Mesh() extends FileCategory {
    override def subdir: String = "blocks"
  }

  case class Shader() extends FileCategory {
    override def subdir: String = "shaders"
  }

  case class Chunk() extends FileCategory {
    override def subdir: String = "chunks"
  }

  case class Config() extends FileCategory {
    override def subdir: String = "config"
  }
}



object FileLocator {
  var base_dir: String = null

  def setBaseDir(base: String): Unit = {
    base_dir = base
  }

  def computePath(category: FileCategory, filename: String): String = {
    val sb = new StringBuilder()
    sb.append(base_dir).append(File.pathSeparator).append(category.subdir)
    if (filename == null) return sb.toString()
    sb.append(File.pathSeparator).(filename).toString()
  }
}
