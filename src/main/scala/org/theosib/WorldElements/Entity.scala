package org.theosib.WorldElements

import org.joml.{Matrix4f, Matrix4fc, Vector3d, Vector3dc}
import org.theosib.Adaptors.{Disposable, RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.Geometry.{CollisionShape, EntityBox}
import org.theosib.GraphicsEngine.{Mesh, MeshRenderer, Shader, Texture}
import org.theosib.Position.BlockPos
import org.theosib.Utils.{Disposer, WindowDimensions}
import org.theosib.WorldElements.Entity.{max_y_vel, tmpFaceList, tmpMeshList, tmpPosList}

import scala.util.control.Breaks.{break, breakable}

class Entity(val world: World, val worldView: WorldView) extends Disposable {
  var ebox: EntityBox = new EntityBox()
  var velocity = new Vector3d()
  var accel = new Vector3d()
  var gravity: Boolean = false
  var onGround: Boolean = false

  var current_pos = new Vector3d()
  var target_pos = new Vector3d()
  var start_pos = new Vector3d()
  var current_time: Double = 0
  var target_time: Double = 0
  var start_time: Double = 0

  var yaw: Double = 0

  var visible: Boolean = true
  var mesh: Mesh = null

  @volatile var render: MeshRenderer = null
  @volatile var render_alt: MeshRenderer = null

  var visualModified: Boolean = false

  var projection: Matrix4fc = null

  def setProjectionMatrix(proj: Matrix4fc): Unit = {
    projection = proj
  }

  def move(motion: Vector3dc): Unit = {
    if (motion.x()==0 && motion.y()==0 && motion.z()==0) return

    var boxHere = ebox.getAxisAlignedBox
    val collisionHere = world.allIntersectingCollisions(boxHere)

    if (motion.y() != 0) {
      val boxThere = boxHere.offset(0, motion.y(), 0)
      val collisionThere = world.allIntersectingCollisions(boxThere)
      val target = collisionThere.difference(collisionHere)

      val d = target.minDistance(boxHere)
      if (d >= Math.abs(motion.y())) {
        ebox.position.y += motion.y()
        onGround = false
      } else {
        if (motion.y() < 0) {
          ebox.position.y -= d
          onGround = true
        } else {
          ebox.position.y += (d - 0.001)
          onGround = false
        }
        velocity.y = 0
        accel.y = 0
      }

      if (ebox.position.y < 0) {
        ebox.position.y = 0;
        velocity.y = 0;
        accel.y = 0;
        onGround = true;
      }
    }

    if (motion.x() == 0 && motion.z() == 0) {
      visualModified = true;
      return
    }

    boxHere = ebox.getAxisAlignedBox
    val tallBoxHere = new EntityBox(ebox.position, ebox.width, ebox.height + 0.625).getAxisAlignedBox
    val tallCollisionHere = world.allIntersectingCollisions(tallBoxHere)

    val target_x: CollisionShape = if (motion.x() != 0) {
      val tallBoxThere = tallBoxHere.offset(motion.x(), 0, 0)
      val collisionThere = world.allIntersectingCollisions(tallBoxThere)
      collisionThere.difference(tallCollisionHere)
    } else {
      null
    }

    val target_z: CollisionShape = if (motion.z() != 0) {
      val tallBoxThere = tallBoxHere.offset(0, 0, motion.z())
      val collisionThere = world.allIntersectingCollisions(tallBoxThere)
      collisionThere.difference(tallCollisionHere)
    } else {
      null
    }

    val target_y: CollisionShape = {
      val boxThere = boxHere.offset(0, 0.625, 0)
      val collisionThere = world.allIntersectingCollisions(boxThere)
      collisionThere.difference(collisionHere)
    }

    val expected_mx = motion.x().abs
    val expected_mz = motion.z().abs

    var best_collision_x = true
    var best_collision_z = true
    var best_step: Double = 0
    var best_dd: Double = 0
    var best_dx: Double = 0
    var best_dz: Double = 0

    breakable {
      for (step_i <- 0 to 20) {
        val step = step_i * 0.03125
        val up = boxHere.offset(0, step, 0)
        val d_y = target_y.minDistance(up) - 0.001
        if (d_y <= 0 && step > 0) break;

        var d_x = if (target_x != null) (target_x.minDistance(up) - 0.001).max(0) else 0
        var d_z = if (target_z != null) (target_z.minDistance(up) - 0.001).max(0) else 0

        if (d_x >= expected_mx && d_z >= expected_mz) {
          best_step = step
          best_dx = expected_mx
          best_dz = expected_mz
          best_collision_x = false
          best_collision_z = false
          break
        }

        var coll_x = false
        if (d_x >= expected_mx) {
          d_x = expected_mx
        } else {
          coll_x = true
        }

        var coll_z = false
        if (d_z >= expected_mz) {
          d_z = expected_mz
        } else {
          coll_z = true
        }

        val dd = d_x*d_x + d_z*d_z
        if (dd > best_dd) {
          best_dd = dd
          best_dx = d_x
          best_dz = d_z
          best_step = step
          best_collision_x = coll_x
          best_collision_z = coll_z
        }

        if (!onGround || !gravity) break
      }
    }

    ebox.position.y += best_step
    ebox.position.x += best_dx * motion.x().sign
    ebox.position.z += best_dz * motion.z().sign
    if (best_collision_x) {
      velocity.x = 0
      accel.x = 0
    }
    if (best_collision_z) {
      velocity.z = 0
      accel.z = 0
    }

    visualModified = true
  }

  def getCameraPos(): Vector3d = {
    new Vector3d(ebox.position.x, ebox.position.y + ebox.height * 0.9, ebox.position.z)
  }

  def setCameraPos(pos: Vector3dc) = {
    ebox.position.x = pos.x()
    ebox.position.y = pos.y() - ebox.height * 0.9
    ebox.position.z = pos.z()
    visualModified = true // May be optional
  }

  def setPosition(pos: Vector3dc): Unit = {
    ebox.position.set(pos)
    visualModified = true // optional?
  }

  def setMesh(mesh: Mesh): Unit = {
    this.mesh = mesh
    visualModified = true
  }

  def setVisible(v: Boolean): Unit = {
    visible = v
    visualModified = true
  }

  def setGravity(g: Boolean): Unit = {
    gravity = g
  }

  def hasGravity(): Boolean = gravity

  def isOnGround(): Boolean = onGround

  def getVelocity(): Vector3dc = velocity
  def setVelocity(v: Vector3dc): Unit = {
    velocity.set(v)
  }
  def addVelocity(dv: Vector3dc): Unit = {
    velocity.add(dv)
  }

  def gameTick(elapsedTime: Double): Unit = {
    val step = elapsedTime * (1.0 / 3.0)
    if (gravity) accel.y = -20

    val pos = new Vector3d(ebox.position)
    for (i <- 0 until 3) {
      // Update position from velocity
      pos.fma(step, velocity)

      // Apply friction
      if (onGround && gravity) {
        val hvel = new Vector3d(velocity.x, 0, velocity.z)
        if (hvel.x > 0.1 || hvel.z > 0.1) hvel.normalize()
        hvel.mul(100)
        val friction = new Vector3d(-hvel.x * 3, 0, -hvel.z * 3)
        if (friction.x.abs > velocity.x.abs) friction.x = -velocity.x
        if (friction.z.abs > velocity.z.abs) friction.z = -velocity.z
        velocity.add(friction)
      }

      velocity.y = velocity.y.max(-max_y_vel).min(max_y_vel)
    }

    pos.sub(ebox.position)
    move(pos)
  }

  def setHorizontalVelocity(vx: Double, vz: Double) = {
    velocity.x = vx
    velocity.z = vz
  }

  def setVerticalVelocity(vy: Double): Unit = {
    velocity.y = vy.max(-max_y_vel).min(max_y_vel)
  }

  def computeVisualUpdates(viewCenter: BlockPos, view: Matrix4fc): Unit = {
    if (!visible || !visualModified || mesh==null) return;
    if (!insideFrustum(view, viewCenter)) return;
    visualModified = false;

    val pos = ebox.position

    var mr1 = render_alt
    if (mr1 == null) {
      mr1 = new MeshRenderer(mesh.getTexture)
      current_pos.set(pos)
      current_time = Window.getCurrentTime
    }

    target_pos.set(pos)
    target_time = Window.getCurrentTime + 0.05

    tmpPosList(0) = current_pos
    tmpMeshList(0) = mesh
    tmpFaceList(0) = -1
    mr1.loadMeshes(tmpMeshList, tmpPosList.asInstanceOf[Array[Object]], tmpFaceList, 1, viewCenter)

    render_alt = render
    render = mr1
  }

  def insideFrustum(view: Matrix4fc, viewCenter: BlockPos): Boolean = {
    true // XXX Actually check frustum
  }

  def draw(shader: Shader, camera: CameraModel): Unit = {
    if (!visible || render==null) return;

    val mr = render
    if (mr == null) return

    val viewCenter = mr.getViewCenter
    val viewMatrix = camera.getViewMatrix(viewCenter)
    if (!insideFrustum(viewMatrix, viewCenter)) return;

    val now = Window.getCurrentTime
    val elapsed = now - start_time
    val togo = target_time - now

    if (togo > 0) {
      val tmp = new Vector3d()
      val interpFactor = elapsed / (target_time - start_time)
      start_pos.lerp(target_pos, interpFactor, current_pos)
    } else {
      current_pos.set(target_pos)
    }

    current_time = now

    val modelMatrix = new Matrix4f().translate(
      (current_pos.x - target_pos.x).toFloat,
      (current_pos.x - target_pos.x).toFloat,
      (current_pos.x - target_pos.x).toFloat)
    shader.setMat4("view", viewMatrix)
    shader.setMat4("model", modelMatrix)
    mr.draw(shader)
  }


  def destroy(): Unit = {
    Disposer.dispose(render)
    Disposer.dispose(render_alt)
  }
}


object Entity {
  var max_y_vel: Double = 50.0

  private val tmpMeshList = new Array[Mesh](1)
  private val tmpPosList = new Array[Vector3dc](1)
  private val tmpFaceList = new Array[Int](1)
}