package org.theosib.WorldElements

import org.joml.{Matrix4fc, Vector3d}
import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Geometry.EntityBox
import org.theosib.GraphicsEngine.{Mesh, MeshRenderer}
import org.theosib.Utils.{Disposer, WindowDimensions}

class Entity extends RenderAgent {
  var ebox: EntityBox = new EntityBox()
  var velocity = new Vector3d()
  var accel = new Vector3d()
  var gravity: Boolean = false
  var on_ground: Boolean = false

  var target_pos = new Vector3d()
  var current_pos = new Vector3d()
  var target_time: Double = 0
  var current_time: Double = 0

  var yaw: Double = 0

  var visible: Boolean = true
  var mesh: Mesh = null

  var render: MeshRenderer = null
  var render_alt: MeshRenderer = null

  var visualModified: Boolean = false

  var projection: Matrix4fc = null

  def setProjection(proj: Matrix4fc): Unit = {
    projection = proj
    visualModified = true
  }

  def move(motion: Vector3d): Unit = {

  }

  def getCameraPos(): Vector3d = {
    new Vector3d(ebox.)
  }






  override def create(w: Window): Unit = ???

  override def destroy(): Unit = {
    Disposer.dispose(render)
    Disposer.dispose(render_alt)
  }

  override def willRender(w: Window): Boolean = ???

  override def render(w: Window): Unit = ???

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    setProjection(w.getProjectionMatrix)
  }

  override def priority(): Int = 1
}
