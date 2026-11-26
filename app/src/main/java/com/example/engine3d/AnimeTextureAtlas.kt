package com.example.engine3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils
import com.example.data.model.QualityLevel
import kotlin.math.abs
import kotlin.math.sin

/**
 * Atlas procedural único Power-of-Two (512x512 ou 256x256 conforme QualityLevel)
 * com Mipmaps em hardware gerados uma única vez no onSurfaceCreated().
 *
 * Grade lógica de quadrantes (UVs normalizados 0..1):
 *   (0,0) Q0 [0.00..0.50, 0.00..0.50] : Olhos Anime Expressivos (Hero + Demon no sub-quadrante)
 *   (1,0) Q1 [0.50..1.00, 0.00..0.50] : Tecido Dobrado / Gibão de Couro & Costura Dourada
 *   (0,1) Q2 [0.00..0.50, 0.50..1.00] : Tufos de Capim do Sertão (Recorte Alpha Cutout)
 *   (1,1) Q3 [0.50..1.00, 0.50..1.00] : Pedra Rachada / Tronco de Mandacaru & Adobe
 */
object AnimeTextureAtlas {

    data class UvRect(val uMin: Float, val vMin: Float, val uMax: Float, val vMax: Float)

    val heroEyeUv = UvRect(0.02f, 0.02f, 0.24f, 0.24f)
    val demonEyeUv = UvRect(0.26f, 0.02f, 0.48f, 0.24f)
    val bannerUv = UvRect(0.02f, 0.26f, 0.48f, 0.48f)
    val clothUv = UvRect(0.51f, 0.01f, 0.99f, 0.49f)
    val grassUv = UvRect(0.01f, 0.51f, 0.49f, 0.99f)
    val rockUv = UvRect(0.51f, 0.51f, 0.74f, 0.99f)
    val barkUv = UvRect(0.76f, 0.51f, 0.99f, 0.99f)

    fun createAndUploadAtlas(quality: QualityLevel, isNight: Boolean): Int {
        val size = if (quality == QualityLevel.BAIXA) 256 else 512
        val s = size / 512f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val half = size * 0.5f

        // 1) Q0 Superior Esquerdo: Olho Anime Heroico (0..256, 0..128)
        drawAnimeEyesTile(c, 0f, 0f, size * 0.25f, size * 0.25f, isDemon = false, p = p)
        // Olho Demoníaco / Brilho Carmesim (128..256, 0..128)
        drawAnimeEyesTile(c, size * 0.25f, 0f, size * 0.25f, size * 0.25f, isDemon = true, p = p)
        // Faixa / Estandarte Vila das Almas (0..256, 128..256)
        drawVillageBannerTile(c, 0f, size * 0.25f, half, size * 0.25f, s, p)

        // 2) Q1 Superior Direito: Couro / Tecido Dobrado com Costura Dourada
        drawFoldedClothTile(c, half, 0f, half, half, s, p)

        // 3) Q2 Inferior Esquerdo: Folhas de Capim com Canal Alpha real
        drawGrassBladesTile(c, 0f, half, half, half, s, isNight, p)

        // 4) Q3 Inferior Direito: Pedra Rachada (esquerda) + Casca/Tronco Sertão (direita)
        drawCrackedRockTile(c, half, half, size * 0.25f, half, s, isNight, p)
        drawBarkAndFoliageTile(c, size * 0.75f, half, size * 0.25f, half, s, isNight, p)

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        val texId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
        return texId
    }

    private fun drawAnimeEyesTile(
        c: Canvas,
        x0: Float,
        y0: Float,
        w: Float,
        h: Float,
        isDemon: Boolean,
        p: Paint
    ) {
        p.style = Paint.Style.FILL
        p.shader = null
        // Fundo neutro transparente para misturar com cor de pele do vértice se necessário;
        // aqui usamos faixa opaca limpa de esclera + íris + delineado estilo Storm.
        p.color = Color.argb(255, 244, 212, 184)
        if (isDemon) p.color = Color.argb(255, 42, 24, 34)
        c.drawRect(x0, y0, x0 + w, y0 + h, p)

        for (side in listOf(0.28f, 0.72f)) {
            val cx = x0 + w * side
            val cy = y0 + h * 0.56f
            val rx = w * 0.145f
            val ry = h * 0.165f

            // Esclera branca (ou escura para demônio)
            p.color = if (isDemon) Color.rgb(30, 12, 18) else Color.rgb(252, 250, 255)
            c.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), p)

