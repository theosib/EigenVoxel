package org.theosib.Camera

import org.theosib.Adaptors.Window
import org.theosib.Position.BlockPos
import org.joml.{Matrix4f, Vector3d, Vector3dc}

class CameraModel(val window: Window) {
  var pos: Vector3d = new Vector3d
  var yaw: Double = 0.0
  var pitch: Double = 0.0
  var modified: Boolean = true

  var world_up = new Vector3d(0, 1, 0)
  var cam_front = new Vector3d()
  var cam_up = new Vector3d()
  var cam_right = new Vector3d()
  var move_front = new Vector3d()
  var move_up = new Vector3d()
  var move_right = new Vector3d()

  var viewMatrix: Matrix4f = null

  def getWindow: Window = window

  def setPos(pos: Vector3d): CameraModel = {
//    println(s"new pos: ${pos}")
    this.pos = pos;
    modified = true
    this
  }

  def setPos(x: Double, y: Double, z: Double): CameraModel = {
//    println("setPos components")
    setPos(new Vector3d(x, y, z))
  }

  def getPos(): Vector3dc = pos

  def setYaw(y: Double): CameraModel = {
    this.yaw = y
    modified = true
    this
  }

  def setPitch(p: Double): CameraModel = {
    this.pitch = p
    modified = true
    this
  }

  def addYaw(y: Double): CameraModel = {
    this.yaw += y
    modified = true
    this
  }

  def addPitch(p: Double): CameraModel = {
    this.pitch += p
    if (this.pitch < -89) this.pitch = -89
    if (this.pitch > 89) this.pitch = 89
    modified = true
    this
  }

  def rotate(xoffset: Double, yoffset: Double): Unit = {
    addYaw(xoffset)
    addPitch(yoffset)
  }

  def move(x: Double, y: Double, z: Double): CameraModel = {
//    println("move components")
    setPos(pos.x() + x, pos.y() + y, pos.z() + z)
  }

  def move(t: Vector3dc): CameraModel = {
//    println("move vector")
    move(t.x(), t.y(), t.z())
  }

  private def recalculate(): Unit = {
    if (!modified) return

    val yaw_r = math.toRadians(yaw)
    val pitch_r = math.toRadians(pitch)
    cam_front = new Vector3d(math.cos(yaw_r) * math.cos(pitch_r), math.sin(pitch_r), math.sin(yaw_r) * math.cos(pitch_r))
    cam_front.normalize()
    //    println(s"cam_front:${cam_front}")
    move_front = new Vector3d(math.cos(yaw_r), 0, math.sin(yaw_r))
    move_front.normalize()
    //    println(s"move_front:${move_front}")

    cam_front.cross(world_up, cam_right).normalize()
    //    println(s"cam_right:${cam_right}")
    cam_right.cross(cam_front, cam_up).normalize()
    //    println(s"cam_up:${cam_up}")
    move_front.cross(world_up, move_right).normalize()
    //    println(s"move_right:${move_right}")
    move_up = world_up

    modified = false
  }

  def getViewMatrix(viewCenter: BlockPos): Matrix4f = {
    recalculate()
    val adjustedPos = new Vector3d()
    val direction = new Vector3d()
    pos.sub(viewCenter.toVector3d(), adjustedPos)
    adjustedPos.add(cam_front, direction)
    new Matrix4f().lookAt(adjustedPos.x.toFloat, adjustedPos.y.toFloat, adjustedPos.z.toFloat,
      direction.x.toFloat, direction.y.toFloat, direction.z.toFloat,
      cam_up.x.toFloat, cam_up.y.toFloat, cam_up.z.toFloat)
  }

  def moveForward(distance: Double): Unit = {
    val offset = new Vector3d()
    move_front.mul(distance, offset);
//    println(s"moving forward ${distance}, move_front=${move_front}, offset=${offset}")
    move(offset);
  }

  def moveBackward(distance: Double): Unit = {
    val offset = new Vector3d()
    move_front.mul(-distance, offset);
//    println(s"moving backwards ${distance}, move_front=${move_front}, offset=${offset}")
    move(offset);
  }

  def moveLeft(distance: Double): Unit = {
    val offset = new Vector3d()
    move_right.mul(-distance, offset);
//    println(s"moving left ${distance}, move_right=${move_right}, offset=${offset}")
    move(offset);
  }

  def moveRight(distance: Double): Unit = {
    val offset = new Vector3d()
    move_right.mul(distance, offset);
//    println(s"moving right ${distance}, move_right=${move_right}, offset=${offset}")
    move(offset);
  }

  def moveUp(distance: Double): Unit = {
    val offset = new Vector3d()
    move_up.mul(distance, offset);
//    println(s"moving up ${distance}, move_up=${move_up}, offset=${offset}")
    move(offset);
  }

  def moveDown(distance: Double): Unit = {
    val offset = new Vector3d()
    move_up.mul(-distance, offset);
//    println(s"moving down ${distance}, move_up=${move_up}, offset=${offset}")
    move(offset);
  }

}
