package org.theosib

import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Camera.CameraModel
import org.theosib.Geometry.GeometryFunctions
import org.theosib.GraphicsEngine._
import org.theosib.Position.BlockPos
import org.theosib.TestQuad.{fragment_block, vertex_block}
import org.theosib.Utils.{Disposer, WindowDimensions}
import org.joml.{Matrix4f, Matrix4fc}

import java.util

class TestBlock(val camera: CameraModel) extends RenderAgent {
  val mesh = new Mesh()
  var shader: Shader = null
  var window: Window = null
  var meshrenderer: MeshRenderer = null;
  var projectionMatrix: Matrix4fc = null;

  override def create(w: Window): Unit = {
    window = w
    mesh.loadMesh("wood");

    val mesh_list = Array(mesh)
    val pos_list = Array(new BlockPos())
    val face_list = Array(-1)

    val cameraPos = camera.getPos()
    val viewCenter = GeometryFunctions.worldViewCenter(cameraPos)

    meshrenderer = new MeshRenderer(mesh.getTexture)
    meshrenderer.loadMeshes(mesh_list, pos_list.asInstanceOf[Array[Object]], face_list, 1, viewCenter)

    shader = new Shader().setVertexCode(vertex_block).setFragmentCode(fragment_block)
    shader.setMat4("view", camera.getViewMatrix(meshrenderer.getViewCenter))
  }

  override def destroy(): Unit = {
    Disposer.dispose(shader)
  }

  override def willRender(w: Window, deltaTime: Double): Boolean = true

  override def render(w: Window, deltaTime: Double): Unit = {
    shader.setMat4("view", camera.getViewMatrix(meshrenderer.getViewCenter))
    meshrenderer.draw(shader)
  }

  override def resize(w: Window, dim: WindowDimensions): Unit = {
    projectionMatrix = w.getProjectionMatrix
    shader.setMat4("projection", projectionMatrix)
  }

  override def priority(): Int = 0
}

object TestBlock {
  val vertex_block =
    """
      |#version 330 core
      |
      |layout (location = 0) in vec3 aPos;
      |layout (location = 1) in vec3 aNormal;
      |layout (location = 2) in vec2 aTexCoord;
      |
      |out vec3 FragPos;
      |out vec3 Normal;
      |out vec2 TexCoord;
      |
      |uniform mat4 view;
      |uniform mat4 projection;
      |
      |void main()
      |{
      |    gl_Position = projection * view * vec4(aPos, 1.0);
      |    FragPos = aPos;
      |    Normal = aNormal;
      |    TexCoord = aTexCoord;
      |}
      |
      |
      |""".stripMargin

  val fragment_block =
    """
      |#version 330 core
      |
      |out vec4 FragColor;
      |in vec3 FragPos;
      |in vec3 Normal;
      |in vec2 TexCoord;
      |
      |uniform sampler2D ourTexture;
      |
      |void main()
      |{
      |    vec3 lightPos = vec3(-500.0, 1000.0, 1000.0);
      |    vec3 lightDir = normalize(lightPos - FragPos);
      |    float diff = max(dot(Normal, lightDir), 0.0);
      |    vec3 diffuse = vec3(diff * 0.7);
      |    vec3 ambient = vec3(0.3);
      |    vec4 fragment4 = texture(ourTexture, TexCoord);
      |    vec3 fragment3 = vec3(fragment4);
      |    vec3 result = (ambient + diffuse) * fragment3;
      |    FragColor = vec4(result, fragment4.a);
      |    //FragColor = vec4(1, 1, 1, 1);
      |}
      |
      |""".stripMargin

}