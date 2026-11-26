package com.example.engine3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Malha humanoide anime articulada para estilo NARUTO STORM:
 * - Partes independentes (Cabeça com faixa UV de olhos, Cabelo pontudo, Tronco, Braços, Pernas,
 *   Manto/Lenço traseiro e Arma) para animação fluida a 60 FPS;
 * - Normais suavizadas assadas com SmoothNormalBaker em cada parte para contorno GL_FRONT sem frestas.
 */
class HumanoidAnimeMesh(val profile: CharacterVisualProfile) {

    enum class BodyPart {
        TORSO, HEAD, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, BACK_CLOTH, WEAPON
    }

    data class GpuPartBuffer(
        val vboId: Int,
        val iboId: Int,
        val indexCount: Int,
        val tintColor: FloatArray,
        val useTexture: Float = 0f,
        val emissive: Float = 0f
    )

    val parts = LinkedHashMap<BodyPart, GpuPartBuffer>()

    fun buildAllParts() {
        release()
        parts[BodyPart.TORSO] = upload(buildTorso(), profile.primaryOutfitColor, useTex = 1f)
        parts[BodyPart.HEAD] = upload(buildHeadWithEyesAndHair(), floatArrayOf(1f, 1f, 1f, 1f), useTex = 1f)
        parts[BodyPart.LEFT_ARM] = upload(buildLimb(0.088f, 0.50f * profile.armLengthScale, profile.primaryOutfitColor, profile.skinColor), floatArrayOf(1f, 1f, 1f, 1f))
        parts[BodyPart.RIGHT_ARM] = upload(buildLimb(0.088f, 0.50f * profile.armLengthScale, profile.primaryOutfitColor, profile.skinColor), floatArrayOf(1f, 1f, 1f, 1f))
        parts[BodyPart.LEFT_LEG] = upload(buildLimb(0.115f, 0.78f * profile.legLengthScale, profile.pantsColor, profile.bootsColor), floatArrayOf(1f, 1f, 1f, 1f))
        parts[BodyPart.RIGHT_LEG] = upload(buildLimb(0.115f, 0.78f * profile.legLengthScale, profile.pantsColor, profile.bootsColor), floatArrayOf(1f, 1f, 1f, 1f))

        if (profile.backGarment != BackGarmentType.NONE) {
            val garmentTint = if (profile.backGarment == BackGarmentType.FLOWING_SCARF) profile.accentColor else profile.secondaryOutfitColor
            parts[BodyPart.BACK_CLOTH] = upload(buildBackGarment(), garmentTint, useTex = 1f)
        }
        if (profile.weaponType != WeaponModelType.NONE) {
            parts[BodyPart.WEAPON] = upload(buildWeaponMesh(), profile.weaponColor, emissive = 0.20f)
        }
    }

    private fun buildTorso(): Pair<FloatArray, ShortArray> {
        val m = RawMeshAccumulator()
        val w = 0.36f * profile.shoulderWidthScale
        // Peitoral estilizado em V
        m.addTaperedBox(0f, 0.28f, 0f, w * 0.76f, w, 0.54f, 0.24f, profile.primaryOutfitColor)
        // Gola alta estilo anime ninja / gibão
        m.addCylinder(0f, 0.54f, 0f, 0.145f, 0.135f, 0.10f, 10, profile.secondaryOutfitColor)
        // Cinturão dourado + placa frontal
        m.addBox(0f, 0.06f, 0f, w * 0.84f, 0.10f, 0.27f, profile.secondaryOutfitColor)
        m.addBox(0f, 0.06f, 0.145f, 0.11f, 0.08f, 0.03f, profile.accentColor)
        // Bainha presa nas costas se usar espada
        if (profile.weaponType == WeaponModelType.MACHETE_SWORD || profile.weaponType == WeaponModelType.DARK_SABER) {
            m.addBox(-0.08f, 0.26f, -0.15f, 0.06f, 0.66f, 0.06f, profile.bootsColor)
        }
        return m.bakeSmooth()
    }

