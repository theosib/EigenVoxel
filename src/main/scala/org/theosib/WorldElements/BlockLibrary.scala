package org.theosib.WorldElements

import org.theosib.Blocks.{Air, StaticCubeBlock}

import scala.collection.mutable

object BlockLibrary {
  val library: mutable.Map[String,BlockImpl] = new mutable.HashMap[String,BlockImpl]()

  // Make dead sure that Air gets registered regardless of class loading order, so register it here
  registerBlockType("Air", Air)

  def lookupBlockType(name: String): BlockImpl = {
    val option: Option[BlockImpl] = library.get(name)
    option match {
      case Some(impl) => impl
      case None =>
        val impl = loadBlockType(name)
        library.put(name, impl)
        impl
    }
  }

  private def loadBlockType(name: String): BlockImpl = {
    new StaticCubeBlock(name)
  }

  def registerBlockType(name: String, impl: BlockImpl): Unit = {
    library.put(name, impl)
  }

  def getBlockTypes(): Iterable[BlockImpl] = {
    library.values
  }

  def getBlockNames(): Iterable[String] = {
    library.keys
  }
}
