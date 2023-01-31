package org.theosib.Geometry

import org.theosib.Position.BlockPos
import org.theosib.Utils.Facing
import org.joml.{Vector3d, Vector3dc}

object GeometryFunctions {

  /**
   * Compute distance from camera to a plane along the direction the camera is pointing
   * @param cameraPos Location of camera
   * @param forward Forward vector, direction camera is facing
   * @param planeNormal A normal vector to the plane
   * @param planePoint Some point on the plane
   * @return
   */
  def intersectDistance(cameraPos: Vector3dc, forward: Vector3dc, planeNormal: Vector3dc, planePoint: Vector3dc): Double = {
    val denom = planeNormal.dot(forward)
    if (denom >= 0) return -1
    val dif = new Vector3d();
    planePoint.sub(cameraPos, dif)
    val numer = planeNormal.dot(dif)
    numer / denom
  }

  /**
   * Compute position forward from camera by distance
   * @param cameraPos location of camera
   * @param forward direction camera is looking
   * @param r distance in units of forward normal vector
   * @return position forward from camera
   */
  def projectForward(cameraPos: Vector3dc, forward: Vector3dc, r: Double): Vector3dc = {
    val prod = new Vector3d()
    val sum = new Vector3d()
    forward.mul(r, prod)
    cameraPos.add(prod, sum)
  }

  /**
   * Compute block position (with real components) from arbitrary point in space
   * @param point point in space
   * @return block position (real components)
   */
  def blockCorner(point: Vector3dc): Vector3dc = {
    val result = new Vector3d()
    point.floor(result)
  }

  /**
   * Given a position in space and the block containing that position,
   * what is the distance to the specified interior face? (Even if that's outside the block)
   * @param pos location of camera
   * @param forward direction camera is looking
   * @param face which inner cube surface
   * @return distance to surface
   */
  def distanceToInnerFace(pos: Vector3dc, forward: Vector3dc, face: Int): Double = {
    val planeNormal = inwardNormal(face)
    val planePoint = inwardCorner(face)
    intersectDistance(pos, forward, planeNormal, planePoint)
  }

  def distanceToOuterFace(pos: Vector3dc, forward: Vector3dc, relativeBlockPos: Vector3dc, face: Int): Double = {
    val planeNormal = outwardNormal(face)
    val planePoint = outwardCorner(face)
    val sum = new Vector3d()
    planePoint.add(relativeBlockPos, sum)
    intersectDistance(pos, forward, planeNormal, sum)
  }

  /**
   * Given a point and a forward direction, which block position is being looked at?
   * If the position is less than 1/1000 away from a block surface, project through to the
   * next block in space.
   * @param point Point in space
   * @param forward Forward vector
   * @return
   */
  def whichBlock(point: Vector3dc, forward: Vector3dc): BlockPos = {
    val corner = blockCorner(point)
    val frac = new Vector3d()
    point.sub(corner, frac)
    var x = corner.x().toInt
    var y = corner.y().toInt
    var z = corner.z().toInt
    if (frac.x<0.001 && forward.x()<0) x -= 1
    if (frac.y<0.001 && forward.y()<0) y -= 1
    if (frac.z<0.001 && forward.z()<0) z -= 1
    if (frac.x>0.999 && forward.x()>0) x += 1
    if (frac.y>0.999 && forward.y()>0) y += 1
    if (frac.z>0.999 && forward.z()>0) z += 1
    new BlockPos(x, y, z)
  }


  /**
   * Which interior face of the block containing a position is closest to the point in the given direction
   * @param posIn position in block
   * @param forward direction looking
   * @return tuple: (face, distance)
   */
  def exitFace(posIn: Vector3dc, forward: Vector3dc): (Int, Double) = {
    val blockPos = whichBlock(posIn, forward)
    val pos = new Vector3d()
    pos.x = posIn.x - blockPos.X
    pos.y = posIn.y - blockPos.Y
    pos.z = posIn.z - blockPos.Z

    var min_d: Double = -1
    var min_face: Int = -1
    for (face <- 0 until Facing.NUM_FACES) {
      val d = distanceToInnerFace(pos, forward, face)
      if (d > 0) {
        if (min_face<0 || d<min_d) {
          min_face = face
          min_d = d
        }
      }
    }

    (min_face, min_d)
  }

  /**
   * Compute a new origin for viewing the world relative to the camera. All positions sent to graphics rendering
   * are computed relative to this point so as to avoid float rounding artifacts when the camera is very far from
   * the world origin.
   * @param cameraPos Absolute position of camera
   * @return Graphics origin of what is rendered
   */
  def worldViewCenter(cameraPos: Vector3dc): BlockPos =
    new BlockPos(cameraPos.x().toInt, cameraPos.y().toInt, cameraPos.z().toInt)




  // Normal vectors of cube faces, pointing inwards
  private val inwardNormalTuples = Array(
    (0.0, 1.0, 0.0), // DOWN
    (0.0, -1.0, 0.0), // UP
    (0.0, 0.0, 1.0), // NORTH
    (0.0, 0.0, -1.0), // SOUTH
    (1.0, 0.0, 0.0), // WEST
    (-1.0, 0.0, 0.0) // EAST
  )
  private val inwardNormal = inwardNormalTuples.map(convertToVector3d(_))

  // Points on cube faces
  private val inwardCornerTuples = Array(
    (0.0, 0.0, 0.0), // DOWN
    (0.0, 1.0, 1.0), // UP
    (0.0, 1.0, 0.0), // NORTH
    (1.0, 1.0, 1.0), // SOUTH
    (0.0, 1.0, 1.0), // WEST
    (1.0, 1.0, 0.0) // EAST
  )
  private val inwardCorner = inwardCornerTuples.map(convertToVector3d(_))

  // Normal vectors of cube faces, pointing outwards
  private val outwardNormalTuples = Array(
    (0.0, -1.0, 0.0), // DOWN
    (0.0, 1.0, 0.0), // UP
    (0.0, 0.0, -1.0), // NORTH
    (0.0, 0.0, 1.0), // SOUTH
    (-1.0, 0.0, 0.0), // WEST
    (1.0, 0.0, 0.0) // EAST
  )
  private val outwardNormal = outwardNormalTuples.map(convertToVector3d(_))

  // Points on cube faces
  private val outwardCornerTuples = Array(
    (0.0, 0.0, 1.0), // DOWN
    (0.0, 1.0, 0.0), // UP
    (1.0, 1.0, 0.0), // NORTH
    (0.0, 1.0, 1.0), // SOUTH
    (0.0, 1.0, 0.0), // WEST
    (1.0, 1.0, 1.0) // EAST
  )
  private val outwardCorner = outwardCornerTuples.map(convertToVector3d(_))

  private def convertToVector3d(tup: (Double, Double, Double)): Vector3d = new Vector3d(tup._1, tup._2, tup._3)


}
