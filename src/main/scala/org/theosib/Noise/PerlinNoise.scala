package org.theosib.Noise

import scala.util.Random

object PerlinNoise {
  def perlinNoise(xIn: Double, yIn: Double): Double = {
    val X = math.floor(xIn).toInt & 255
    val Y = math.floor(yIn).toInt & 255
    val x = xIn - math.floor(xIn)
    val y = yIn - math.floor(yIn)
    val u = fade(x)
    val v = fade(y)
    val A = p(X  ) + Y
    val B = p(X+1) + Y
    lerp(v, lerp(u, grad(p(A  ), x  , y  ),
      grad(p(B  ), x-1, y  )),
      lerp(u, grad(p(A+1), x  , y-1),
        grad(p(B+1), x-1, y-1)))
  }

  def fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

  def lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)

  def grad(hash: Int, x: Double, y: Double): Double = {
    val h = hash & 15
    val u = if (h<8) x else y
    val v = if (h<4) y else if (h==12||h==14) x else 0
    ((h&1) match {
      case 0 => u
      case _ => -u
    }) + ((h&2) match {
      case 0 => v
      case _ => -v
    })
  }

  val p = Array.fill(512)(0) ++ Array.fill(512)(1)
  Random.shuffle(p.toList).toArray
}