    private fun buildHeadWithEyesAndHair(): Pair<FloatArray, ShortArray> {
        val m = RawMeshAccumulator()
        val eyeUv = if (profile.demonEyes) AnimeTextureAtlas.demonEyeUv else AnimeTextureAtlas.heroEyeUv

        // Pescoço
        m.addCylinder(0f, 0.04f, 0f, 0.075f, 0.068f, 0.12f, 8, profile.skinColor)
        // Rosto anime (esfera levemente afilada para o queixo)
        m.addSphere(0f, 0.22f, 0f, 0.165f, 12, 8, profile.skinColor)
        // Queixo suave frontal
        m.addTaperedBox(0f, 0.115f, 0.03f, 0.11f, 0.18f, 0.09f, 0.15f, profile.skinColor)

        // Faixa UV dos Olhos Anime Expressivos no plano frontal (+Z)
        m.addTexturedQuad(
            p0 = floatArrayOf(-0.115f, 0.165f, 0.162f),
            p1 = floatArrayOf(0.115f, 0.165f, 0.162f),
            p2 = floatArrayOf(0.115f, 0.258f, 0.162f),
            p3 = floatArrayOf(-0.115f, 0.258f, 0.162f),
            nx = 0f, ny = 0f, nz = 1f,
            uv = eyeUv,
            color = floatArrayOf(1f, 1f, 1f, 1f)
        )

        // Cabelo base + pontas espetadas estilo Storm (Anexos 1, 3 e 4)
        m.addSphere(0f, 0.27f, -0.02f, 0.175f, 10, 6, profile.hairColor)
        if (profile.spikyHair) {
            val spikes = listOf(
                floatArrayOf(-0.13f, 0.37f, -0.02f),
                floatArrayOf(0.13f, 0.37f, -0.02f),
                floatArrayOf(0f, 0.42f, -0.05f),
                floatArrayOf(-0.08f, 0.35f, 0.10f),
                floatArrayOf(0.08f, 0.35f, 0.10f),
                floatArrayOf(-0.15f, 0.29f, -0.10f),
                floatArrayOf(0.15f, 0.29f, -0.10f),
                floatArrayOf(0f, 0.34f, -0.15f)
            )
            for (sp in spikes) {
                m.addCone(sp[0], sp[1], sp[2], 0.058f, 0.165f, 6, profile.hairColor)
            }
        }

        // Chapéus, bandana ou elmo de chifres
        when (profile.headgear) {
            HeadgearType.LEATHER_HAT -> {
                m.addCylinder(0f, 0.34f, 0f, 0.34f, 0.34f, 0.032f, 14, profile.secondaryOutfitColor)
                m.addCone(0f, 0.36f, 0f, 0.17f, 0.16f, 12, profile.secondaryOutfitColor)
            }
            HeadgearType.CANGACEIRO_CROWN -> {
                m.addCylinder(0f, 0.34f, 0f, 0.35f, 0.32f, 0.038f, 14, profile.secondaryOutfitColor)
                m.addBox(0f, 0.39f, 0.11f, 0.20f, 0.09f, 0.05f, profile.accentColor)
            }
            HeadgearType.SHINOBI_BANDANA -> {
                m.addCylinder(0f, 0.285f, 0f, 0.175f, 0.175f, 0.058f, 12, profile.secondaryOutfitColor)
                m.addBox(0f, 0.285f, 0.172f, 0.12f, 0.046f, 0.02f, floatArrayOf(0.78f, 0.82f, 0.88f, 1f))
            }
            HeadgearType.HORNED_HELM -> {
                m.addCylinder(0f, 0.30f, 0f, 0.18f, 0.16f, 0.08f, 10, profile.secondaryOutfitColor)
                m.addCone(-0.14f, 0.36f, 0.02f, 0.045f, 0.22f, 6, profile.accentColor)
                m.addCone(0.14f, 0.36f, 0.02f, 0.045f, 0.22f, 6, profile.accentColor)
            }
            HeadgearType.NONE -> Unit
        }
        return m.bakeSmooth()
    }

    private fun buildLimb(
        radius: Float,
        length: Float,
        upperColor: FloatArray,
        lowerColor: FloatArray
    ): Pair<FloatArray, ShortArray> {
        val m = RawMeshAccumulator()
        m.addCylinder(0f, -length * 0.52f, 0f, radius * 0.92f, radius, length * 0.52f, 10, upperColor)
        m.addCylinder(0f, -length, 0f, radius * 0.85f, radius * 0.92f, length * 0.48f, 10, lowerColor)
        return m.bakeSmooth()
    }

