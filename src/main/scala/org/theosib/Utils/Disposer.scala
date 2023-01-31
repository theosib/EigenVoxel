package org.theosib.Utils

import org.theosib.Adaptors.{Disposable, RenderAgent, Window}

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.IterableHasAsJava

object Disposer extends RenderAgent {
  val deadQueue = new ConcurrentLinkedQueue[Disposable]
  val deadListQueue = new ConcurrentLinkedQueue[Iterable[Disposable]]

  // Schedule disposal from any thread
  def dispose(mr: Disposable): Unit = {
    if (mr == null) return
    deadQueue.add(mr)
  }

  def dispose(mrs: Iterable[Disposable]): Unit = {
    if (mrs == null) return
    deadListQueue.add(mrs)
  }

  // Dispose from main rendering thread
  def drainRenderDisposal(): Unit = {
//    println(s"disposing ${deadQueue.size()} + ${deadListQueue.size()}")
    // XXX Limit how many can be disposed at once to avoid framerate hits
    while (!deadQueue.isEmpty) {
      val mr = deadQueue.remove()
      mr.destroy()
    }
    while (!deadListQueue.isEmpty) {
      val mrs = deadListQueue.remove()
      mrs.foreach(_.destroy())
    }
  }


  // Disposal has to be on the main thread. Might as well abuse the render loop.
  // XXX Maybe provide another interface that doesn't require this abuse
  override def create(w: Window): Unit = {}

  override def destroy(): Unit = {}

  override def willRender(w: Window): Boolean = {
    drainRenderDisposal()
    false
  }

  override def render(w: Window): Unit = {}

  override def resize(w: Window, dim: WindowDimensions): Unit = {}

  override def priority(): Int = 100
}
