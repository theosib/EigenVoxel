package org.theosib.WorkerThreads

import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.Utils.WindowDimensions
import org.theosib.WorldElements.WorldView

class UpdateRenderThread(val camera: CameraModel, val view: WorldView) extends Thread with RenderAgent {
  var quitFlag = false;
  var doCompute = false;

  private def doWait(): Unit = {
    this.synchronized {
      while (!doCompute) this.wait()
      doCompute = false
    }
  }

  def doNotify(): Unit = {
    this.synchronized {
      doCompute = true
      this.notify()
    }
  }

  override def run(): Unit = {
    while (!quitFlag) {
      doWait()
//      Thread.sleep(10)
      if (quitFlag) return
      view.computeChunkAndEntityRenders(camera)
    }
  }

  def quit(): Unit = {
    quitFlag = true
    doNotify()
    join()
  }

  // Abuse the RenderAgent interface to get this thread woken up every video frame
  override def create(w: Window): Unit = {}
  override def destroy(): Unit = {}
  override def willRender(w: Window): Boolean = {
    doNotify()
    false
  }
  override def render(w: Window): Unit = {}
  override def resize(w: Window, dim: WindowDimensions): Unit = {}
  override def priority(): Int = 1000000
}
