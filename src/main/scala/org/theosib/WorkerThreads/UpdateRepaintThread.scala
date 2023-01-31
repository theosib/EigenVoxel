package org.theosib.WorkerThreads

import org.theosib.WorldElements.World

class UpdateRepaintThread(val world: World) extends Thread {
  var quitFlag = false;

  override def run(): Unit = {
    while (!quitFlag) {
      // XXX Actually get the current time and make sure this wakes every 50ms, regardless of how long the
      // updates take
      Thread.sleep(50)

      world.doBlockUpdates()
      // world.doLoadSave() // Move this to its own thread
    }
  }

  def quit(): Unit = {
    quitFlag = true
    join()
  }
}