    private fun buildBackGarment(): Pair<FloatArray, ShortArray> {
        val m = RawMeshAccumulator()
        when (profile.backGarment) {
            BackGarmentType.FLOWING_SCARF -> {
                m.addTaperedBox(0f, -0.28f, -0.15f, 0.14f, 0.24f, 0.58f, 0.035f, profile.accentColor)
                m.addTaperedBox(0.06f, -0.44f, -0.17f, 0.08f, 0.14f, 0.34f, 0.028f, profile.accentColor)
            }
            BackGarmentType.PONCHO_CAPE -> {
                m.addTaperedBox(0f, -0.46f, -0.16f, 0.56f, 0.36f, 0.78f, 0.04f, profile.secondaryOutfitColor)
            }
            BackGarmentType.WINGED_CLOAK -> {
                m.addTaperedBox(-0.28f, -0.25f, -0.18f, 0.42f, 0.16f, 0.62f, 0.03f, profile.secondaryOutfitColor)
                m.addTaperedBox(0.28f, -0.25f, -0.18f, 0.42f, 0.16f, 0.62f, 0.03f, profile.secondaryOutfitColor)
            }
            BackGarmentType.NONE -> Unit
        }
        return m.bakeSmooth()
    }

    private fun buildWeaponMesh(): Pair<FloatArray, ShortArray> {
        val m = RawMeshAccumulator()
        when (profile.weaponType) {
            WeaponModelType.HEAVY_CLUB -> {
                m.addCylinder(0f, 0f, 0.32f, 0.04f, 0.12f, 0.85f, 8, profile.weaponColor)
                m.addBox(0f, 0.02f, 0.68f, 0.24f, 0.24f, 0.32f, profile.accentColor)
            }
            WeaponModelType.DUAL_BLADES -> {
                m.addBox(0f, 0f, 0.02f, 0.04f, 0.04f, 0.22f, profile.bootsColor)
                m.addBox(0f, 0.02f, 0.48f, 0.045f, 0.08f, 0.88f, profile.weaponColor)
                m.addBox(-0.04f, -0.04f, 0.40f, 0.035f, 0.06f, 0.72f, profile.accentColor)
            }
            else -> {
                // Cabo + guarda dourada + lâmina longa estilo Storm
                m.addBox(0f, 0f, 0.04f, 0.036f, 0.036f, 0.20f, profile.bootsColor)
                m.addBox(0f, 0f, 0.15f, 0.15f, 0.048f, 0.03f, floatArrayOf(0.92f, 0.74f, 0.22f, 1f))
                m.addTaperedBox(0f, 0.03f, 0.56f, 0.022f, 0.048f, 0.075f, 0.82f, profile.weaponColor)
            }
        }
        return m.bakeSmooth()
    }

    private fun upload(
        data: Pair<FloatArray, ShortArray>,
        tint: FloatArray,
        useTex: Float = 0f,
        emissive: Float = 0f
    ): GpuPartBuffer {
        val (verts, idx) = data
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

        return GpuPartBuffer(ids[0], ids[1], idx.size, tint, useTex, emissive)
    }

    fun release() {
        parts.values.forEach {
            val bufs = intArrayOf(it.vboId, it.iboId)
            GLES20.glDeleteBuffers(2, bufs, 0)
        }
        parts.clear()
    }

    /** Acumulador interno de primitivas com 14 floats/vértice + baking automático de SmoothNormalBaker. */
    private class RawMeshAccumulator {
        private val batch = StaticSceneBatcher.BatchBuilder(alphaCutoff = 0f, useTexture = 0f)
        private val clothUv = AnimeTextureAtlas.UvRect(0.62f, 0.14f, 0.88f, 0.38f)

        fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, col: FloatArray) {
            batch.addBox(cx, cy, cz, sx, sy, sz, 0f, clothUv, col, col)
        }

