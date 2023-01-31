package org.theosib.Utils

object Packing {
  val blockX_bits = 26
  val blockZ_bits = 26
  val blockY_bits = 16

  val blockX_shift = 0
  val blockZ_shift = blockX_bits
  val blockY_shift = blockZ_shift + blockZ_bits

  val blockX_mask = (1L << blockX_bits) - 1
  val blockZ_mask = (1L << blockZ_bits) - 1
  val blockY_mask = (1L << blockY_bits) - 1

  val chunkX_bits = blockX_bits - 4
  val chunkZ_bits = blockZ_bits - 4
  val chunkY_bits = blockY_bits - 4

  val chunkX_shift = 0
  val chunkZ_shift = chunkX_bits
  val chunkY_shift = chunkZ_shift + chunkZ_bits

  val chunkX_mask = (1L << chunkX_bits) - 1
  val chunkZ_mask = (1L << chunkZ_bits) - 1
  val chunkY_mask = (1L << chunkY_bits) - 1
}
