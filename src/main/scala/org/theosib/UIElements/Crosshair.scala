package org.theosib.UIElements

import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.GraphicsEngine.RenderingUtils.Handles
import org.theosib.GraphicsEngine.{RenderingUtils, Shader}
import org.theosib.Utils.WindowDimensions
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil

import java.nio.FloatBuffer

class Crosshair extends RenderAgent {
  private val cross_vertex_code =
    """
      |#version 330 core
      |
      |layout (location = 0) in vec3 aPos;
      |
      |void main()
      |{
      |    gl_Position = vec4(aPos, 1.0);
      |}
      |""".stripMargin

  private val cross_fragment_code =
    """
      |#version 330 core
      |
      |out vec4 FragColor;
      |
      |void main()
      |{
      |    FragColor = vec4(1.0, 1.0, 1.0, 1.0);
      |}
      |""".stripMargin

  private var shader : Shader = null
  private var handles : Handles = null
  private var points : FloatBuffer = null
  private var windowDimensions : WindowDimensions = null
  private var sizeFactor : Float = 0.03f

  def setSizeFactor(factor : Float) = {
    sizeFactor = factor
    computePoints();
  }

  override def create(w: Window): Unit = {
    shader = new Shader().setVertexCode(cross_vertex_code).setFragmentCode(cross_fragment_code)
  }

  override def destroy(): Unit = {
    if (shader != null) shader.destroy()
    if (points != null) MemoryUtil.memFree(points)
    if (handles != null) handles.destroy();
    shader = null
    points = null
    handles = null
    windowDimensions = null
    sizeFactor = 0.03f
  }

  override def willRender(w: Window): Boolean = true

  private def computePoints(): Unit = {
    if (windowDimensions == null) return;
    if (points == null) points = MemoryUtil.memAllocFloat(12)

    val length = sizeFactor * windowDimensions.fb.h
    val adjusted_width = length / windowDimensions.fb.w
    points.put(0, -adjusted_width)
    points.put(1, 0);
    points.put(2, 0);
    points.put(3, adjusted_width)
    points.put(4, 0);
    val adjusted_height = length / windowDimensions.fb.h
    points.put(5, 0);
    points.put(6, 0);
    points.put(7, -adjusted_height)
    points.put(8, 0);
    points.put(9, 0);
    points.put(10, adjusted_height)
    points.put(11, 0);
  }

  override def render(w: Window): Unit = {
    handles = RenderingUtils.renderLines(shader, points, 4, handles)
  }

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    windowDimensions = dim
    computePoints()
  }

  override def priority(): Int = -1000
}
