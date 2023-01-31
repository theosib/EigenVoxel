package org.theosib.Utils

import org.joml.{Vector3f, Vector3i}

object Facing {
  val DOWN = 0      // negative Y
  val UP = 1        // positive Y
  val NORTH = 2     // negative Z
  val SOUTH = 3     // positive Z
  val WEST = 4      // negative X
  val EAST = 5      // positive X
  val NUM_FACES = 6

  val DOWN_MASK = 1<<DOWN
  val UP_MASK = 1<<UP
  val NORTH_MASK = 1<<NORTH
  val SOUTH_MASK = 1<<SOUTH
  val WEST_MASK = 1<<WEST
  val EAST_MASK = 1<<EAST

  def bitMask(face: Int, bit: Int = 1): Int = { bit << face }
  def oppositeFace(face: Int): Int = { face ^ 1 }

  def hasFace(faceMask: Int, face: Int): Boolean = { (faceMask & bitMask(face)) != 0 }

  val face_name = Array(
    "DOWN",
    "UP",
    "NORTH",
    "SOUTH",
    "WEST",
    "EAST")

  val int_tuple = Array(
    (0, -1, 0),
    (0, 1, 0),
    (0, 0, -1),
    (0, 0, 1),
    (-1, 0, 0),
    (1, 0, 0))

  val int_vector: Array[Vector3i] = int_tuple.map(t => new Vector3i(t._1, t._2, t._3))
  val float_vector: Array[Vector3f] = int_tuple.map(t => new Vector3f(t._1, t._2, t._3))

  def faceFromName(name: String): Int = {
    if (name.charAt(0).isDigit) return name.toInt
    name.charAt(0).toLower match {
      case 'd' | 'b' => DOWN
      case 'u' | 't' => UP
      case 'n' => NORTH
      case 's' => SOUTH
      case 'w' => WEST
      case 'e' => EAST
      case _ => -1
    }
  }

  // XXX Rotation stuff

}