            // Íris com brilho interno
            p.color = if (isDemon) Color.rgb(255, 58, 36) else Color.rgb(245, 168, 42)
            c.drawOval(RectF(cx - rx * 0.56f, cy - ry * 0.72f, cx + rx * 0.56f, cy + ry * 0.72f), p)

            // Pupila
            p.color = Color.rgb(18, 12, 16)
            c.drawOval(RectF(cx - rx * 0.24f, cy - ry * 0.38f, cx + rx * 0.24f, cy + ry * 0.38f), p)

            // Brilho specular duplo estilo anime
            p.color = Color.WHITE
            c.drawCircle(cx - rx * 0.22f, cy - ry * 0.26f, rx * 0.17f, p)
            c.drawCircle(cx + rx * 0.18f, cy + ry * 0.22f, rx * 0.09f, p)

            // Delineado superior espesso + sobrancelha expressiva
            p.style = Paint.Style.STROKE
            p.strokeWidth = w * 0.042f
            p.color = Color.rgb(18, 12, 14)
            c.drawArc(RectF(cx - rx * 1.05f, cy - ry * 1.02f, cx + rx * 1.05f, cy + ry * 0.85f), 192f, 156f, false, p)

            p.strokeWidth = w * 0.032f
            val tilt = if (side < 0.5f) 1 else -1
            c.drawLine(
                cx - rx * 0.95f,
                cy - ry * (1.28f - 0.15f * tilt),
                cx + rx * 0.95f,
                cy - ry * (1.28f + 0.15f * tilt),
                p
            )
            p.style = Paint.Style.FILL
        }
    }

    private fun drawVillageBannerTile(c: Canvas, x0: Float, y0: Float, w: Float, h: Float, s: Float, p: Paint) {
        p.style = Paint.Style.FILL
        p.color = Color.rgb(246, 240, 224)
        c.drawRect(x0, y0, x0 + w, y0 + h, p)
        // Borda carmesim e dourada das faixas da vila (Anexo 4)
        p.color = Color.rgb(176, 42, 34)
        c.drawRect(x0, y0, x0 + w, y0 + h * 0.14f, p)
        c.drawRect(x0, y0 + h * 0.86f, x0 + w, y0 + h, p)
        p.color = Color.rgb(218, 166, 48)
        for (i in 0 until 4) {
            val bx = x0 + w * (0.14f + i * 0.24f)
            c.drawCircle(bx, y0 + h * 0.5f, 14f * s, p)
            p.color = Color.rgb(198, 66, 26)
            c.drawCircle(bx, y0 + h * 0.5f, 7f * s, p)
            p.color = Color.rgb(218, 166, 48)
        }
    }

    private fun drawFoldedClothTile(c: Canvas, x0: Float, y0: Float, w: Float, h: Float, s: Float, p: Paint) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(
            x0, y0, x0 + w, y0 + h,
            intArrayOf(Color.rgb(248, 242, 232), Color.rgb(214, 204, 192), Color.rgb(240, 232, 220)),
            null,
            Shader.TileMode.CLAMP
        )
        c.drawRect(x0, y0, x0 + w, y0 + h, p)
        p.shader = null

        // Dobras diagonais cel-shaded no tecido
        p.strokeWidth = 3f * s
        p.color = Color.argb(65, 60, 42, 34)
        for (i in 1..6) {
            val yy = y0 + (h / 7f) * i
            c.drawLine(x0 + 8f * s, yy, x0 + w - 8f * s, yy + sin(i.toFloat()) * 14f * s, p)
        }
        // Costura dourada estilo Storm
        p.color = Color.rgb(224, 174, 52)
        p.strokeWidth = 2.5f * s
        var sy = y0 + 14f * s
        while (sy < y0 + h - 14f * s) {
            c.drawLine(x0 + 18f * s, sy, x0 + 18f * s, sy + 8f * s, p)
            c.drawLine(x0 + w - 18f * s, sy, x0 + w - 18f * s, sy + 8f * s, p)
            sy += 16f * s
        }
    }

    private fun drawGrassBladesTile(
        c: Canvas,
        x0: Float,
        y0: Float,
        w: Float,
        h: Float,
        s: Float,
        isNight: Boolean,
        p: Paint
    ) {
        // Fundo alpha 0 garantido para recorte limpo de folhas!
        p.style = Paint.Style.FILL
        val baseDark = if (isNight) Color.rgb(36, 68, 42) else Color.rgb(66, 128, 44)
        val baseLight = if (isNight) Color.rgb(72, 126, 78) else Color.rgb(138, 198, 72)

        for (i in 0 until 13) {
            val t = i / 12f
            val bx = x0 + w * (0.08f + t * 0.84f)
            val bladeH = h * (0.58f + (i % 3) * 0.14f)
            val lean = ((i % 5) - 2) * 14f * s
            val halfW = (9f + (i % 2) * 4f) * s

            val path = Path().apply {
                moveTo(bx - halfW, y0 + h - 4f * s)
                quadTo(bx - halfW * 0.4f + lean * 0.5f, y0 + h - bladeH * 0.55f, bx + lean, y0 + h - bladeH)
                quadTo(bx + halfW * 0.4f + lean * 0.5f, y0 + h - bladeH * 0.55f, bx + halfW, y0 + h - 4f * s)
                close()
            }
            p.shader = LinearGradient(
                bx, y0 + h, bx + lean, y0 + h - bladeH,
                baseDark, baseLight,
                Shader.TileMode.CLAMP
            )
            c.drawPath(path, p)
        }
        p.shader = null
    }

    private fun drawCrackedRockTile(
        c: Canvas,
        x0: Float,
        y0: Float,
        w: Float,
        h: Float,
        s: Float,
        isNight: Boolean,
        p: Paint
    ) {
        p.style = Paint.Style.FILL
        p.color = if (isNight) Color.rgb(120, 126, 146) else Color.rgb(168, 148, 126)
        c.drawRect(x0, y0, x0 + w, y0 + h, p)

        // Manchas facetadas estilo pintura anime dos penhascos (Anexos 2 e 3)
        for (i in 0 until 16) {
            val rx = x0 + ((i * 37) % 100) / 100f * w * 0.8f
            val ry = y0 + ((i * 53) % 100) / 100f * h * 0.8f
            val rw = (18f + (i % 4) * 12f) * s
            val rh = (14f + (i % 3) * 10f) * s
            p.color = if (i % 2 == 0) Color.argb(46, 255, 245, 226) else Color.argb(56, 54, 42, 34)
            c.drawRect(rx, ry, rx + rw, ry + rh, p)
        }

        // Veios escuros de rachadura
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * s
        p.color = Color.argb(95, 38, 28, 22)
        for (i in 0 until 5) {
            val sx = x0 + w * (0.12f + i * 0.18f)
            c.drawLine(sx, y0 + 10f * s, sx + abs(sin(i.toFloat())) * 18f * s, y0 + h - 10f * s, p)
        }
        p.style = Paint.Style.FILL
    }

    private fun drawBarkAndFoliageTile(
        c: Canvas,
        x0: Float,
        y0: Float,
        w: Float,
        h: Float,
        s: Float,
        isNight: Boolean,
        p: Paint
    ) {
        // Metade superior = tronco / casca áspera, metade inferior = folhagem/mandacaru
        p.style = Paint.Style.FILL
        p.color = Color.rgb(142, 98, 64)
        c.drawRect(x0, y0, x0 + w, y0 + h * 0.5f, p)
        p.color = Color.rgb(92, 58, 36)
        for (i in 0 until 6) {
            val xx = x0 + w * (0.1f + i * 0.16f)
            c.drawRect(xx, y0 + 4f * s, xx + 4f * s, y0 + h * 0.48f, p)
        }

        p.color = if (isNight) Color.rgb(44, 92, 60) else Color.rgb(74, 148, 62)
        c.drawRect(x0, y0 + h * 0.5f, x0 + w, y0 + h, p)
        p.color = if (isNight) Color.rgb(78, 134, 86) else Color.rgb(146, 206, 86)
        for (i in 0 until 10) {
            val cx = x0 + ((i * 29 + 11) % 88 + 6) / 100f * w
            val cy = y0 + h * 0.56f + ((i * 41) % 38) / 100f * h
            c.drawCircle(cx, cy, (9f + (i % 3) * 4f) * s, p)
        }
    }
}
