package com.example.engine3d

import android.opengl.GLES20
import com.example.data.model.QualityLevel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utilitário de Static Batching para OpenGL ES 2.0:
 * agrupa toda a geometria estática da cena (capim com recorte alpha, rochas facetadas,
 * árvores/mandacarus do sertão e arquitetura da vila) em pouquíssimos VBOs + IBOs únicos,
 * respeitando o limite de 65.535 índices (GL_UNSIGNED_SHORT) por lote para manter 60 FPS.
 */
class StaticSceneBatcher {

    companion object {
        const val FLOATS_PER_VERTEX = 14 // pos(3) + norm(3) + uv(2) + color(3) + smoothNorm(3)
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
        const val MAX_VERTICES_PER_BATCH = 58_000
    }

    data class GpuStaticBatch(
        val vboId: Int,
        val iboId: Int,
        val indexCount: Int,
        val alphaCutoff: Float,
        val useTexture: Float,
        val rimStrength: Float,
        val unlit: Float
    )

    class BatchBuilder(
        val alphaCutoff: Float = 0f,
        val useTexture: Float = 1f,
        val rimStrength: Float = 0.16f,
        val unlit: Float = 0f
    ) {
        private val floats = ArrayList<Float>(4096)
        private val indices = ArrayList<Short>(4096)
        val vertexCount: Int get() = floats.size / FLOATS_PER_VERTEX
        val batchedIndices: Int get() = indices.size

        fun addVertex(
            px: Float, py: Float, pz: Float,
            nx: Float, ny: Float, nz: Float,
            u: Float, v: Float,
            cr: Float, cg: Float, cb: Float,
            snx: Float = nx, sny: Float = ny, snz: Float = nz
        ): Short {
            val idx = vertexCount
            floats.add(px); floats.add(py); floats.add(pz)
            floats.add(nx); floats.add(ny); floats.add(nz)
            floats.add(u); floats.add(v)
            floats.add(cr); floats.add(cg); floats.add(cb)
            floats.add(snx); floats.add(sny); floats.add(snz)
            return idx.toShort()
        }

        fun addTriangle(a: Short, b: Short, c: Short) {
            indices.add(a); indices.add(b); indices.add(c)
        }

        fun addQuad(
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            nx: Float, ny: Float, nz: Float,
            uv: AnimeTextureAtlas.UvRect,
            cBottom: FloatArray,
            cTop: FloatArray
        ) {
            val a = addVertex(p0[0], p0[1], p0[2], nx, ny, nz, uv.uMin, uv.vMax, cBottom[0], cBottom[1], cBottom[2])
            val b = addVertex(p1[0], p1[1], p1[2], nx, ny, nz, uv.uMax, uv.vMax, cBottom[0], cBottom[1], cBottom[2])
            val c = addVertex(p2[0], p2[1], p2[2], nx, ny, nz, uv.uMax, uv.vMin, cTop[0], cTop[1], cTop[2])
            val d = addVertex(p3[0], p3[1], p3[2], nx, ny, nz, uv.uMin, uv.vMin, cTop[0], cTop[1], cTop[2])
            addTriangle(a, b, c)
            addTriangle(a, c, d)
        }

        fun addBox(
            cx: Float, cy: Float, cz: Float,
            sx: Float, sy: Float, sz: Float,
            rotY: Float = 0f,
            uv: AnimeTextureAtlas.UvRect,
            bottomCol: FloatArray,
            topCol: FloatArray
        ) {
            val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
            val cosY = cos(rotY); val sinY = sin(rotY)

            fun xform(lx: Float, ly: Float, lz: Float): FloatArray = floatArrayOf(
                cx + lx * cosY + lz * sinY,
                cy + ly,
                cz - lx * sinY + lz * cosY
            )

            fun rotN(nx: Float, ny: Float, nz: Float): FloatArray = floatArrayOf(
                nx * cosY + nz * sinY,
                ny,
                -nx * sinY + nz * cosY
            )

            val faces = listOf(
                Triple(floatArrayOf(0f, 0f, 1f), arrayOf(floatArrayOf(-hx, -hy, hz), floatArrayOf(hx, -hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(-hx, hy, hz)), false),
                Triple(floatArrayOf(0f, 0f, -1f), arrayOf(floatArrayOf(hx, -hy, -hz), floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(hx, hy, -hz)), false),
                Triple(floatArrayOf(-1f, 0f, 0f), arrayOf(floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, -hy, hz), floatArrayOf(-hx, hy, hz), floatArrayOf(-hx, hy, -hz)), false),
                Triple(floatArrayOf(1f, 0f, 0f), arrayOf(floatArrayOf(hx, -hy, hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, hy, -hz), floatArrayOf(hx, hy, hz)), false),
                Triple(floatArrayOf(0f, 1f, 0f), arrayOf(floatArrayOf(-hx, hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(hx, hy, -hz), floatArrayOf(-hx, hy, -hz)), true),
                Triple(floatArrayOf(0f, -1f, 0f), arrayOf(floatArrayOf(-hx, -hy, -hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, -hy, hz), floatArrayOf(-hx, -hy, hz)), false)
            )
            for ((norm, pts, allTop) in faces) {
                val rn = rotN(norm[0], norm[1], norm[2])
                val p0 = xform(pts[0][0], pts[0][1], pts[0][2])
                val p1 = xform(pts[1][0], pts[1][1], pts[1][2])
                val p2 = xform(pts[2][0], pts[2][1], pts[2][2])
                val p3 = xform(pts[3][0], pts[3][1], pts[3][2])
                addQuad(
                    p0, p1, p2, p3,
                    rn[0], rn[1], rn[2],
                    uv,
                    if (allTop) topCol else bottomCol,
                    topCol
                )
            }
        }

        fun addPrism(
            cx: Float, bottomY: Float, cz: Float,
            bottomRadius: Float, topRadius: Float, height: Float,
            sides: Int,
            rotY: Float = 0f,
            uv: AnimeTextureAtlas.UvRect,
            bottomCol: FloatArray,
            topCol: FloatArray
        ) {
            val step = (2f * PI.toFloat()) / sides
            for (i in 0 until sides) {
                val a0 = rotY + i * step
                val a1 = rotY + (i + 1) * step
                val c0 = cos(a0); val s0 = sin(a0)
                val c1 = cos(a1); val s1 = sin(a1)
                val midA = (a0 + a1) * 0.5f
                val nx = cos(midA); val nz = sin(midA)

                val v0 = floatArrayOf(cx + c0 * bottomRadius, bottomY, cz + s0 * bottomRadius)
                val v1 = floatArrayOf(cx + c1 * bottomRadius, bottomY, cz + s1 * bottomRadius)
                val v2 = floatArrayOf(cx + c1 * topRadius, bottomY + height, cz + s1 * topRadius)
                val v3 = floatArrayOf(cx + c0 * topRadius, bottomY + height, cz + s0 * topRadius)
                addQuad(v0, v1, v2, v3, nx, 0.25f, nz, uv, bottomCol, topCol)

                if (topRadius > 0.02f) {
                    val tc = addVertex(cx, bottomY + height, cz, 0f, 1f, 0f, (uv.uMin + uv.uMax) * 0.5f, uv.vMin, topCol[0], topCol[1], topCol[2])
                    val t0 = addVertex(v3[0], v3[1], v3[2], 0f, 1f, 0f, uv.uMin, uv.vMin, topCol[0], topCol[1], topCol[2])
                    val t1 = addVertex(v2[0], v2[1], v2[2], 0f, 1f, 0f, uv.uMax, uv.vMin, topCol[0], topCol[1], topCol[2])
                    addTriangle(tc, t0, t1)
                }
            }
        }

        fun toFloatArray(): FloatArray = floats.toFloatArray()
        fun toShortArray(): ShortArray = indices.toShortArray()

        fun uploadToGpu(): GpuStaticBatch? {
            if (indices.isEmpty()) return null
            val vertData = toFloatArray()
            val idxData = toShortArray()

            val ids = IntArray(2)
            GLES20.glGenBuffers(2, ids, 0)
            val vboId = ids[0]
            val iboId = ids[1]

            val vBuf = ByteBuffer.allocateDirect(vertData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            vBuf.put(vertData).position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertData.size * 4, vBuf, GLES20.GL_STATIC_DRAW)

            val iBuf = ByteBuffer.allocateDirect(idxData.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            iBuf.put(idxData).position(0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idxData.size * 2, iBuf, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            return GpuStaticBatch(
                vboId = vboId,
                iboId = iboId,
                indexCount = idxData.size,
                alphaCutoff = alphaCutoff,
                useTexture = useTexture,
                rimStrength = rimStrength,
                unlit = unlit
            )
        }
    }

    private val batches = mutableListOf<GpuStaticBatch>()
    var totalBatchedPrimitives: Int = 0
        private set

    fun buildStaticSertaoProps(regionSeed: Int, quality: QualityLevel, isNight: Boolean) {
        release()
        val rockBuilders = mutableListOf(BatchBuilder(alphaCutoff = 0f, useTexture = 1f, rimStrength = 0.14f))
        val grassBuilders = mutableListOf(BatchBuilder(alphaCutoff = 0.45f, useTexture = 1f, rimStrength = 0.05f))
        val treeBuilders = mutableListOf(BatchBuilder(alphaCutoff = 0f, useTexture = 1f, rimStrength = 0.20f))
        val villageBuilders = mutableListOf(BatchBuilder(alphaCutoff = 0f, useTexture = 1f, rimStrength = 0.18f))

        fun <T : BatchBuilder> ensureCapacity(list: MutableList<T>, neededVerts: Int, create: () -> T): T {
            val current = list.last()
            if (current.vertexCount + neededVerts > MAX_VERTICES_PER_BATCH) {
                val next = create()
                list.add(next)
                return next
            }
            return current
        }

        // 1. VILA DAS ALMAS / ARQUITETURA SERTANEJA (Casas, escadaria do santuário, poço, faixas)
        val wallTint = if (isNight) floatArrayOf(0.56f, 0.52f, 0.58f) else floatArrayOf(0.95f, 0.89f, 0.78f)
        val wallShade = if (isNight) floatArrayOf(0.36f, 0.34f, 0.42f) else floatArrayOf(0.76f, 0.67f, 0.56f)
        val roofTop = if (isNight) floatArrayOf(0.42f, 0.24f, 0.22f) else floatArrayOf(0.74f, 0.34f, 0.22f)
        val roofBot = floatArrayOf(0.48f, 0.20f, 0.14f)
        val stoneTop = if (isNight) floatArrayOf(0.52f, 0.55f, 0.64f) else floatArrayOf(0.78f, 0.75f, 0.71f)
        val stoneBot = floatArrayOf(0.46f, 0.43f, 0.40f)

        val vb = ensureCapacity(villageBuilders, 400) { BatchBuilder(alphaCutoff = 0f, useTexture = 1f) }
        // Escadaria de pedra larga para o altar da chama (Anexo 1)
        for (step in 0 until 7) {
            val sy = 0.28f * (step + 1)
            val sz = -15.5f - step * 0.72f
            vb.addBox(0f, sy * 0.5f, sz, 11.4f, sy, 0.78f, 0f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
        }
        // Terraço superior + muros laterais + Poço
        vb.addBox(0f, 0.98f, -23.5f, 18.0f, 1.96f, 8.2f, 0f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
        vb.addBox(-12.5f, 1.15f, -20.5f, 10.0f, 2.3f, 4.5f, 0f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
        vb.addBox(12.5f, 1.15f, -20.5f, 10.0f, 2.3f, 4.5f, 0f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
        vb.addPrism(-5.5f, 0f, 4.5f, 1.05f, 1.0f, 1.15f, 8, 0f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
        vb.addPrism(-5.5f, 2.15f, 4.5f, 1.35f, 0.08f, 0.95f, 8, 0f, AnimeTextureAtlas.clothUv, roofBot, roofTop)
        vb.addBox(-6.2f, 1.45f, 4.5f, 0.16f, 1.7f, 0.16f, 0f, AnimeTextureAtlas.barkUv, roofBot, wallTint)
        vb.addBox(-4.8f, 1.45f, 4.5f, 0.16f, 1.7f, 0.16f, 0f, AnimeTextureAtlas.barkUv, roofBot, wallTint)

        fun addVillageHouse(bx: Float, bz: Float, rotY: Float, w: Float, h: Float, d: Float) {
            vb.addBox(bx, h * 0.5f, bz, w, h, d, rotY, AnimeTextureAtlas.clothUv, wallShade, wallTint)
            vb.addPrism(bx, h, bz, w * 0.68f, w * 0.36f, 1.6f, 4, rotY + 0.785f, AnimeTextureAtlas.clothUv, roofBot, roofTop)
            val fx = sin(rotY); val fz = cos(rotY)
            val rx = cos(rotY); val rz = -sin(rotY)
            val ax = bx + fx * (d * 0.5f + 0.62f)
            val az = bz + fz * (d * 0.5f + 0.62f)
            vb.addBox(ax, h * 0.62f, az, w + 0.25f, 0.16f, 1.35f, rotY, AnimeTextureAtlas.clothUv, roofBot, roofTop)
            vb.addBox(ax + rx * w * 0.42f, h * 0.31f, az + rz * w * 0.42f, 0.18f, h * 0.62f, 0.18f, rotY, AnimeTextureAtlas.barkUv, wallShade, wallTint)
            vb.addBox(ax - rx * w * 0.42f, h * 0.31f, az - rz * w * 0.42f, 0.18f, h * 0.62f, 0.18f, rotY, AnimeTextureAtlas.barkUv, wallShade, wallTint)
        }

        addVillageHouse(-9.8f, -3.5f, 1.50f, 5.2f, 3.8f, 4.4f)
        addVillageHouse(9.9f, -4.5f, -1.52f, 5.4f, 4.0f, 4.4f)
        addVillageHouse(-10.4f, -11.5f, 1.48f, 4.8f, 3.6f, 4.0f)
        if (quality != QualityLevel.BAIXA) {
            addVillageHouse(10.3f, -12.0f, -1.48f, 4.8f, 3.7f, 4.2f)
            addVillageHouse(-11.6f, 9.0f, 0.85f, 4.6f, 3.4f, 3.8f)
        }

        // Estandarte / Faixa atravessando a rua (Anexo 4)
        vb.addBox(-5.4f, 2.5f, -8.5f, 0.22f, 5.0f, 0.22f, 0f, AnimeTextureAtlas.barkUv, roofBot, wallTint)
        vb.addBox(5.4f, 2.5f, -8.5f, 0.22f, 5.0f, 0.22f, 0f, AnimeTextureAtlas.barkUv, roofBot, wallTint)
        vb.addQuad(
            floatArrayOf(-5.3f, 3.85f, -8.5f),
            floatArrayOf(5.3f, 3.85f, -8.5f),
            floatArrayOf(5.3f, 5.05f, -8.5f),
            floatArrayOf(-5.3f, 5.05f, -8.5f),
            0f, 0f, 1f, AnimeTextureAtlas.bannerUv, wallShade, wallTint
        )
        totalBatchedPrimitives += 14

        // 2. PENHASCOS E ROCHAS FACETADAS DO SERTÃO (1 único VBO+IBO)
        val rb = ensureCapacity(rockBuilders, 1200) { BatchBuilder(alphaCutoff = 0f, useTexture = 1f) }
        val cliffCount = if (quality == QualityLevel.BAIXA) 10 else 16
        for (i in 0 until cliffCount) {
            val ang = (i.toFloat() / cliffCount) * (2f * PI.toFloat()) + regionSeed * 0.29f
            val dist = 42f + (i % 3) * 6f
            val cx = cos(ang) * dist
            val cz = sin(ang) * dist
            val h = 11f + ((i * 7 + regionSeed * 3) % 7) * 2.8f
            val rad = 6.2f + (i % 4) * 1.5f
            rb.addPrism(cx, 0f, cz, rad, rad * 0.58f, h, 7, ang, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
            totalBatchedPrimitives++
        }

        val rockCount = when (quality) {
            QualityLevel.BAIXA -> 16
            QualityLevel.MEDIA -> 28
            QualityLevel.ALTA -> 42
        }
        for (i in 0 until rockCount) {
            val rxRaw = (((i * 61 + regionSeed * 23) % 100) / 100f - 0.5f) * 52f
            val rz = (((i * 89 + regionSeed * 41) % 100) / 100f - 0.5f) * 50f
            val rx = if (abs(rxRaw) < 4.6f) (if (rxRaw < 0f) -5.5f - (i % 4) else 5.5f + (i % 4)) else rxRaw
            val rw = 0.65f + (i % 4) * 0.35f
            val rh = 0.55f + (i % 3) * 0.38f
            rb.addPrism(rx, 0f, rz, rw, rw * 0.55f, rh, 6, i * 0.85f, AnimeTextureAtlas.rockUv, stoneBot, stoneTop)
            totalBatchedPrimitives++
        }

        // 3. ÁRVORES ESTILIZADAS E MANDACARUS DO SERTÃO (1 único VBO+IBO)
        val tb = ensureCapacity(treeBuilders, 1600) { BatchBuilder(alphaCutoff = 0f, useTexture = 1f) }
        val trunkBot = floatArrayOf(0.40f, 0.26f, 0.16f)
        val trunkTop = floatArrayOf(0.64f, 0.44f, 0.28f)
        val leafBot = if (isNight) floatArrayOf(0.16f, 0.34f, 0.22f) else floatArrayOf(0.24f, 0.56f, 0.22f)
        val leafTop = if (isNight) floatArrayOf(0.28f, 0.54f, 0.36f) else floatArrayOf(0.48f, 0.84f, 0.34f)
        val treeCount = when (quality) {
            QualityLevel.BAIXA -> 14
            QualityLevel.MEDIA -> 24
            QualityLevel.ALTA -> 36
        }
        for (i in 0 until treeCount) {
            val ang = (i.toFloat() / treeCount) * 2f * PI.toFloat() + regionSeed * 0.41f
            val dist = 16.5f + (i % 5) * 4.4f
            val tx = cos(ang) * dist
            val tz = sin(ang) * dist
            if (abs(tx) < 5.5f && tz > -26f && tz < 18f) continue

            if (i % 3 == 0) {
                // Mandacaru estilizado em cel shading
                val ch = 2.6f + (i % 3) * 0.65f
                tb.addPrism(tx, 0f, tz, 0.34f, 0.28f, ch, 6, i * 0.5f, AnimeTextureAtlas.barkUv, leafBot, leafTop)
                tb.addBox(tx + 0.36f, ch * 0.52f, tz, 0.52f, 0.22f, 0.22f, ang, AnimeTextureAtlas.barkUv, leafBot, leafTop)
                tb.addBox(tx + 0.58f, ch * 0.72f, tz, 0.22f, 0.75f, 0.22f, ang, AnimeTextureAtlas.barkUv, leafBot, leafTop)
            } else {
                // Árvore do Sertão com copa dupla anime
                val th = 2.5f + (i % 3) * 0.55f
                tb.addPrism(tx, 0f, tz, 0.32f, 0.20f, th, 6, ang, AnimeTextureAtlas.barkUv, trunkBot, trunkTop)
                tb.addPrism(tx, th - 0.3f, tz, 1.75f + (i % 2) * 0.35f, 0.55f, 1.55f, 7, ang, AnimeTextureAtlas.barkUv, leafBot, leafTop)
                tb.addPrism(tx, th + 0.9f, tz, 1.25f, 0.12f, 1.25f, 7, ang + 0.4f, AnimeTextureAtlas.barkUv, leafBot, leafTop)
            }
            totalBatchedPrimitives++
        }

        // 4. TUFOS DE CAPIM CRUZADOS COM ALPHA CUTOUT E SOMBREAMENTO NA RAIZ (1 único VBO+IBO)
        val grassCount = when (quality) {
            QualityLevel.BAIXA -> 120
            QualityLevel.MEDIA -> 240
            QualityLevel.ALTA -> 380
        }
        val rootCol = if (isNight) floatArrayOf(0.22f, 0.34f, 0.26f) else floatArrayOf(0.36f, 0.56f, 0.24f)
        val tipCol = if (isNight) floatArrayOf(0.45f, 0.68f, 0.50f) else floatArrayOf(0.76f, 0.96f, 0.44f)
        for (i in 0 until grassCount) {
            val gb = ensureCapacity(grassBuilders, 16) { BatchBuilder(alphaCutoff = 0.45f, useTexture = 1f, rimStrength = 0.05f) }
            val gxRaw = (((i * 73 + regionSeed * 19) % 1000) / 1000f - 0.5f) * 64f
            val gz = (((i * 137 + regionSeed * 37) % 1000) / 1000f - 0.5f) * 64f
            if (abs(gxRaw) < 4.1f && gz > -26f && gz < 22f) continue
            val bladeW = 0.48f + (i % 3) * 0.14f
            val bladeH = 0.48f + (i % 4) * 0.14f
            val a1 = i * 0.77f
            val a2 = a1 + 1.57f
            for (ang in floatArrayOf(a1, a2)) {
                val dx = cos(ang) * bladeW
                val dz = sin(ang) * bladeW
                val nx = -sin(ang)
                val nz = cos(ang)
                gb.addQuad(
                    floatArrayOf(gxRaw - dx, 0f, gz - dz),
                    floatArrayOf(gxRaw + dx, 0f, gz + dz),
                    floatArrayOf(gxRaw + dx, bladeH, gz + dz),
                    floatArrayOf(gxRaw - dx, bladeH, gz - dz),
                    nx, 0.35f, nz, AnimeTextureAtlas.grassUv, rootCol, tipCol
                )
            }
            totalBatchedPrimitives++
        }

        (villageBuilders + rockBuilders + treeBuilders + grassBuilders).forEach { builder ->
            builder.uploadToGpu()?.let { batches.add(it) }
        }
    }

    fun drawAll(
        toonProg: Int,
        atlasTexId: Int,
        vpMatrix: FloatArray,
        identityModelMatrix: FloatArray
    ) {
        if (batches.isEmpty()) return
        GLES20.glUseProgram(toonProg)
        GLES20.glDisable(GLES20.GL_CULL_FACE) // Capim e faixas visíveis dos dois lados
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlasTexId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(toonProg, "uTexture"), 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(toonProg, "uMVPMatrix"), 1, false, vpMatrix, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(toonProg, "uModelMatrix"), 1, false, identityModelMatrix, 0)
        GLES20.glUniform4f(GLES20.glGetUniformLocation(toonProg, "uBaseColor"), 1f, 1f, 1f, 1f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uEmissive"), 0f)

        val posLoc = GLES20.glGetAttribLocation(toonProg, "aPosition")
        val normLoc = GLES20.glGetAttribLocation(toonProg, "aNormal")
        val uvLoc = GLES20.glGetAttribLocation(toonProg, "aTexCoord")
        val colLoc = GLES20.glGetAttribLocation(toonProg, "aVertexColor")
        val cutoffLoc = GLES20.glGetUniformLocation(toonProg, "uAlphaCutoff")
        val useTexLoc = GLES20.glGetUniformLocation(toonProg, "uUseTexture")
        val rimLoc = GLES20.glGetUniformLocation(toonProg, "uRimStrength")
        val unlitLoc = GLES20.glGetUniformLocation(toonProg, "uUnlit")

        for (b in batches) {
            GLES20.glUniform1f(cutoffLoc, b.alphaCutoff)
            GLES20.glUniform1f(useTexLoc, b.useTexture)
            GLES20.glUniform1f(rimLoc, b.rimStrength)
            GLES20.glUniform1f(unlitLoc, b.unlit)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, b.vboId)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, b.iboId)

            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0)
            GLES20.glEnableVertexAttribArray(normLoc)
            GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 12)
            GLES20.glEnableVertexAttribArray(uvLoc)
            GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, 24)
            GLES20.glEnableVertexAttribArray(colLoc)
            GLES20.glVertexAttribPointer(colLoc, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 32)

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, b.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
        }

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(normLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
        GLES20.glDisableVertexAttribArray(colLoc)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }

    fun batchCount(): Int = batches.size

    fun release() {
        batches.forEach {
            val bufs = intArrayOf(it.vboId, it.iboId)
            GLES20.glDeleteBuffers(2, bufs, 0)
        }
        batches.clear()
        totalBatchedPrimitives = 0
    }
}
