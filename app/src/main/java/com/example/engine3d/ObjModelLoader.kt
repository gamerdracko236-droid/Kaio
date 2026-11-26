package com.example.engine3d

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

class LoadedObjMesh(
    val chunks: List<GpuChunk>,
    var textureId: Int = 0
) {
    data class GpuChunk(val vboId: Int, val iboId: Int, val indexCount: Int)

    fun draw(prog: Int, isOutlinePass: Boolean) {
        val stride = StaticSceneBatcher.STRIDE_BYTES
        val posLoc = GLES20.glGetAttribLocation(prog, "aPosition")

        for (chunk in chunks) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunk.vboId)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, chunk.iboId)

            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, 0)

            if (isOutlinePass) {
                val snLoc = GLES20.glGetAttribLocation(prog, "aSmoothNormal")
                GLES20.glEnableVertexAttribArray(snLoc)
                GLES20.glVertexAttribPointer(snLoc, 3, GLES20.GL_FLOAT, false, stride, 44)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, chunk.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                GLES20.glDisableVertexAttribArray(posLoc)
                GLES20.glDisableVertexAttribArray(snLoc)
            } else {
                val normLoc = GLES20.glGetAttribLocation(prog, "aNormal")
                val uvLoc = GLES20.glGetAttribLocation(prog, "aTexCoord")
                val colLoc = GLES20.glGetAttribLocation(prog, "aVertexColor")
                GLES20.glEnableVertexAttribArray(normLoc)
                GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, 12)
                GLES20.glEnableVertexAttribArray(uvLoc)
                GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, stride, 24)
                GLES20.glEnableVertexAttribArray(colLoc)
                GLES20.glVertexAttribPointer(colLoc, 3, GLES20.GL_FLOAT, false, stride, 32)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, chunk.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                GLES20.glDisableVertexAttribArray(posLoc)
                GLES20.glDisableVertexAttribArray(normLoc)
                GLES20.glDisableVertexAttribArray(uvLoc)
                GLES20.glDisableVertexAttribArray(colLoc)
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun release() {
        chunks.forEach {
            GLES20.glDeleteBuffers(2, intArrayOf(it.vboId, it.iboId), 0)
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
    }
}

/**
 * Carregador de malhas Wavefront (.obj) + textura (.png) para OpenGL ES 2.0:
 * - Triangula polígonos automaticamente;
 * - Suporta v/vt/vn, v//vn, v/vt, v e índices negativos;
 * - Inverte o eixo V (1 - v) para alinhar com Bitmap/OpenGL;
 * - Centraliza X/Z, apoia Y=0 e assa normais suavizadas via SmoothNormalBaker;
 * - Divide em chunks seguros para GL_UNSIGNED_SHORT.
 */
object ObjModelLoader {

    private const val MAX_VERTICES_PER_CHUNK = 58_000

    fun loadFromAssets(
        assets: AssetManager?,
        objAssetPath: String,
        pngAssetPath: String?,
        targetHeightMeters: Float = 1.75f,
        flipV: Boolean = true
    ): LoadedObjMesh? {
        val mgr = assets ?: return null
        val interleaved = runCatching {
            mgr.open(objAssetPath).use { stream ->
                parseAndNormalizeObj(stream, targetHeightMeters, flipV)
            }
        }.getOrNull() ?: return null

        if (interleaved.isEmpty()) return null
        val chunks = uploadInChunks(interleaved)
        val texId = if (pngAssetPath != null) loadTextureFromAssets(mgr, pngAssetPath) else 0
        return LoadedObjMesh(chunks, texId)
    }