        fun addTaperedBox(
            cx: Float, cy: Float, cz: Float,
            botW: Float, topW: Float, h: Float, d: Float,
            col: FloatArray
        ) {
            val hbw = botW * 0.5f; val htw = topW * 0.5f
            val hy = h * 0.5f; val hd = d * 0.5f
            val faces = listOf(
                Pair(floatArrayOf(0f, 0f, 1f), arrayOf(floatArrayOf(-hbw, -hy, hd), floatArrayOf(hbw, -hy, hd), floatArrayOf(htw, hy, hd), floatArrayOf(-htw, hy, hd))),
                Pair(floatArrayOf(0f, 0f, -1f), arrayOf(floatArrayOf(hbw, -hy, -hd), floatArrayOf(-hbw, -hy, -hd), floatArrayOf(-htw, hy, -hd), floatArrayOf(htw, hy, -hd))),
                Pair(floatArrayOf(-1f, 0f, 0f), arrayOf(floatArrayOf(-hbw, -hy, -hd), floatArrayOf(-hbw, -hy, hd), floatArrayOf(-htw, hy, hd), floatArrayOf(-htw, hy, -hd))),
                Pair(floatArrayOf(1f, 0f, 0f), arrayOf(floatArrayOf(hbw, -hy, hd), floatArrayOf(hbw, -hy, -hd), floatArrayOf(htw, hy, -hd), floatArrayOf(htw, hy, hd))),
                Pair(floatArrayOf(0f, 1f, 0f), arrayOf(floatArrayOf(-htw, hy, hd), floatArrayOf(htw, hy, hd), floatArrayOf(htw, hy, -hd), floatArrayOf(-htw, hy, -hd))),
                Pair(floatArrayOf(0f, -1f, 0f), arrayOf(floatArrayOf(-hbw, -hy, -hd), floatArrayOf(hbw, -hy, -hd), floatArrayOf(hbw, -hy, hd), floatArrayOf(-hbw, -hy, hd)))
            )
            for ((n, pts) in faces) {
                batch.addQuad(
                    floatArrayOf(cx + pts[0][0], cy + pts[0][1], cz + pts[0][2]),
                    floatArrayOf(cx + pts[1][0], cy + pts[1][1], cz + pts[1][2]),
                    floatArrayOf(cx + pts[2][0], cy + pts[2][1], cz + pts[2][2]),
                    floatArrayOf(cx + pts[3][0], cy + pts[3][1], cz + pts[3][2]),
                    n[0], n[1], n[2], clothUv, col, col
                )
            }
        }

        fun addCylinder(
            cx: Float, bottomY: Float, cz: Float,
            botR: Float, topR: Float, h: Float,
            sides: Int,
            col: FloatArray
        ) {
            batch.addPrism(cx, bottomY, cz, botR, topR, h, sides, 0f, clothUv, col, col)
        }

        fun addCone(cx: Float, bottomY: Float, cz: Float, r: Float, h: Float, sides: Int, col: FloatArray) {
            batch.addPrism(cx, bottomY, cz, r, 0.01f, h, sides, 0f, clothUv, col, col)
        }

        fun addSphere(cx: Float, cy: Float, cz: Float, radius: Float, slices: Int, stacks: Int, col: FloatArray) {
            for (st in 0 until stacks) {
                val v0 = st.toFloat() / stacks
                val v1 = (st + 1).toFloat() / stacks
                val phi0 = (v0 - 0.5f) * PI.toFloat()
                val phi1 = (v1 - 0.5f) * PI.toFloat()
                val y0 = sin(phi0) * radius; val r0 = cos(phi0) * radius
                val y1 = sin(phi1) * radius; val r1 = cos(phi1) * radius
                for (sl in 0 until slices) {
                    val th0 = (sl.toFloat() / slices) * 2f * PI.toFloat()
                    val th1 = ((sl + 1).toFloat() / slices) * 2f * PI.toFloat()
                    val x00 = cos(th0) * r0; val z00 = sin(th0) * r0
                    val x10 = cos(th1) * r0; val z10 = sin(th1) * r0
                    val x11 = cos(th1) * r1; val z11 = sin(th1) * r1
                    val x01 = cos(th0) * r1; val z01 = sin(th0) * r1

                    val a = batch.addVertex(cx + x00, cy + y0, cz + z00, x00 / radius, y0 / radius, z00 / radius, clothUv.uMin, clothUv.vMin, col[0], col[1], col[2])
                    val b = batch.addVertex(cx + x10, cy + y0, cz + z10, x10 / radius, y0 / radius, z10 / radius, clothUv.uMax, clothUv.vMin, col[0], col[1], col[2])
                    val c = batch.addVertex(cx + x11, cy + y1, cz + z11, x11 / radius, y1 / radius, z11 / radius, clothUv.uMax, clothUv.vMax, col[0], col[1], col[2])
                    val d = batch.addVertex(cx + x01, cy + y1, cz + z01, x01 / radius, y1 / radius, z01 / radius, clothUv.uMin, clothUv.vMax, col[0], col[1], col[2])
                    batch.addTriangle(a, b, c)
                    batch.addTriangle(a, c, d)
                }
            }
        }

        fun addTexturedQuad(
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            nx: Float, ny: Float, nz: Float,
            uv: AnimeTextureAtlas.UvRect,
            color: FloatArray
        ) {
            batch.addQuad(p0, p1, p2, p3, nx, ny, nz, uv, color, color)
        }

        fun bakeSmooth(): Pair<FloatArray, ShortArray> {
            val baked = SmoothNormalBaker.bake(batch.toFloatArray(), StaticSceneBatcher.FLOATS_PER_VERTEX)
            return baked to batch.toShortArray()
        }
    }
}
