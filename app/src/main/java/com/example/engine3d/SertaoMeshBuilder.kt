package com.example.engine3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GpuMesh(
    val vboId: Int,
    val vertexCount: Int
) {
    fun release() {
        if (vboId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
        }
    }
}

data class RawVertex(
    val x: Float, val y: Float, val z: Float,
    val nx: Float, val ny: Float, val nz: Float,
    val u: Float, val v: Float
)

object SertaoMeshBuilder {
    // Interleaved layout: pos(3) + normal(3) + smoothNormal(3) + uv(2) = 11 floats = 44 bytes
    const val FLOATS_PER_VERTEX = 11
    const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4

    fun uploadVertices(vertices: List<RawVertex>): GpuMesh {
        val packed = packWithSmoothNormals(vertices)
        return uploadPackedArray(packed)
    }

    fun uploadPackedArray(packed: FloatArray): GpuMesh {
        val count = packed.size / FLOATS_PER_VERTEX
        if (count == 0) return GpuMesh(0, 0)

        val buf = ByteBuffer.allocateDirect(packed.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(packed)
        buf.position(0)

        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        val vboId = ids[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            packed.size * 4,
            buf,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return GpuMesh(vboId, count)
    }

    /**
     * Averages face normals at shared quantized positions so the GL_FRONT outline pass
     * extrudes corners seamlessly without cracks on cubes, cylinders, and articulated limbs.
     */
    fun packWithSmoothNormals(vertices: List<RawVertex>): FloatArray {
        val dirSets = HashMap<Long, MutableSet<Long>>(vertices.size)
        val accum = HashMap<Long, FloatArray>(vertices.size)
        for (v in vertices) {
            val key = posKey(v.x, v.y, v.z)
            val nKey = posKey(v.nx, v.ny, v.nz)
            val seen = dirSets.getOrPut(key) { HashSet(4) }
            if (seen.add(nKey)) {
                val sum = accum.getOrPut(key) { floatArrayOf(0f, 0f, 0f) }
                sum[0] += v.nx
                sum[1] += v.ny
                sum[2] += v.nz
            }
        }
        for ((_, sum) in accum) {
            val len = sqrt(sum[0] * sum[0] + sum[1] * sum[1] + sum[2] * sum[2]).coerceAtLeast(1e-5f)
            sum[0] /= len
            sum[1] /= len
            sum[2] /= len
        }

        val out = FloatArray(vertices.size * FLOATS_PER_VERTEX)
        var idx = 0
        for (v in vertices) {
            val smooth = accum[posKey(v.x, v.y, v.z)] ?: floatArrayOf(v.nx, v.ny, v.nz)
            out[idx++] = v.x
            out[idx++] = v.y
            out[idx++] = v.z
            out[idx++] = v.nx
            out[idx++] = v.ny
            out[idx++] = v.nz
            out[idx++] = smooth[0]
            out[idx++] = smooth[1]
            out[idx++] = smooth[2]
            out[idx++] = v.u
            out[idx++] = v.v
        }
        return out
    }

    private fun posKey(x: Float, y: Float, z: Float): Long {
        val qx = (x * 1000f).roundToInt().toLong() and 0x1FFFFFL
        val qy = (y * 1000f).roundToInt().toLong() and 0x1FFFFFL
        val qz = (z * 1000f).roundToInt().toLong() and 0x1FFFFFL
        return (qx shl 42) or (qy shl 21) or qz
    }

    fun buildCubeVertices(uvScaleX: Float = 1f, uvScaleY: Float = 1f): List<RawVertex> {
        val p = 0.5f
        val n = -0.5f
        val list = ArrayList<RawVertex>(36)
        // Front (+Z)
        addQuad(list, n, n, p, p, n, p, p, p, p, n, p, p, 0f, 0f, 1f, uvScaleX, uvScaleY)
        // Back (-Z)
        addQuad(list, p, n, n, n, n, n, n, p, n, p, p, n, 0f, 0f, -1f, uvScaleX, uvScaleY)
        // Left (-X)
        addQuad(list, n, n, n, n, n, p, n, p, p, n, p, n, -1f, 0f, 0f, uvScaleX, uvScaleY)
        // Right (+X)
        addQuad(list, p, n, p, p, n, n, p, p, n, p, p, p, 1f, 0f, 0f, uvScaleX, uvScaleY)
        // Top (+Y)
        addQuad(list, n, p, p, p, p, p, p, p, n, n, p, n, 0f, 1f, 0f, uvScaleX, uvScaleY)
        // Bottom (-Y)
        addQuad(list, n, n, n, p, n, n, p, n, p, n, n, p, 0f, -1f, 0f, uvScaleX, uvScaleY)
        return list
    }

    fun buildTaperedBoxVertices(topScaleX: Float, topScaleZ: Float): List<RawVertex> {
        val b = 0.5f
        val tx = 0.5f * topScaleX
        val tz = 0.5f * topScaleZ
        val list = ArrayList<RawVertex>(36)
        addTriFaceQuad(list, -b, -b, b, b, -b, b, tx, b, tz, -tx, b, tz, 1f, 1f)
        addTriFaceQuad(list, b, -b, -b, -b, -b, -b, -tx, b, -tz, tx, b, -tz, 1f, 1f)
        addTriFaceQuad(list, -b, -b, -b, -b, -b, b, -tx, b, tz, -tx, b, -tz, 1f, 1f)
        addTriFaceQuad(list, b, -b, b, b, -b, -b, tx, b, -tz, tx, b, tz, 1f, 1f)
        addTriFaceQuad(list, -tx, b, tz, tx, b, tz, tx, b, -tz, -tx, b, -tz, 1f, 1f)
        addTriFaceQuad(list, -b, -b, -b, b, -b, -b, b, -b, b, -b, -b, b, 1f, 1f)
        return list
    }

    fun buildCylinderVertices(segments: Int, topRadiusScale: Float = 1f): List<RawVertex> {
        val list = ArrayList<RawVertex>(segments * 12)
        val rBot = 0.5f
        val rTop = 0.5f * topRadiusScale
        for (i in 0 until segments) {
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments
            val a0 = u0 * 2f * PI.toFloat()
            val a1 = u1 * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val x0b = c0 * rBot; val z0b = s0 * rBot
            val x1b = c1 * rBot; val z1b = s1 * rBot
            val x0t = c0 * rTop; val z0t = s0 * rTop
            val x1t = c1 * rTop; val z1t = s1 * rTop

            list += RawVertex(x0b, -0.5f, z0b, c0, 0f, s0, u0, 1f)
            list += RawVertex(x1b, -0.5f, z1b, c1, 0f, s1, u1, 1f)
            list += RawVertex(x1t, 0.5f, z1t, c1, 0f, s1, u1, 0f)

            list += RawVertex(x0b, -0.5f, z0b, c0, 0f, s0, u0, 1f)
            list += RawVertex(x1t, 0.5f, z1t, c1, 0f, s1, u1, 0f)
            list += RawVertex(x0t, 0.5f, z0t, c0, 0f, s0, u0, 0f)

            // Top cap
            list += RawVertex(0f, 0.5f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            list += RawVertex(x1t, 0.5f, z1t, 0f, 1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
            list += RawVertex(x0t, 0.5f, z0t, 0f, 1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)

            // Bottom cap
            list += RawVertex(0f, -0.5f, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            list += RawVertex(x0b, -0.5f, z0b, 0f, -1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)
            list += RawVertex(x1b, -0.5f, z1b, 0f, -1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
        }
        return list
    }

    fun buildConeVertices(segments: Int): List<RawVertex> {
        val list = ArrayList<RawVertex>(segments * 6)
        val r = 0.5f
        for (i in 0 until segments) {
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments
            val a0 = u0 * 2f * PI.toFloat()
            val a1 = u1 * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val amid = (a0 + a1) * 0.5f
            list += RawVertex(c0 * r, -0.5f, s0 * r, c0, 0.45f, s0, u0, 1f)
            list += RawVertex(c1 * r, -0.5f, s1 * r, c1, 0.45f, s1, u1, 1f)
            list += RawVertex(0f, 0.5f, 0f, cos(amid), 0.55f, sin(amid), (u0 + u1) * 0.5f, 0f)

            list += RawVertex(0f, -0.5f, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            list += RawVertex(c0 * r, -0.5f, s0 * r, 0f, -1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)
            list += RawVertex(c1 * r, -0.5f, s1 * r, 0f, -1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
        }
        return list
    }

    fun buildSphereVertices(stacks: Int, slices: Int): List<RawVertex> {
        val list = ArrayList<RawVertex>(stacks * slices * 6)
        val r = 0.5f
        for (i in 0 until stacks) {
            val v0 = 1f - i.toFloat() / stacks
            val v1 = 1f - (i + 1).toFloat() / stacks
            val p0 = (i.toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            val p1 = ((i + 1).toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            for (j in 0 until slices) {
                val u0 = j.toFloat() / slices
                val u1 = (j + 1).toFloat() / slices
                val t0 = u0 * 2f * PI.toFloat()
                val t1 = u1 * 2f * PI.toFloat()

                val n0 = sphereNorm(p0, t0)
                val n1 = sphereNorm(p0, t1)
                val n2 = sphereNorm(p1, t1)
                val n3 = sphereNorm(p1, t0)

                list += RawVertex(n0.first * r, n0.second * r, n0.third * r, n0.first, n0.second, n0.third, u0, v0)
                list += RawVertex(n1.first * r, n1.second * r, n1.third * r, n1.first, n1.second, n1.third, u1, v0)
                list += RawVertex(n2.first * r, n2.second * r, n2.third * r, n2.first, n2.second, n2.third, u1, v1)

                list += RawVertex(n0.first * r, n0.second * r, n0.third * r, n0.first, n0.second, n0.third, u0, v0)
                list += RawVertex(n2.first * r, n2.second * r, n2.third * r, n2.first, n2.second, n2.third, u1, v1)
                list += RawVertex(n3.first * r, n3.second * r, n3.third * r, n3.first, n3.second, n3.third, u0, v1)
            }
        }
        return list
    }

    fun buildSkyDomeVertices(stacks: Int, slices: Int): List<RawVertex> {
        val list = ArrayList<RawVertex>(stacks * slices * 6)
        val r = 1.0f
        for (i in 0 until stacks) {
            val v0 = 1f - i.toFloat() / stacks
            val v1 = 1f - (i + 1).toFloat() / stacks
            val p0 = (i.toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            val p1 = ((i + 1).toFloat() / stacks) * PI.toFloat() - PI.toFloat() * 0.5f
            for (j in 0 until slices) {
                val u0 = j.toFloat() / slices
                val u1 = (j + 1).toFloat() / slices
                val t0 = u0 * 2f * PI.toFloat()
                val t1 = u1 * 2f * PI.toFloat()

                val n0 = sphereNorm(p0, t0)
                val n1 = sphereNorm(p0, t1)
                val n2 = sphereNorm(p1, t1)
                val n3 = sphereNorm(p1, t0)

                // Inward winding so the inside of the sky dome renders cleanly
                list += RawVertex(n0.first * r, n0.second * r, n0.third * r, 0f, 1f, 0f, u0 * 2f, v0)
                list += RawVertex(n2.first * r, n2.second * r, n2.third * r, 0f, 1f, 0f, u1 * 2f, v1)
                list += RawVertex(n1.first * r, n1.second * r, n1.third * r, 0f, 1f, 0f, u1 * 2f, v0)

                list += RawVertex(n0.first * r, n0.second * r, n0.third * r, 0f, 1f, 0f, u0 * 2f, v0)
                list += RawVertex(n3.first * r, n3.second * r, n3.third * r, 0f, 1f, 0f, u0 * 2f, v1)
                list += RawVertex(n2.first * r, n2.second * r, n2.third * r, 0f, 1f, 0f, u1 * 2f, v1)
            }
        }
        return list
    }

    fun buildGroundDiscVertices(segments: Int): List<RawVertex> {
        val list = ArrayList<RawVertex>(segments * 3)
        val r = 0.5f
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            list += RawVertex(0f, 0f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            list += RawVertex(c1 * r, 0f, s1 * r, 0f, 1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
            list += RawVertex(c0 * r, 0f, s0 * r, 0f, 1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)
        }
        return list
    }

    fun buildCrossedGrassTuftVertices(): List<RawVertex> {
        val list = ArrayList<RawVertex>(24)
        val w = 0.45f
        val h = 0.72f
        // Two double-sided crossed planes forming a lush tuft silhouette
        addDoubleSidedQuad(list, -w, 0f, 0f, w, 0f, 0f, w, h, 0f, -w, h, 0f, 0f, 1f, 0f)
        addDoubleSidedQuad(list, 0f, 0f, -w, 0f, 0f, w, 0f, h, w, 0f, h, -w, 0f, 1f, 0f)
        return list
    }

    fun buildDoubleSidedBannerQuad(): List<RawVertex> {
        val list = ArrayList<RawVertex>(12)
        addDoubleSidedQuad(list, -0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0f, 0f, 1f)
        return list
    }

    private fun sphereNorm(phi: Float, theta: Float): Triple<Float, Float, Float> {
        val c = cos(phi)
        return Triple(c * cos(theta), sin(phi), c * sin(theta))
    }

    private fun addQuad(
        list: MutableList<RawVertex>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        uScale: Float, vScale: Float
    ) {
        list += RawVertex(x0, y0, z0, nx, ny, nz, 0f, vScale)
        list += RawVertex(x1, y1, z1, nx, ny, nz, uScale, vScale)
        list += RawVertex(x2, y2, z2, nx, ny, nz, uScale, 0f)

        list += RawVertex(x0, y0, z0, nx, ny, nz, 0f, vScale)
        list += RawVertex(x2, y2, z2, nx, ny, nz, uScale, 0f)
        list += RawVertex(x3, y3, z3, nx, ny, nz, 0f, 0f)
    }

    private fun addDoubleSidedQuad(
        list: MutableList<RawVertex>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        addQuad(list, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, 1f, 1f)
        addQuad(list, x1, y1, z1, x0, y0, z0, x3, y3, z3, x2, y2, z2, nx, ny, nz, 1f, 1f)
    }

    private fun addTriFaceQuad(
        list: MutableList<RawVertex>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        uScale: Float, vScale: Float
    ) {
        val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
        val vx = x2 - x0; val vy = y2 - y0; val vz = z2 - z0
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
        nx /= len; ny /= len; nz /= len
        addQuad(list, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, uScale, vScale)
    }

    /**
     * Collects many static world instances into a single VBO batch so OpenGL ES 2.0 renders
     * dozens of village props, stair steps, rocks, and grass tufts in one draw call.
     */
    class StaticBatch {
        private val batchVerts = ArrayList<RawVertex>(2048)

        fun append(
            template: List<RawVertex>,
            x: Float, y: Float, z: Float,
            sx: Float, sy: Float, sz: Float,
            rotY: Float = 0f,
            uMult: Float = 1f,
            vMult: Float = 1f
        ) {
            val c = if (abs(rotY) > 1e-4f) cos(rotY) else 1f
            val s = if (abs(rotY) > 1e-4f) sin(rotY) else 0f
            for (v in template) {
                val px = v.x * sx
                val py = v.y * sy
                val pz = v.z * sz
                val rx = px * c + pz * s + x
                val rz = -px * s + pz * c + z
                val ry = py + y

                val rnx = v.nx * c + v.nz * s
                val rnz = -v.nx * s + v.nz * c
                val rny = v.ny

                batchVerts += RawVertex(rx, ry, rz, rnx, rny, rnz, v.u * uMult, v.v * vMult)
            }
        }

        fun buildGpuMesh(): GpuMesh = uploadVertices(batchVerts)
    }
}
