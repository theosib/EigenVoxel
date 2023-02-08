package org.theosib.Camera

import org.theosib.Adaptors.InputReceiver
import org.theosib.Events.{InputEvent, InputState}
import org.lwjgl.glfw.GLFW

class CameraController(val model: CameraModel) extends InputReceiver {
  val sensitivity = 0.1f;

  var grabbing: Boolean = false;

  override def event(ev: InputEvent): Boolean = {
    if (ev.isInstanceOf[InputEvent.MotionEvent]) {
      val motion = ev.asInstanceOf[InputEvent.MotionEvent]
      rotate(motion.xpos, motion.ypos)
    } else if (ev.isInstanceOf[InputEvent.KeyEvent]) {
      val key = ev.asInstanceOf[InputEvent.KeyEvent]
      if (key.action == GLFW.GLFW_PRESS) {
        if (key.key == GLFW.GLFW_KEY_ESCAPE) {
          println("Escape")
          grabbing = !grabbing;
          ev.window.grabCursor(grabbing)
        } else if (key.key == GLFW.GLFW_KEY_Q) {
          ev.window.setQuit(true)
        }
      }
    }
    return false;
  }

  override def process(state: InputState, timeDelta: Double): Unit = {
    val w = state.isKeyPressed(GLFW.GLFW_KEY_W)
    val s = state.isKeyPressed(GLFW.GLFW_KEY_S)
    val a = state.isKeyPressed(GLFW.GLFW_KEY_A)
    val d = state.isKeyPressed(GLFW.GLFW_KEY_D)
    val space = state.isKeyPressed(GLFW.GLFW_KEY_SPACE)
    val shift = state.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)

    val delta = if (timeDelta < 0.008) 0.008 else timeDelta

    if (w) moveForward(delta)
    if (s) moveBackward(delta)
    if (a) moveLeft(delta)
    if (d) moveRight(delta)
    if (shift) moveDown(delta)
    if (space) moveUp(delta)
  }

  def moveForward(timeDelta: Double): Unit = {
    model.moveForward(timeDelta);
  }

  def moveBackward(timeDelta: Double): Unit = {
    model.moveBackward(timeDelta);
  }

  def moveLeft(timeDelta: Double): Unit = {
    model.moveLeft(timeDelta);
  }

  def moveRight(timeDelta: Double): Unit = {
    model.moveRight(timeDelta);
  }

  def moveUp(timeDelta: Double): Unit = {
    model.moveUp(timeDelta);
  }

  def moveDown(timeDelta: Double): Unit = {
    model.moveDown(timeDelta);
  }

  var last_mouse_x: Double = 0
  var last_mouse_y: Double = 0
  var valid_mouse_pos: Boolean = false

  def rotate(xpos: Double, ypos: Double): Unit = {
//    println(s"xpos=${xpos} ypos=${ypos}")

    val grab = model.getWindow.cursorIsGrabbed()
    if (!grab) {
      valid_mouse_pos = false
      return
    }

    if (!valid_mouse_pos) {
      last_mouse_x = xpos;
      last_mouse_y = ypos
      valid_mouse_pos = true
      return
    }

    val xoffset = xpos - last_mouse_x
    val yoffset = last_mouse_y - ypos
    last_mouse_x = xpos
    last_mouse_y = ypos

    model.rotate(xoffset * sensitivity, yoffset * sensitivity)
  }

  override def priority(): Int = 0
}
