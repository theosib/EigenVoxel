package org.theosib.Camera

import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Position.BlockPos
import org.joml.{Matrix4f, Vector3d, Vector3dc}
import org.theosib.Utils.WindowDimensions
import org.theosib.WorldElements.{Entity, World, WorldView}

class CameraModel(val window: Window, val world: World, val worldView: WorldView) extends RenderAgent {
//  var pos: Vector3d = new Vector3d
//  var yaw: Double = 0.0
//  var pitch: Double = 0.0
  var recompute: Boolean = true
  val entity = new Entity(world, worldView)
  entity.setPosition(new Vector3d(10, 10, 10))
  entity.setSize(0.6, 1.8)

  var world_up = new Vector3d(0, 1, 0)
  var cam_front = new Vector3d()
  var cam_up = new Vector3d()
  var cam_right = new Vector3d()
  var move_front = new Vector3d()
  var move_up = new Vector3d()
  var move_right = new Vector3d()

  var movement_speed: Double = 5

  var viewMatrix: Matrix4f = null

  def getWindow: Window = window

  def getEntity: Entity = entity

  def setPos(pos: Vector3d): CameraModel = {
    entity.setCameraPos(pos)
    recompute = true
    this
  }

  def setPos(x: Double, y: Double, z: Double): CameraModel = {
    setPos(new Vector3d(x, y, z))
  }

  def getPos(): Vector3dc = entity.getCameraPos()

  def setYaw(y: Double): CameraModel = {
    entity.setYaw(y)
    recompute = true
    this
  }

  def setPitch(p: Double): CameraModel = {
    entity.setPitch(p)
    recompute = true
    this
  }

  def addYaw(y: Double): CameraModel = {
    entity.setYaw(entity.getYaw() + y)
    recompute = true
    this
  }

  def addPitch(p: Double): CameraModel = {
    var pitch = entity.getPitch()
    pitch += p
    if (pitch < -89) pitch = -89
    if (pitch > 89) pitch = 89
    entity.setPitch(pitch)
    recompute = true
    this
  }

  def rotate(xoffset: Double, yoffset: Double): CameraModel = {
    addYaw(xoffset)
    addPitch(yoffset)
  }

  def move(x: Double, y: Double, z: Double): CameraModel = {
    move(new Vector3d(x, y, z))
    this
  }

  def move(t: Vector3dc): CameraModel = {
//    println("move vector")
    entity.move(t)
    this
  }

  private def recalculate(): Unit = {
    if (!recompute) return

    val yaw_r = math.toRadians(entity.getYaw())
    val pitch_r = math.toRadians(entity.getPitch())
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

    recompute = false
  }

  def getViewMatrix(viewCenter: BlockPos): Matrix4f = {
    recalculate()
    val adjustedPos = new Vector3d()
    val direction = new Vector3d()
    entity.getCameraPos().sub(viewCenter.toVector3d(), adjustedPos)
    adjustedPos.add(cam_front, direction)
    new Matrix4f().lookAt(adjustedPos.x.toFloat, adjustedPos.y.toFloat, adjustedPos.z.toFloat,
      direction.x.toFloat, direction.y.toFloat, direction.z.toFloat,
      cam_up.x.toFloat, cam_up.y.toFloat, cam_up.z.toFloat)
  }

  def moveHorizontally(vec: Vector3dc): Unit = {
    val vel = entity.getVelocity()
    var new_x: Double = 0
    var new_z: Double = 0
    if (vel.y() != 0) {
      val hspeed = (movement_speed - vel.y().abs).max(0)
      new_z = vec.z * hspeed
      new_x = vec.x * hspeed
    } else {
      new_z = vec.z * movement_speed
      new_x = vec.x * movement_speed
    }

    var old_z = vel.z()
    var old_x = vel.x()
    if (new_z.abs > old_z.abs || (new_z<0 && old_z>0) || (new_z>0 && old_z<0)) old_z = new_z
    if (new_x.abs > old_x.abs || (new_x<0 && old_x>0) || (new_x>0 && old_x<0)) old_x = new_x
    entity.setHorizontalVelocity(old_x, old_z)
  }

  val tmpOffset = new Vector3d()

  def moveForward(timeDelta: Double): Unit = {
    if (entity.hasGravity()) {
      moveHorizontally(move_front)
    } else {
      move_front.mul(timeDelta * movement_speed, tmpOffset)
      move(tmpOffset)
    }
  }

  def moveBackward(timeDelta: Double): Unit = {
    if (entity.hasGravity()) {
      move_front.negate(tmpOffset)
      moveHorizontally(tmpOffset)
    } else {
      move_front.mul(-timeDelta * movement_speed, tmpOffset)
      move(tmpOffset)
    }
  }

  def moveLeft(timeDelta: Double): Unit = {
    if (entity.hasGravity()) {
      move_right.negate(tmpOffset)
      moveHorizontally(tmpOffset)
    } else {
      move_right.mul(-timeDelta * movement_speed, tmpOffset)
      move(tmpOffset)
    }
  }

  def moveRight(timeDelta: Double): Unit = {
    if (entity.hasGravity()) {
      moveHorizontally(move_right)
    } else {
      move_right.mul(timeDelta * movement_speed, tmpOffset)
      move(tmpOffset)
    }
  }

  def moveUp(timeDelta: Double): Unit = {
    if (entity.hasGravity()) {
      if (entity.isOnGround()) {
        entity.setVerticalVelocity(movement_speed * 1.5)
      }
    } else {
      move_up.mul(timeDelta * movement_speed, tmpOffset)
      move(tmpOffset)
    }
  }

  def moveDown(timeDelta: Double): Unit = {
    move_up.mul(-timeDelta * movement_speed, tmpOffset);
    move(tmpOffset);
  }

  override def create(w: Window): Unit = {}

  override def destroy(): Unit = {}

  override def willRender(w: Window, elapsedTime: Double): Boolean = {
//    println(s"Camera model game tick ${elapsedTime}")
    entity.gameTick(elapsedTime)
    false
  }

  override def render(w: Window, elapsedTime: Double): Unit = {}

  override def resize(w: Window, dim: WindowDimensions): Unit = {}

  override def priority(): Int = -1000
}
