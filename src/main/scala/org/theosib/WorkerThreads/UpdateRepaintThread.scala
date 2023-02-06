package org.theosib.WorkerThreads

import org.theosib.Adaptors.Window
import org.theosib.WorldElements.World

class UpdateRepaintThread(val world: World) extends Thread {
  var quitFlag = false;
  val tickPeriod = 50.0 / 1000.0

  override def run(): Unit = {
    var lastTime: Double = Window.getCurrentTime
    while (!quitFlag) {
      val now = Window.getCurrentTime
      val target = lastTime + tickPeriod
      val remaining = target - now
      val elapsed = now - lastTime
      val sleepTime: Long = ((remaining * 1000).round).max(1)
      lastTime = now
      Thread.sleep(sleepTime)

      world.doBlockUpdates()
      world.doRepaintEvents()
      world.doGameTickEvents(elapsed)
      // world.doLoadSave() // Move this to its own thread
    }
  }

  def quit(): Unit = {
    quitFlag = true
    join()
  }
}
