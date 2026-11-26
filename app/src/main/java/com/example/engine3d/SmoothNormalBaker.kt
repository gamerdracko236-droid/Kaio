package com.example.engine3d

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Calcula normais suavizadas médias por posição compartilhada de vértice,
 * deduplicando direções iguais no mesmo canto para evitar viés de triangulação
 * e impedir frestas/rachaduras durante a extrusão do shader de contorno (GL_FRONT).
 */
object SmoothNormalBaker {

    private const val QUANT_SCALE = 1000f
    private const val DIR_QUANT_SCALE = 100f

    private fun posKey(x: Float, y: Float, z: Float): Long {
        val qx = (x * QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        val qy = (y * QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        val qz = (z * QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        return (qx shl 40) or (qy shl 20) or qz
    }

    private fun dirKey(nx: Float, ny: Float, nz: Float): Long {
        val qx = (nx * DIR_QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        val qy = (ny * DIR_QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        val qz = (nz * DIR_QUANT_SCALE).roundToInt().toLong() and 0xFFFFFL
        return (qx shl 40) or (qy shl 20) or qz
    }

    /**
     * Recebe um array intercalado [px, py, pz, nx, ny, nz, u, v, r, g, b, snx, sny, snz]
     * com [strideFloats] = 14 floats por vértice e grava a normal suavizada nos offsets 11..13.
     */
    fun bake(vertices: FloatArray, strideFloats: Int = 14): FloatArray {
        val out = vertices.copyOf()
        val count = out.size / strideFloats
        if (count <= 0) return out

        val accum = HashMap<Long, FloatArray>(count)
        val seenDirs = HashMap<Long, MutableSet<Long>>(count)

        for (i in 0 until count) {
            val base = i * strideFloats
            val key = posKey(out[base], out[base + 1], out[base + 2])
            val nx = out[base + 3]
            val ny = out[base + 4]
            val nz = out[base + 5]
            val dKey = dirKey(nx, ny, nz)

            val dirs = seenDirs.getOrPut(key) { HashSet(6) }
            if (dirs.add(dKey)) {
                val acc = accum.getOrPut(key) { FloatArray(3) }
                acc[0] += nx
                acc[1] += ny
                acc[2] += nz
            }
        }

        for (acc in accum.values) {
            val len = sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2])
            if (len > 1e-5f) {
                acc[0] /= len
                acc[1] /= len
                acc[2] /= len
            } else {
                acc[0] = 0f
                acc[1] = 1f
                acc[2] = 0f
            }
        }

        for (i in 0 until count) {
            val base = i * strideFloats
            val key = posKey(out[base], out[base + 1], out[base + 2])
            val sn = accum[key] ?: continue
            out[base + 11] = sn[0]
            out[base + 12] = sn[1]
            out[base + 13] = sn[2]
        }

        return out
    }
}
