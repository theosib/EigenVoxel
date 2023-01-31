package org.theosib

import org.joml.{Vector2f, Vector2fc, Vector3f, Vector3fc}

import java.nio.FloatBuffer
import scala.collection.mutable.ListBuffer

class Face {
  val vertices = new ListBuffer[Vector3fc]
  val texcoords = new ListBuffer[Vector2fc]
  var normal: Vector3fc = null

  def numVertices(): Int = vertices.length

  def numTriangleVertices(): Int = {
    numVertices() match {
      case 0 => 0
      case 3 => 3
      case 4 => 6
      case _ => _
    }
  }

  def addVertex(x: Float, y: Float, z: Float): Face = {
    addVertex(new Vector3f(x, y, z))
  }

  def addVertex(vertex: Vector3fc): Face = {
    vertices.append(vertex)
    if (vertices.length >= 3) computeNormal()
    this
  }

  def addTexCoord(x: Float, y: Float): Face = {
    addTexCoord(new Vector2f(x, y))
  }

  def addTexCoord(texcoord: Vector2fc): Face = {
    texcoords.append(texcoord)
  }

  def computeNormal(): Unit = {
    var a: Vector3fc = null
    var b: Vector3fc = null
    var c: Vector3fc = null

    if (numVertices()==3) {
      a = vertices(0)
      b = vertices(1)
      c = vertices(2)
    } else {
      if (vertices(0).equals(vertices(1)) || vertices(0).equals(vertices(2))) {
        a = vertices(0)
        b = vertices(2)
        c = vertices(3)
      } else {
        a = vertices(0)
        b = vertices(1)
        c = vertices(2)
      }
    }

    val cb = new Vector3f()
    val ab = new Vector3f()
    c.sub(b, cb)
    a.sub(b, ab)
    val x = new Vector3f()
    cb.cross(ab, x)
    normal = x.normalize()
  }

  final private val face_indices = Array(0, 1, 2, 0, 2, 3)

  def getTriangleVertices(outBuf: FloatBuffer, start: Int, offset: Vector3fc): Int = {
    var index = start
    for (i <- 0 until numTriangleVertices()) {
      val v = face_indices(i)
      outBuf.put(index, vertices(v).x() + offset.x()); index += 1;
      outBuf.put(index, vertices(v).y() + offset.y()); index += 1;
      outBuf.put(index, vertices(v).z() + offset.z()); index += 1;
    }
    index
  }

  def getTriangleTextcoords(outBuf: FloatBuffer, start: Int): Int = {
    var index = start
    for (i <- 0 until numTriangleVertices()) {
      val v = face_indices(i)
      outBuf.put(index, texcoords(v).x()); index += 1;
      outBuf.put(index, texcoords(v).y()); index += 1;
    }
    index
  }

  def getTriangleNormals(outBuf: FloatBuffer, start: Int): Int = {
    var index = start;
    for (i <- 0 until numTriangleVertices()) {
      outBuf.put(index, normal.x()); index += 1;
      outBuf.put(index, normal.y()); index += 1;
      outBuf.put(index, normal.z()); index += 1;
    }
    index
  }

/*


   */
}
