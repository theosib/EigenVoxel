package org.theosib

import org.joml.{Matrix4f, Vector3d}
import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.Geometry.GeometryFunctions
import org.theosib.GraphicsEngine.{FontAtlas, Shader, StringRenderer}
import org.theosib.Position.BlockPos
import org.theosib.Utils.{Disposer, WindowDimensions}

class TestText(val camera: CameraModel) extends RenderAgent {
  var shader: Shader = null
  var atlas: FontAtlas = null;
  var render: StringRenderer = null;

  override def create(w: Window): Unit = {
    shader = new Shader().setVertexCodeFile("string_vertex.glsl").setFragmentCodeFile("string_fragment.glsl")
    shader.setMat4("projection", w.getProjectionMatrix)
    shader.setMat4("view", new Matrix4f())
    shader.setColor("fgColor", 0xffffffff);
    atlas = FontAtlas.lookupFont("default")
    render = new StringRenderer(atlas)

    println(w.getProjectionMatrix)

    val corner = new Vector3d(-0.9, 1, 4.1);
    val right = new Vector3d(1, 0, 0);
    val down = new Vector3d(0, -1, 0);
    //     void loadMesh(String string, Vector3dc corner, Vector3dc right, Vector3dc down, double hscale, double vscale, BlockPos viewCenter) {

    render.loadMesh("This text is for you!", corner, right, down, 0.05, 0.05, new BlockPos())

//    render = new StringRenderer("T", atlas,
//      new Vector3d(), new Vector3d(1, 0, 0), new Vector3d(0, -1, 0), 0.1, 0.1)
  }

  override def destroy(): Unit = {
    Disposer.dispose(shader)
    Disposer.dispose(render)
  }

  override def willRender(w: Window, elapsedTime: Double): Boolean = true

  override def render(w: Window, elapsedTime: Double): Unit = {
    //val viewCenter = GeometryFunctions.worldViewCenter(camera.getPos())
    shader.setMat4("view", camera.getViewMatrix(new BlockPos))
    render.draw(shader)
  }

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    val projection = w.getProjectionMatrix
    shader.setMat4("projection", projection)
  }

  override def priority(): Int = 1001
}
