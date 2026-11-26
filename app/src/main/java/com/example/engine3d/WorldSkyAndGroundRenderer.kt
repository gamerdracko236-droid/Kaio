package com.example.engine3d

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderiza o Domo Celeste em degradê dinâmico (dia/noite), o chão subdividido da praça/estrada,
 * o Altar da Chama Sagrada, o Portal de Região e as sombras de contato projetadas no solo.
 */
class WorldSkyAndGroundRenderer {

    private var skyVbo = 0; private var skyIbo = 0; private var skyCount = 0
    private var groundVbo = 0; private var groundIbo = 0; private var groundCount = 0
    private var shadowVbo = 0; private var shadowIbo = 0; private var shadowCount = 0
    private var monumentVbo = 0; private var monumentIbo = 0; private var monumentCount = 0
    private var ringVbo = 0; private var ringIbo = 0; private var ringCount = 0
    private var orbVbo = 0; private var orbIbo = 0; private var orbCount = 0

    private var cachedIsNight: Boolean? = null
    private var cachedSkyTop: Triple<Float, Float, Float>? = null
    private var cachedSkyBottom: Triple<Float, Float, Float>? = null
    private var cachedGround: Triple<Float, Float, Float>? = null

    fun initOrUpdate(
        skyTopRgb: Triple<Float, Float, Float>,
        skyBottomRgb: Triple<Float, Float, Float>,
        groundRgb: Triple<Float, Float, Float>,
        isNight: Boolean
    ) {
        if (skyVbo != 0 &&
            cachedIsNight == isNight &&
            cachedSkyTop == skyTopRgb &&
            cachedSkyBottom == skyBottomRgb &&
            cachedGround == groundRgb
        ) {
            return
        }
        release()
        cachedIsNight = isNight
        cachedSkyTop = skyTopRgb
        cachedSkyBottom = skyBottomRgb
        cachedGround = groundRgb

        buildSkyDome(skyTopRgb, skyBottomRgb)
        buildGroundMesh(groundRgb, isNight)
        buildShadowDisc()
        buildMonumentMesh()
        buildRingMesh()
        buildOrbMesh()
    }

    private fun buildSkyDome(
        topRgb: Triple<Float, Float, Float>,
        botRgb: Triple<Float, Float, Float>
    ) {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 0f, unlit = 1f)
        val rings = 8
        val slices = 20
        val radius = 128f
        val uv = AnimeTextureAtlas.clothUv