    fun parseAndNormalizeObj(
        stream: InputStream,
        targetHeightMeters: Float = 1.75f,
        flipV: Boolean = true
    ): FloatArray? {
        val rawPos = ArrayList<Float>(4096)
        val rawUv = ArrayList<Float>(4096)
        val rawNorm = ArrayList<Float>(4096)

        val outPos = ArrayList<Float>(8192)
        val outNorm = ArrayList<Float>(8192)
        val outUv = ArrayList<Float>(4096)

        BufferedReader(InputStreamReader(stream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val t = line!!.trim()
                if (t.isEmpty() || t.startsWith("#")) continue
                val parts = t.split(Regex("\\s+"))
                when (parts[0]) {
                    "v" -> if (parts.size >= 4) {
                        rawPos.add(parts[1].toFloat())
                        rawPos.add(parts[2].toFloat())
                        rawPos.add(parts[3].toFloat())
                    }
                    "vt" -> if (parts.size >= 3) {
                        rawUv.add(parts[1].toFloat())
                        val vVal = parts[2].toFloat()
                        rawUv.add(if (flipV) 1.0f - vVal else vVal)
                    }
                    "vn" -> if (parts.size >= 4) {
                        rawNorm.add(parts[1].toFloat())
                        rawNorm.add(parts[2].toFloat())
                        rawNorm.add(parts[3].toFloat())
                    }
                    "f" -> if (parts.size >= 4) {
                        for (i in 2 until parts.size - 1) {
                            emitTriangle(
                                parts[1], parts[i], parts[i + 1],
                                rawPos, rawUv, rawNorm,
                                outPos, outNorm, outUv
                            )
                        }
                    }
                }
            }
        }

        if (outPos.isEmpty()) return null
        normalizeScaleAndGround(outPos, targetHeightMeters)

        val vertCount = outPos.size / 3
        val packed = FloatArray(vertCount * StaticSceneBatcher.FLOATS_PER_VERTEX)
        for (i in 0 until vertCount) {
            val o = i * StaticSceneBatcher.FLOATS_PER_VERTEX
            packed[o + 0] = outPos[i * 3 + 0]
            packed[o + 1] = outPos[i * 3 + 1]
            packed[o + 2] = outPos[i * 3 + 2]
            packed[o + 3] = outNorm[i * 3 + 0]
            packed[o + 4] = outNorm[i * 3 + 1]
            packed[o + 5] = outNorm[i * 3 + 2]
            packed[o + 6] = outUv[i * 2 + 0]
            packed[o + 7] = outUv[i * 2 + 1]
            packed[o + 8] = 1f
            packed[o + 9] = 1f
            packed[o + 10] = 1f
        }
        return SmoothNormalBaker.bake(packed, StaticSceneBatcher.FLOATS_PER_VERTEX)
    }

    private fun emitTriangle(
        t0: String, t1: String, t2: String,
        rawPos: List<Float>, rawUv: List<Float>, rawNorm: List<Float>,
        outPos: MutableList<Float>, outNorm: MutableList<Float>, outUv: MutableList<Float>
    ) {
        val tokens = arrayOf(t0, t1, t2)
        val pCount = rawPos.size / 3
        val uvCount = rawUv.size / 2
        val nCount = rawNorm.size / 3

        val triPos = FloatArray(9)
        val triUv = FloatArray(6)
        val triNorm = FloatArray(9)
        var hasExplicitNormal = true

        for (k in 0..2) {
            val sub = tokens[k].split("/")
            val vi = resolveIndex(sub[0].toInt(), pCount)
            triPos[k * 3 + 0] = rawPos[vi * 3 + 0]
            triPos[k * 3 + 1] = rawPos[vi * 3 + 1]
            triPos[k * 3 + 2] = rawPos[vi * 3 + 2]

            if (sub.size >= 2 && sub[1].isNotEmpty() && uvCount > 0) {
                val uvi = resolveIndex(sub[1].toInt(), uvCount)
                triUv[k * 2 + 0] = rawUv[uvi * 2 + 0]
                triUv[k * 2 + 1] = rawUv[uvi * 2 + 1]
            } else {
                triUv[k * 2 + 0] = 0.5f
                triUv[k * 2 + 1] = 0.5f
            }

            if (sub.size >= 3 && sub[2].isNotEmpty() && nCount > 0) {
                val ni = resolveIndex(sub[2].toInt(), nCount)
                triNorm[k * 3 + 0] = rawNorm[ni * 3 + 0]
                triNorm[k * 3 + 1] = rawNorm[ni * 3 + 1]
                triNorm[k * 3 + 2] = rawNorm[ni * 3 + 2]
            } else {
                hasExplicitNormal = false
            }
        }

        if (!hasExplicitNormal) {
            val ux = triPos[3] - triPos[0]; val uy = triPos[4] - triPos[1]; val uz = triPos[5] - triPos[2]
            val vx = triPos[6] - triPos[0]; val vy = triPos[7] - triPos[1]; val vz = triPos[8] - triPos[2]
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            for (k in 0..2) {
                triNorm[k * 3 + 0] = nx; triNorm[k * 3 + 1] = ny; triNorm[k * 3 + 2] = nz
            }
        }

        for (k in 0..2) {
            outPos.add(triPos[k * 3 + 0]); outPos.add(triPos[k * 3 + 1]); outPos.add(triPos[k * 3 + 2])
            outNorm.add(triNorm[k * 3 + 0]); outNorm.add(triNorm[k * 3 + 1]); outNorm.add(triNorm[k * 3 + 2])
            outUv.add(triUv[k * 2 + 0]); outUv.add(triUv[k * 2 + 1])
        }
    }

