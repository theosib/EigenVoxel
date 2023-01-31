package org.theosib

import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.GraphicsEngine.RenderingUtils.Handles
import org.theosib.GraphicsEngine.{RenderingUtils, Shader}
import org.theosib.TestTriangle.{fragment_shader, vertex_shader}
import org.theosib.Utils.{Disposer, WindowDimensions}
import org.joml.{Matrix4f, Matrix4fc, Vector3d, Vector4f}
import org.lwjgl.system.MemoryUtil

import java.nio.FloatBuffer

class TestTriangle(camera: CameraModel) extends RenderAgent {
  private var shader: Shader = null
  private var handles: Handles = null
  private var points: FloatBuffer = null
  private var windowDimensions: WindowDimensions = null
  private var projectionMatrix: Matrix4fc = null

  override def create(w: Window): Unit = {
    shader = new Shader().setVertexCode(vertex_shader).setFragmentCode(fragment_shader)
    val view = camera.getViewMatrix()
    val proj = w.getProjectionMatrix

    println(s"viewMatrix:\n${view}")
    println(s"proj:\n${proj}")

    val x = new Vector4f(1, 1, 0, 1);
    val y = new Vector4f();
    x.mul(proj, y)
    println(s"prod:\n${y}")

    shader.setMat4("view", view)
    computePoints()
  }

  override def destroy(): Unit = {
    Disposer.dispose(shader)
    if (points != null) MemoryUtil.memFree(points)
    Disposer.dispose(handles)
    shader = null
    points = null
    handles = null
    windowDimensions = null
  }

  private def computePoints(): Unit = {
    if (points == null) points = MemoryUtil.memAllocFloat(9)

    points.put(0, -1 * 0.8f)
    points.put(1, -1 * 0.9f)
    points.put(2, 0)

    points.put(3, -1 * 0.8f)
    points.put(4, 1 * 0.9f)
    points.put(5, 0)

    points.put(6, 1 * 0.8f)
    points.put(7, 1 * 0.9f)
    points.put(8, 0)
  }

  override def willRender(w: Window): Boolean = true

  override def render(w: Window): Unit = {
    handles = RenderingUtils.renderTriangles(shader, points, 3, handles)
  }

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    projectionMatrix = w.getProjectionMatrix
    shader.setMat4("projection", projectionMatrix)
  }

  override def priority(): Int = 2
}

object TestTriangle {
  val vertex_shader =
    """
      |#version 330 core
      |
      |layout (location = 0) in vec3 aPos;
      |
      |out vec3 FragPos;
      |
      |uniform mat4 view;
      |uniform mat4 projection;
      |
      |void main()
      |{
      |    gl_Position = projection * view * vec4(aPos, 1.0);
      |    FragPos = aPos;
      |}
      |""".stripMargin

  val fragment_shader =
    """
      |#version 330 core
      |
      |out vec4 FragColor;
      |in vec3 FragPos;
      |
      |void main()
      |{

      |    FragColor = vec4(1.0, 1.0, 1.0, 1.0);
      |}
      |
      |""".stripMargin
}