        for (r in 0 until rings) {
            val t0 = r.toFloat() / rings
            val t1 = (r + 1).toFloat() / rings
            val phi0 = t0 * (PI.toFloat() * 0.52f) - 0.08f
            val phi1 = t1 * (PI.toFloat() * 0.52f) - 0.08f
            val y0 = sin(phi0) * radius; val rad0 = cos(phi0) * radius
            val y1 = sin(phi1) * radius; val rad1 = cos(phi1) * radius

            val c0 = floatArrayOf(
                botRgb.first + (topRgb.first - botRgb.first) * t0,
                botRgb.second + (topRgb.second - botRgb.second) * t0,
                botRgb.third + (topRgb.third - botRgb.third) * t0
            )
            val c1 = floatArrayOf(
                botRgb.first + (topRgb.first - botRgb.first) * t1,
                botRgb.second + (topRgb.second - botRgb.second) * t1,
                botRgb.third + (topRgb.third - botRgb.third) * t1
            )

            for (s in 0 until slices) {
                val th0 = (s.toFloat() / slices) * 2f * PI.toFloat()
                val th1 = ((s + 1).toFloat() / slices) * 2f * PI.toFloat()
                val a = bb.addVertex(cos(th0) * rad0, y0, sin(th0) * rad0, 0f, 1f, 0f, uv.uMin, uv.vMin, c0[0], c0[1], c0[2])
                val b = bb.addVertex(cos(th1) * rad0, y0, sin(th1) * rad0, 0f, 1f, 0f, uv.uMax, uv.vMin, c0[0], c0[1], c0[2])
                val c = bb.addVertex(cos(th1) * rad1, y1, sin(th1) * rad1, 0f, 1f, 0f, uv.uMax, uv.vMax, c1[0], c1[1], c1[2])
                val d = bb.addVertex(cos(th0) * rad1, y1, sin(th0) * rad1, 0f, 1f, 0f, uv.uMin, uv.vMax, c1[0], c1[1], c1[2])
                bb.addTriangle(a, c, b)
                bb.addTriangle(a, d, c)
            }
        }
        val up = uploadStatic(bb)
        skyVbo = up.first; skyIbo = up.second; skyCount = up.third
    }

    private fun buildGroundMesh(groundRgb: Triple<Float, Float, Float>, isNight: Boolean) {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 1f)
        val gridN = 20
        val halfSpan = 56f
        val step = (halfSpan * 2f) / gridN
        val grassBase = if (isNight) floatArrayOf(0.18f, 0.28f, 0.24f) else floatArrayOf(0.46f, 0.64f, 0.28f)
        val roadBase = floatArrayOf(groundRgb.first * 1.05f, groundRgb.second * 1.02f, groundRgb.third * 0.96f)

        for (gx in 0 until gridN) {
            for (gz in 0 until gridN) {
                val x0 = -halfSpan + gx * step; val x1 = x0 + step
                val z0 = -halfSpan + gz * step; val z1 = z0 + step
                val mx = (x0 + x1) * 0.5f; val mz = (z0 + z1) * 0.5f
                val onMainRoad = kotlin.math.abs(mx) < 4.3f && mz in -26f..24f
                val onCrossRoad = kotlin.math.abs(mz - 2f) < 3.4f && kotlin.math.abs(mx) < 15f
                val isRoad = onMainRoad || onCrossRoad
                val col = if (isRoad) roadBase else grassBase
                val uv = if (isRoad) AnimeTextureAtlas.rockUv else AnimeTextureAtlas.barkUv
                bb.addQuad(
                    floatArrayOf(x0, 0f, z1),
                    floatArrayOf(x1, 0f, z1),
                    floatArrayOf(x1, 0f, z0),
                    floatArrayOf(x0, 0f, z0),
                    0f, 1f, 0f, uv, col, col
                )
            }
        }
        val up = uploadStatic(bb)
        groundVbo = up.first; groundIbo = up.second; groundCount = up.third
    }

    private fun buildShadowDisc() {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 0f)
        val slices = 18
        val uv = AnimeTextureAtlas.clothUv
        val c = bb.addVertex(0f, 0.02f, 0f, 0f, 1f, 0f, uv.uMin, uv.vMin, 0.05f, 0.03f, 0.04f)
        for (i in 0 until slices) {
            val a0 = (i.toFloat() / slices) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / slices) * 2f * PI.toFloat()
            val v0 = bb.addVertex(cos(a0) * 0.55f, 0.02f, sin(a0) * 0.55f, 0f, 1f, 0f, uv.uMin, uv.vMin, 0.05f, 0.03f, 0.04f)
            val v1 = bb.addVertex(cos(a1) * 0.55f, 0.02f, sin(a1) * 0.55f, 0f, 1f, 0f, uv.uMin, uv.vMin, 0.05f, 0.03f, 0.04f)
            bb.addTriangle(c, v1, v0)
        }
        val up = uploadStatic(bb)
        shadowVbo = up.first; shadowIbo = up.second; shadowCount = up.third
    }

    private fun buildMonumentMesh() {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 1f)
        val stone = floatArrayOf(0.76f, 0.72f, 0.68f)
        val flame = floatArrayOf(0.96f, 0.42f, 0.14f)
        val ember = floatArrayOf(1.0f, 0.80f, 0.26f)
        bb.addBox(0f, 0.6f, 0f, 2.3f, 1.2f, 2.0f, 0f, AnimeTextureAtlas.rockUv, stone, stone)
        bb.addPrism(0f, 1.2f, 0f, 0.88f, 0.16f, 2.5f, 8, 0f, AnimeTextureAtlas.clothUv, flame, ember)
        bb.addPrism(-0.34f, 1.45f, 0.14f, 0.42f, 0.06f, 1.55f, 6, 0.4f, AnimeTextureAtlas.clothUv, flame, ember)
        bb.addPrism(0.36f, 1.35f, -0.10f, 0.36f, 0.05f, 1.30f, 6, -0.4f, AnimeTextureAtlas.clothUv, flame, ember)
        val up = uploadStatic(bb)
        monumentVbo = up.first; monumentIbo = up.second; monumentCount = up.third
    }

    private fun buildRingMesh() {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 0f)
        val slices = 24
        val uv = AnimeTextureAtlas.clothUv
        for (i in 0 until slices) {
            val a0 = (i.toFloat() / slices) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / slices) * 2f * PI.toFloat()
            val inR = 0.82f; val outR = 1.0f
            bb.addQuad(
                floatArrayOf(cos(a0) * inR, 0.04f, sin(a0) * inR),
                floatArrayOf(cos(a0) * outR, 0.04f, sin(a0) * outR),
                floatArrayOf(cos(a1) * outR, 0.04f, sin(a1) * outR),
                floatArrayOf(cos(a1) * inR, 0.04f, sin(a1) * inR),
                0f, 1f, 0f, uv, floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 1f, 1f)
            )
        }
        val up = uploadStatic(bb)
        ringVbo = up.first; ringIbo = up.second; ringCount = up.third
    }

    private fun buildOrbMesh() {
        val bb = StaticSceneBatcher.BatchBuilder(useTexture = 0f)
        val slices = 10; val stacks = 6
        val uv = AnimeTextureAtlas.clothUv
        for (st in 0 until stacks) {
            val phi0 = ((st.toFloat() / stacks) - 0.5f) * PI.toFloat()
            val phi1 = (((st + 1).toFloat() / stacks) - 0.5f) * PI.toFloat()
            val y0 = sin(phi0); val r0 = cos(phi0)
            val y1 = sin(phi1); val r1 = cos(phi1)
            for (sl in 0 until slices) {
                val th0 = (sl.toFloat() / slices) * 2f * PI.toFloat()
                val th1 = ((sl + 1).toFloat() / slices) * 2f * PI.toFloat()
                val a = bb.addVertex(cos(th0) * r0, y0, sin(th0) * r0, cos(th0) * r0, y0, sin(th0) * r0, uv.uMin, uv.vMin, 1f, 1f, 1f)
                val b = bb.addVertex(cos(th1) * r0, y0, sin(th1) * r0, cos(th1) * r0, y0, sin(th1) * r0, uv.uMax, uv.vMin, 1f, 1f, 1f)
                val c = bb.addVertex(cos(th1) * r1, y1, sin(th1) * r1, cos(th1) * r1, y1, sin(th1) * r1, uv.uMax, uv.vMax, 1f, 1f, 1f)
                val d = bb.addVertex(cos(th0) * r1, y1, sin(th0) * r1, cos(th0) * r1, y1, sin(th0) * r1, uv.uMin, uv.vMax, 1f, 1f, 1f)
                bb.addTriangle(a, b, c); bb.addTriangle(a, c, d)
            }
        }
        val up = uploadStatic(bb)
        orbVbo = up.first; orbIbo = up.second; orbCount = up.third
    }

    private fun uploadStatic(bb: StaticSceneBatcher.BatchBuilder): Triple<Int, Int, Int> {
        val verts = bb.toFloatArray()
        val idx = bb.toShortArray()
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        val vb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(verts).position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.size * 4, vb, GLES20.GL_STATIC_DRAW)

        val ib = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(idx).position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[1])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, ib, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        return Triple(ids[0], ids[1], idx.size)
    }

    fun drawSkyAndGround(
        toonProg: Int,
        atlasTexId: Int,
        vpMatrix: FloatArray,
        heroX: Float,
        heroZ: Float
    ) {
        GLES20.glUseProgram(toonProg)
        val model = FloatArray(16)
        val mvp = FloatArray(16)

        // 1. Domo Celeste centrado no jogador
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, heroX, -4f, heroZ)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        drawBuffer(toonProg, skyVbo, skyIbo, skyCount, mvp, model, atlasTexId, 0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f)
        GLES20.glDepthMask(true)

        // 2. Chão subdividido + Monumento da Chama no altar
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        drawBuffer(toonProg, groundVbo, groundIbo, groundCount, mvp, model, atlasTexId, 1f, 0f, 0f, 0.08f, 1f, 1f, 1f, 1f)

        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, 0f, 1.96f, -24.0f)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        drawBuffer(toonProg, monumentVbo, monumentIbo, monumentCount, mvp, model, atlasTexId, 1f, 0f, 0f, 0.35f, 1f, 1f, 1f, 1f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    fun drawContactShadow(toonProg: Int, vpMatrix: FloatArray, x: Float, z: Float, actorHeightY: Float, radiusScale: Float) {
        val shrink = (1.0f - (actorHeightY * 0.18f)).coerceIn(0.45f, 1.0f) * radiusScale
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, 0.01f, z)
        Matrix.scaleM(model, 0, shrink, 1f, shrink)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        drawBuffer(toonProg, shadowVbo, shadowIbo, shadowCount, mvp, model, 0, 0f, 0f, 1f, 0f, 0.06f, 0.04f, 0.06f, 0.48f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)
    }

    fun drawRing(
        toonProg: Int,
        vpMatrix: FloatArray,
        x: Float,
        y: Float,
        z: Float,
        radius: Float,
        rotY: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.rotateM(model, 0, Math.toDegrees(rotY.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(model, 0, radius, 1f, radius)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        drawBuffer(toonProg, ringVbo, ringIbo, ringCount, mvp, model, 0, 0f, 0.35f, 1f, 0f, r, g, b, a)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    fun drawOrb(
        toonProg: Int,
        vpMatrix: FloatArray,
        x: Float,
        y: Float,
        z: Float,
        radius: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val model = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, radius, radius, radius)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        drawBuffer(toonProg, orbVbo, orbIbo, orbCount, mvp, model, 0, 0f, 0.4f, 1f, 0f, r, g, b, a)
    }

    private fun drawBuffer(
        prog: Int,
        vbo: Int,
        ibo: Int,
        count: Int,
        mvp: FloatArray,
        model: FloatArray,
        texId: Int,
        useTex: Float,
        emissive: Float,
        unlit: Float,
        rimStrength: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        if (vbo == 0 || ibo == 0 || count == 0) return
        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uMVPMatrix"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uModelMatrix"), 1, false, model, 0)
        GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uBaseColor"), r, g, b, a)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUseTexture"), useTex)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uAlphaCutoff"), 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uEmissive"), emissive)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUnlit"), unlit)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uRimStrength"), rimStrength)
        if (texId != 0 && useTex > 0.5f) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "uTexture"), 0)
        }

        val stride = StaticSceneBatcher.STRIDE_BYTES
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)

        val posLoc = GLES20.glGetAttribLocation(prog, "aPosition")
        val normLoc = GLES20.glGetAttribLocation(prog, "aNormal")
        val uvLoc = GLES20.glGetAttribLocation(prog, "aTexCoord")
        val colLoc = GLES20.glGetAttribLocation(prog, "aVertexColor")

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glEnableVertexAttribArray(normLoc)
        GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, 12)
        GLES20.glEnableVertexAttribArray(uvLoc)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, stride, 24)
        GLES20.glEnableVertexAttribArray(colLoc)
        GLES20.glVertexAttribPointer(colLoc, 3, GLES20.GL_FLOAT, false, stride, 32)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(normLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
        GLES20.glDisableVertexAttribArray(colLoc)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun release() {
        val vbos = intArrayOf(skyVbo, skyIbo, groundVbo, groundIbo, shadowVbo, shadowIbo, monumentVbo, monumentIbo, ringVbo, ringIbo, orbVbo, orbIbo)
            .filter { it != 0 }.toIntArray()
        if (vbos.isNotEmpty()) GLES20.glDeleteBuffers(vbos.size, vbos, 0)
        skyVbo = 0; skyIbo = 0; groundVbo = 0; groundIbo = 0
        shadowVbo = 0; shadowIbo = 0; monumentVbo = 0; monumentIbo = 0
        ringVbo = 0; ringIbo = 0; orbVbo = 0; orbIbo = 0
    }
}