    private fun resolveIndex(idx: Int, size: Int): Int =
        (if (idx < 0) size + idx else idx - 1).coerceIn(0, size - 1)

    private fun normalizeScaleAndGround(pos: MutableList<Float>, targetHeight: Float) {
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        val count = pos.size / 3
        for (i in 0 until count) {
            val x = pos[i * 3]; val y = pos[i * 3 + 1]; val z = pos[i * 3 + 2]
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
        }
        val cx = (minX + maxX) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val height = max(0.001f, maxY - minY)
        val scale = targetHeight / height

        for (i in 0 until count) {
            pos[i * 3 + 0] = (pos[i * 3 + 0] - cx) * scale
            pos[i * 3 + 1] = (pos[i * 3 + 1] - minY) * scale
            pos[i * 3 + 2] = (pos[i * 3 + 2] - cz) * scale
        }
    }

    private fun uploadInChunks(interleaved: FloatArray): List<LoadedObjMesh.GpuChunk> {
        val stride = StaticSceneBatcher.FLOATS_PER_VERTEX
        val totalVerts = interleaved.size / stride
        val chunks = ArrayList<LoadedObjMesh.GpuChunk>()
        var startVert = 0

        while (startVert < totalVerts) {
            val count = minOf(MAX_VERTICES_PER_CHUNK - (MAX_VERTICES_PER_CHUNK % 3), totalVerts - startVert)
            val slice = interleaved.copyOfRange(startVert * stride, (startVert + count) * stride)
            val indices = ShortArray(count) { it.toShort() }

            val vBuf = ByteBuffer.allocateDirect(slice.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(slice); position(0)
            }
            val iBuf = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(indices); position(0)
            }

            val ids = IntArray(2)
            GLES20.glGenBuffers(2, ids, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, slice.size * 4, vBuf, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[1])
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, iBuf, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            chunks.add(LoadedObjMesh.GpuChunk(ids[0], ids[1], count))
            startVert += count
        }
        return chunks
    }

    fun loadTextureFromAssets(assets: AssetManager, assetPath: String): Int {
        return try {
            assets.open(assetPath).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return 0
                val isPot = (bmp.width and (bmp.width - 1)) == 0 && (bmp.height and (bmp.height - 1)) == 0
                val ids = IntArray(1)
                GLES20.glGenTextures(1, ids, 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
                GLES20.glTexParameteri(
                    GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    if (isPot) GLES20.GL_LINEAR_MIPMAP_LINEAR else GLES20.GL_LINEAR
                )
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
                if (isPot) GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
                bmp.recycle()
                ids[0]
            }
        } catch (_: Exception) {
            0
        }
    }
}
