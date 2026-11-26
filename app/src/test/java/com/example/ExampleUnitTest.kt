package com.example

import com.example.data.model.HatStyle
import com.example.data.model.WorldCatalog
import com.example.engine3d.SertaoCharacterCatalog
import com.example.engine3d.SertaoMeshBuilder
import com.example.engine3d.SmoothNormalBaker
import com.example.engine3d.StaticSceneBatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {
    @Test
    fun smoothNormals_preventCornerGapsOnCube() {
        val cubeVerts = SertaoMeshBuilder.buildCubeVertices()
        val packed = SertaoMeshBuilder.packWithSmoothNormals(cubeVerts)
        assertEquals(cubeVerts.size * SertaoMeshBuilder.FLOATS_PER_VERTEX, packed.size)

        // First vertex is (-0.5, -0.5, 0.5): smooth normal must point outward diagonally (-1, -1, +1)/sqrt(3)
        val snx = packed[6]
        val sny = packed[7]
        val snz = packed[8]
        val expected = 0.57735f
        assertTrue(abs(snx - (-expected)) < 0.01f)
        assertTrue(abs(sny - (-expected)) < 0.01f)
        assertTrue(abs(snz - expected) < 0.01f)
    }

    @Test
    fun smoothNormalBaker_deduplicatesAndAveragesCornerDirections() {
        // Two triangles sharing corner (0,0,0) with face normals +Z and +X -> diagonal (0.707, 0, 0.707)
        val bb = StaticSceneBatcher.BatchBuilder()
        bb.addVertex(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
        bb.addVertex(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f) // duplicate +Z face normal
        bb.addVertex(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f)
        val baked = SmoothNormalBaker.bake(bb.toFloatArray(), StaticSceneBatcher.FLOATS_PER_VERTEX)
        val invSqrt2 = 0.7071f
        assertTrue(abs(baked[11] - invSqrt2) < 0.01f)
        assertTrue(abs(baked[12]) < 0.01f)
        assertTrue(abs(baked[13] - invSqrt2) < 0.01f)
    }

    @Test
    fun staticBatchBuilder_packsIndexedGeometryUnderShortLimit() {
        val bb = StaticSceneBatcher.BatchBuilder(alphaCutoff = 0.45f)
        val col = floatArrayOf(0.5f, 0.8f, 0.3f)
        val uv = com.example.engine3d.AnimeTextureAtlas.grassUv
        for (i in 0 until 25) {
            bb.addBox(i * 2f, 0f, 0f, 1f, 1f, 1f, 0f, uv, col, col)
        }
        assertEquals(25 * 24, bb.vertexCount)
        assertEquals(25 * 36, bb.batchedIndices)
        assertTrue(bb.vertexCount < StaticSceneBatcher.MAX_VERTICES_PER_BATCH)
    }

    @Test
    fun characterCatalog_tracksHeroUpgradesAndRegionNpcs() {
        val baseHero = SertaoCharacterCatalog.heroKauan(hatStyle = HatStyle.VAQUEIRO, weaponUpgrade = 1)
        val eliteHero = SertaoCharacterCatalog.heroKauan(hatStyle = HatStyle.BANDANA, weaponUpgrade = 4)
        assertTrue(eliteHero.shoulderWidthScale > baseHero.shoulderWidthScale)
        assertEquals("Mestre Severino", SertaoCharacterCatalog.npcForRegion("Mestre Severino").displayName)
    }

    @Test
    fun objLoader_triangulatesQuadsNormalizesHeightAndBakesSmoothNormals() {
        val sampleObj = """
            v -1.0 2.0 0.0
            v 1.0 2.0 0.0
            v 1.0 6.0 0.0
            v -1.0 6.0 0.0
            vt 0.0 0.0
            vt 1.0 1.0
            f 1/1 2/2 3/2 4/1
        """.trimIndent()
        val parsed = com.example.engine3d.ObjModelLoader.parseAndNormalizeObj(
            sampleObj.byteInputStream(),
            targetHeightMeters = 2.0f,
            flipV = true
        )!!
        // Quad -> 2 triangles = 6 vertices * 14 floats
        assertEquals(6 * StaticSceneBatcher.FLOATS_PER_VERTEX, parsed.size)
        // Minimum Y grounded to 0.0f, max Y scaled to 2.0f
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until 6) {
            val y = parsed[i * StaticSceneBatcher.FLOATS_PER_VERTEX + 1]
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        assertTrue(abs(minY) < 0.001f)
        assertTrue(abs(maxY - 2.0f) < 0.001f)
    }

    @Test
    fun campaignStoryAndChapters_remainIntact() {
        assertEquals(10, WorldCatalog.regions.size)
        assertEquals(1, WorldCatalog.regions.first().chapterRange.first)
        assertEquals(20, WorldCatalog.regions.last().chapterRange.last)
    }
}
