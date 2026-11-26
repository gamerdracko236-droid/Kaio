package com.example.engine3d

import android.content.res.AssetManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.model.BladeColor
import com.example.data.model.CombatStyle
import com.example.data.model.HatStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.QualityLevel
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer principal em OpenGL ES 2.0 no estilo NARUTO: Ultimate Ninja STORM.
 * - Carrega toon_shader.glsl (MToon 2 bandas + Rim Light + Fog) e outline_shader.glsl (GL_FRONT com normais suavizadas).
 * - Gera o Atlas Procedural de Texturas com Mipmaps no onSurfaceCreated().
 * - Agrupa todos os objetos estáticos (capim, rochas, árvores/mandacarus, arquitetura) via StaticSceneBatcher.
 * - Renderiza personagens humanoides articulados com passagem dupla (Toon + Contorno Anime) e sombras projetadas.
 */
class GameRenderer : GLSurfaceView.Renderer {

    @Volatile
    var snapshot: WorldRenderSnapshot = WorldRenderSnapshot()

    @Volatile
    var cameraYaw: Float = 0f

    @Volatile
    var cameraPitch: Float = 0.34f

    @Volatile
    var cameraDistance: Float = 7.4f

    @Volatile
    private var assetManager: AssetManager? = null

    fun attachAssetManager(assets: AssetManager) {
        assetManager = assets
    }

    private var toonProg = 0
    private var outlineProg = 0
    private var atlasTexId = 0

    private val staticSceneBatcher = StaticSceneBatcher()
    private val worldRenderer = WorldSkyAndGroundRenderer()

    private var heroMesh: HumanoidAnimeMesh? = null
    private var npcMesh: HumanoidAnimeMesh? = null
    private val enemyMeshes = HashMap<EnemyVisualKind, HumanoidAnimeMesh>()
    private val objModels = HashMap<String, LoadedObjMesh>()

    private var builtSeed = -1
    private var builtQuality: QualityLevel? = null
    private var builtIsNight: Boolean? = null
    private var builtHeroKey: String = ""
    private var builtNpcName: String = ""

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

    private var animTime = 0f
    private var lastFrameNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        val (toonVs, toonFs) = loadShaderPair("shaders/toon_shader.glsl", DEFAULT_TOON_VERT, DEFAULT_TOON_FRAG)
        val (outVs, outFs) = loadShaderPair("shaders/outline_shader.glsl", DEFAULT_OUTLINE_VERT, DEFAULT_OUTLINE_FRAG)
        toonProg = createProgram(toonVs, toonFs)
        outlineProg = createProgram(outVs, outFs)

        val snap = snapshot
        rebuildAtlasAndStaticWorld(snap)
        worldRenderer.initOrUpdate(snap.skyTopRgb, snap.skyBottomRgb, snap.groundRgb, snap.isNight)
        rebuildHeroMeshIfNeeded(snap)
        rebuildNpcMeshIfNeeded(snap)
        buildEnemyMeshes()
        loadOptionalObjAssets()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 50f, aspect, 0.2f, 185f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now
        animTime += dt

        val snap = snapshot
        if (snap.regionSeed != builtSeed || snap.qualityLevel != builtQuality || snap.isNight != builtIsNight) {
            rebuildAtlasAndStaticWorld(snap)
        }
        rebuildHeroMeshIfNeeded(snap)
        rebuildNpcMeshIfNeeded(snap)

        worldRenderer.initOrUpdate(snap.skyTopRgb, snap.skyBottomRgb, snap.groundRgb, snap.isNight)

        val bg = snap.skyBottomRgb
        GLES20.glClearColor(bg.first, bg.second, bg.third, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val camPos = updateCamera(snap)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        applyGlobalShaderUniforms(snap, camPos)

        // 1. Domo Celeste, Chão e Monumento da Chama
        worldRenderer.drawSkyAndGround(toonProg, atlasTexId, vpMatrix, snap.playerX, snap.playerZ)

        // 2. Static Batching (Vila, rochas, árvores/mandacarus e tufos de capim em poucos VBOs)
        staticSceneBatcher.drawAll(toonProg, atlasTexId, vpMatrix, identityMatrix)

        // Props externos opcionais (.obj + .png em assets/models/)
        objModels["arvore"]?.let { tree ->
            val treeCoords = arrayOf(-11f to -4f, 11f to -6f, -13f to -17f, 12f to -20f)
            for ((tx, tz) in treeCoords) {
                drawStaticObjProp(tree, tx, 0f, tz, yawDeg = tx * 25f)
            }
        }
        objModels["casa"]?.let { house ->
            drawStaticObjProp(house, -11.5f, 0f, 8.5f, yawDeg = 35f)
            drawStaticObjProp(house, 11.5f, 0f, -11.0f, yawDeg = -85f)
        }

        // 3. Portal da Região
        drawPortalMarker(snap)

        // 4. Sombras de contato projetadas no solo
        worldRenderer.drawContactShadow(toonProg, vpMatrix, snap.playerX, snap.playerZ, snap.playerY, 1.0f)
        worldRenderer.drawContactShadow(toonProg, vpMatrix, snap.npcX, snap.npcZ, 0f, 1.0f)
        for (enemy in snap.enemies) {
            val s = enemyMeshes[enemy.kind]?.profile?.scale ?: 1f
            worldRenderer.drawContactShadow(toonProg, vpMatrix, enemy.x, enemy.z, enemy.y, s)
        }

        // 5. Personagens Humanoides Anime (Passagem 1 MToon + Passagem 2 Contorno GL_FRONT)
        val speedRatio = when {
            snap.isSprinting -> 1.0f
            snap.isMoving -> 0.65f
            else -> 0f
        }
        val heroObj = objModels["kauan"]
        if (heroObj != null) {
            drawObjActor(heroObj, snap.playerX, snap.playerY, snap.playerZ, snap.playerYawRad, speedRatio, outlinePass = false)
            drawObjActor(heroObj, snap.playerX, snap.playerY, snap.playerZ, snap.playerYawRad, speedRatio, outlinePass = true)
        } else {
            heroMesh?.let { mesh ->
                drawArticulatedCharacter(
                    mesh = mesh,
                    x = snap.playerX,
                    y = snap.playerY,
                    z = snap.playerZ,
                    yawRad = snap.playerYawRad,
                    speedRatio = speedRatio,
                    attackProgress = snap.attackProgress,
                    isBlocking = snap.isBlocking,
                    isCharging = snap.isChargingEnergy,
                    isHitFlash = false,
                    isWindup = false,
                    isStunned = false,
                    floatingBob = 0f,
                    camPos = camPos
                )
            }
        }

        val npcYaw = kotlin.math.atan2(snap.playerX - snap.npcX, snap.playerZ - snap.npcZ)
        val npcObj = objModels["npc"]
        if (npcObj != null) {
            drawObjActor(npcObj, snap.npcX, 0f, snap.npcZ, npcYaw, 0f, outlinePass = false)
            drawObjActor(npcObj, snap.npcX, 0f, snap.npcZ, npcYaw, 0f, outlinePass = true)
        } else {
            npcMesh?.let { mesh ->
                drawArticulatedCharacter(
                    mesh = mesh,
                    x = snap.npcX,
                    y = 0f,
                    z = snap.npcZ,
                    yawRad = npcYaw,
                    speedRatio = 0f,
                    attackProgress = 0f,
                    isBlocking = false,
                    isCharging = false,
                    isHitFlash = false,
                    isWindup = false,
                    isStunned = false,
                    floatingBob = sin(animTime * 2.2f) * 0.02f,
                    camPos = camPos
                )
            }
        }

        for (enemy in snap.enemies) {
            val mesh = enemyMeshes[enemy.kind] ?: continue
            val bob = if (enemy.kind == EnemyVisualKind.FLYING) 0.55f + sin(animTime * 4.2f) * 0.16f else 0f
            val eSpeed = if (!enemy.isStunned && !enemy.isWindup) 0.45f else 0f
            val enemyObj = objModels[enemy.kind.name.lowercase()]
            if (enemyObj != null) {
                drawObjActor(enemyObj, enemy.x, enemy.y + bob, enemy.z, enemy.yawRad, eSpeed, outlinePass = false, isHitFlash = enemy.isHitFlash)
                drawObjActor(enemyObj, enemy.x, enemy.y + bob, enemy.z, enemy.yawRad, eSpeed, outlinePass = true, isHitFlash = enemy.isHitFlash)
            } else {
                drawArticulatedCharacter(
                    mesh = mesh,
                    x = enemy.x,
                    y = enemy.y,
                    z = enemy.z,
                    yawRad = enemy.yawRad,
                    speedRatio = eSpeed,
                    attackProgress = if (enemy.isWindup) 0.35f else 0f,
                    isBlocking = false,
                    isCharging = false,
                    isHitFlash = enemy.isHitFlash,
                    isWindup = enemy.isWindup,
                    isStunned = enemy.isStunned,
                    floatingBob = bob,
                    camPos = camPos
                )
            }
            if (enemy.isWindup) {
                worldRenderer.drawRing(toonProg, vpMatrix, enemy.x, 0.04f, enemy.z, 1.35f * mesh.profile.scale, animTime, 1f, 0.20f, 0.18f, 0.72f)
            }
            if (enemy.isLocked) {
                worldRenderer.drawRing(toonProg, vpMatrix, enemy.x, 0.05f, enemy.z, 1.15f * mesh.profile.scale, animTime * 2.4f, 1f, 0.84f, 0.22f, 0.90f)
            }
        }

        // 6. Auras e Efeitos de Combate (Golpes, Carregamento e Defesa)
        if (snap.isBlocking) {
            val col = styleRgb(snap.activeStyle)
            worldRenderer.drawRing(toonProg, vpMatrix, snap.playerX, 0.95f + snap.playerY, snap.playerZ, 1.05f, animTime * 4f, col.first, col.second, col.third, 0.78f)
        }
        if (snap.isChargingEnergy) {
            val col = styleRgb(snap.activeStyle)
            val pulse = 0.85f + 0.22f * sin(animTime * 12f)
            worldRenderer.drawRing(toonProg, vpMatrix, snap.playerX, 0.12f + snap.playerY, snap.playerZ, 1.25f * pulse, -animTime * 5f, col.first, col.second, col.third, 0.85f)
        }
        for (vfx in snap.vfxBursts) {
            worldRenderer.drawOrb(toonProg, vpMatrix, vfx.x, vfx.y, vfx.z, vfx.radius * 0.55f, vfx.rgb.first, vfx.rgb.second, vfx.rgb.third, vfx.alpha)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawPortalMarker(snap: WorldRenderSnapshot) {
        val r = if (snap.portalUnlocked) 0.22f else 0.72f
        val g = if (snap.portalUnlocked) 0.92f else 0.52f
        val b = if (snap.portalUnlocked) 1.0f else 0.24f
        worldRenderer.drawRing(toonProg, vpMatrix, snap.portalX, 0.06f, snap.portalZ, 1.75f, animTime * 1.6f, r, g, b, 0.88f)
        if (snap.portalUnlocked) {
            worldRenderer.drawOrb(toonProg, vpMatrix, snap.portalX, 1.45f + sin(animTime * 3f) * 0.14f, snap.portalZ, 0.52f, r, g, b, 0.65f)
        }
    }

    private fun drawArticulatedCharacter(
        mesh: HumanoidAnimeMesh,
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        speedRatio: Float,
        attackProgress: Float,
        isBlocking: Boolean,
        isCharging: Boolean,
        isHitFlash: Boolean,
        isWindup: Boolean,
        isStunned: Boolean,
        floatingBob: Float,
        camPos: FloatArray
    ) {
        drawCharacterPass(
            mesh = mesh,
            outlinePass = false,
            x = x, y = y + floatingBob, z = z, yawRad = yawRad,
            speedRatio = speedRatio, attackProgress = attackProgress,
            isBlocking = isBlocking, isCharging = isCharging,
            isHitFlash = isHitFlash, isWindup = isWindup, isStunned = isStunned
        )
        drawCharacterPass(
            mesh = mesh,
            outlinePass = true,
            x = x, y = y + floatingBob, z = z, yawRad = yawRad,
            speedRatio = speedRatio, attackProgress = attackProgress,
            isBlocking = isBlocking, isCharging = isCharging,
            isHitFlash = isHitFlash, isWindup = isWindup, isStunned = isStunned
        )
    }

    private fun drawCharacterPass(
        mesh: HumanoidAnimeMesh,
        outlinePass: Boolean,
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        speedRatio: Float,
        attackProgress: Float,
        isBlocking: Boolean,
        isCharging: Boolean,
        isHitFlash: Boolean,
        isWindup: Boolean,
        isStunned: Boolean
    ) {
        val prog = if (outlinePass) outlineProg else toonProg
        GLES20.glUseProgram(prog)

        if (outlinePass) {
            GLES20.glCullFace(GLES20.GL_FRONT)
            val oc = mesh.profile.outlineColor
            GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uOutlineColor"), oc[0], oc[1], oc[2], oc[3])
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uOutlineWidth"), mesh.profile.outlineWidth)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uVPMatrix"), 1, false, vpMatrix, 0)
        } else {
            GLES20.glCullFace(GLES20.GL_BACK)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlasTexId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "uTexture"), 0)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uAlphaCutoff"), 0f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uRimStrength"), if (isCharging) 0.85f else 0.44f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUnlit"), 0f)
        }

        val yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()
        val runSwing = sin(animTime * 11f) * 38f * speedRatio
        val runLean = 14f * speedRatio + (if (isCharging) -8f else 0f)
        val slashAngle = sin(attackProgress * PI.toFloat()) * 112f
        val sc = mesh.profile.scale
        val sw = mesh.profile.shoulderWidthScale

        for ((part, buf) in mesh.parts) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, sc, sc, sc)
            Matrix.translateM(modelMatrix, 0, 0f, 0.78f * mesh.profile.legLengthScale, 0f)
            Matrix.rotateM(modelMatrix, 0, runLean, 1f, 0f, 0f)

            when (part) {
                HumanoidAnimeMesh.BodyPart.TORSO -> Unit
                HumanoidAnimeMesh.BodyPart.HEAD -> {
                    Matrix.translateM(modelMatrix, 0, 0f, 0.56f, 0f)
                    if (isStunned) Matrix.rotateM(modelMatrix, 0, sin(animTime * 10f) * 12f, 0f, 0f, 1f)
                }
                HumanoidAnimeMesh.BodyPart.LEFT_ARM -> {
                    Matrix.translateM(modelMatrix, 0, -0.25f * sw, 0.50f, 0f)
                    val blockAngle = if (isBlocking) -72f else runSwing
                    Matrix.rotateM(modelMatrix, 0, blockAngle, 1f, 0f, 0f)
                }
                HumanoidAnimeMesh.BodyPart.RIGHT_ARM -> {
                    Matrix.translateM(modelMatrix, 0, 0.25f * sw, 0.50f, 0f)
                    val windupAngle = if (isWindup) -55f else -runSwing - slashAngle
                    Matrix.rotateM(modelMatrix, 0, windupAngle, 1f, 0f, 0f)
                }
                HumanoidAnimeMesh.BodyPart.WEAPON -> {
                    Matrix.translateM(modelMatrix, 0, 0.25f * sw, 0.50f, 0f)
                    val windupAngle = if (isWindup) -55f else -runSwing - slashAngle
                    Matrix.rotateM(modelMatrix, 0, windupAngle, 1f, 0f, 0f)
                    Matrix.translateM(modelMatrix, 0, 0f, -0.48f * mesh.profile.armLengthScale, 0.04f)
                    Matrix.rotateM(modelMatrix, 0, 12f, 1f, 0f, 0f)
                }
                HumanoidAnimeMesh.BodyPart.LEFT_LEG -> {
                    Matrix.translateM(modelMatrix, 0, -0.13f, 0f, 0f)
                    Matrix.rotateM(modelMatrix, 0, -runSwing * 0.9f, 1f, 0f, 0f)
                }
                HumanoidAnimeMesh.BodyPart.RIGHT_LEG -> {
                    Matrix.translateM(modelMatrix, 0, 0.13f, 0f, 0f)
                    Matrix.rotateM(modelMatrix, 0, runSwing * 0.9f, 1f, 0f, 0f)
                }
                HumanoidAnimeMesh.BodyPart.BACK_CLOTH -> {
                    Matrix.translateM(modelMatrix, 0, 0f, 0.48f, -0.02f)
                    val flutter = 10f + speedRatio * 28f + sin(animTime * 6.5f) * 8f
                    Matrix.rotateM(modelMatrix, 0, flutter, 1f, 0f, 0f)
                }
            }

            if (outlinePass) {
                GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uModelMatrix"), 1, false, modelMatrix, 0)
            } else {
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uMVPMatrix"), 1, false, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uModelMatrix"), 1, false, modelMatrix, 0)

                val tint = buf.tintColor
                val rMul = if (isHitFlash) 1.6f else if (isWindup) 1.35f else tint[0]
                val gMul = if (isHitFlash) 1.6f else if (isStunned) 1.25f else tint[1]
                val bMul = if (isHitFlash) 1.6f else tint[2]
                GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uBaseColor"), rMul, gMul, bMul, tint[3])
                GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUseTexture"), buf.useTexture)
                GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uEmissive"), if (isHitFlash) 0.45f else buf.emissive)
            }

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf.vboId)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buf.iboId)

            val stride = StaticSceneBatcher.STRIDE_BYTES
            val posLoc = GLES20.glGetAttribLocation(prog, "aPosition")
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, 0)

            if (outlinePass) {
                val snLoc = GLES20.glGetAttribLocation(prog, "aSmoothNormal")
                GLES20.glEnableVertexAttribArray(snLoc)
                GLES20.glVertexAttribPointer(snLoc, 3, GLES20.GL_FLOAT, false, stride, 44)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, buf.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                GLES20.glDisableVertexAttribArray(posLoc)
                GLES20.glDisableVertexAttribArray(snLoc)
            } else {
                val normLoc = GLES20.glGetAttribLocation(prog, "aNormal")
                val uvLoc = GLES20.glGetAttribLocation(prog, "aTexCoord")
                val colLoc = GLES20.glGetAttribLocation(prog, "aVertexColor")
                GLES20.glEnableVertexAttribArray(normLoc); GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, 12)
                GLES20.glEnableVertexAttribArray(uvLoc); GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, stride, 24)
                GLES20.glEnableVertexAttribArray(colLoc); GLES20.glVertexAttribPointer(colLoc, 3, GLES20.GL_FLOAT, false, stride, 32)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, buf.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
                GLES20.glDisableVertexAttribArray(posLoc)
                GLES20.glDisableVertexAttribArray(normLoc)
                GLES20.glDisableVertexAttribArray(uvLoc)
                GLES20.glDisableVertexAttribArray(colLoc)
            }
        }
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun updateCamera(snap: WorldRenderSnapshot): FloatArray {
        val camPos = FloatArray(3)
        if (snap.isDialogueActive) {
            val midX = (snap.playerX + snap.npcX) * 0.5f
            val midZ = (snap.playerZ + snap.npcZ) * 0.5f
            val sideX = cos(snap.playerYawRad) * 2.1f
            val sideZ = -sin(snap.playerYawRad) * 2.1f
            camPos[0] = midX + sideX
            camPos[1] = 1.68f
            camPos[2] = midZ + sideZ + 1.4f
            Matrix.setLookAtM(viewMatrix, 0, camPos[0], camPos[1], camPos[2], midX, 1.38f, midZ, 0f, 1f, 0f)
        } else {
            val dist = cameraDistance.coerceIn(4.6f, 10.5f)
            camPos[0] = snap.playerX + sin(cameraYaw) * cos(cameraPitch) * dist
            camPos[1] = snap.playerY + 1.55f + sin(cameraPitch) * dist
            camPos[2] = snap.playerZ + cos(cameraYaw) * cos(cameraPitch) * dist
            Matrix.setLookAtM(viewMatrix, 0, camPos[0], camPos[1], camPos[2], snap.playerX, snap.playerY + 1.32f, snap.playerZ, 0f, 1f, 0f)
        }
        return camPos
    }

    private fun applyGlobalShaderUniforms(snap: WorldRenderSnapshot, camPos: FloatArray) {
        GLES20.glUseProgram(toonProg)
        if (snap.isNight) {
            GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uSunDir"), -0.35f, 0.72f, 0.45f)
            GLES20.glUniform4f(GLES20.glGetUniformLocation(toonProg, "uShadeTint"), 0.52f, 0.56f, 0.74f, 1f)
            GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uRimColor"), 0.46f, 0.68f, 1.0f)
        } else {
            GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uSunDir"), 0.45f, 0.78f, 0.42f)
            GLES20.glUniform4f(GLES20.glGetUniformLocation(toonProg, "uShadeTint"), 0.72f, 0.68f, 0.78f, 1f)
            GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uRimColor"), 1.0f, 0.90f, 0.68f)
        }
        GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uCameraPos"), camPos[0], camPos[1], camPos[2])
        val fog = snap.skyBottomRgb
        GLES20.glUniform3f(GLES20.glGetUniformLocation(toonProg, "uFogColor"), fog.first, fog.second, fog.third)
    }

    private fun rebuildAtlasAndStaticWorld(snap: WorldRenderSnapshot) {
        if (atlasTexId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(atlasTexId), 0)
            atlasTexId = 0
        }
        atlasTexId = AnimeTextureAtlas.createAndUploadAtlas(snap.qualityLevel, snap.isNight)
        staticSceneBatcher.buildStaticSertaoProps(snap.regionSeed, snap.qualityLevel, snap.isNight)
        builtSeed = snap.regionSeed
        builtQuality = snap.qualityLevel
        builtIsNight = snap.isNight
    }

    private fun rebuildHeroMeshIfNeeded(snap: WorldRenderSnapshot) {
        val key = "${snap.hatStyle}_${snap.outfitStyle}_${snap.bladeColor}_${snap.activeStyle}_${snap.weaponUpgrade}"
        if (key == builtHeroKey && heroMesh != null) return
        heroMesh?.release()
        val profile = SertaoCharacterCatalog.heroKauan(
            hatStyle = snap.hatStyle,
            outfitStyle = snap.outfitStyle,
            bladeColor = snap.bladeColor,
            activeStyle = snap.activeStyle,
            weaponUpgrade = snap.weaponUpgrade
        )
        heroMesh = HumanoidAnimeMesh(profile).apply { buildAllParts() }
        builtHeroKey = key
    }

    private fun rebuildNpcMeshIfNeeded(snap: WorldRenderSnapshot) {
        if (snap.npcName == builtNpcName && npcMesh != null) return
        npcMesh?.release()
        val profile = SertaoCharacterCatalog.npcForRegion(snap.npcName)
        npcMesh = HumanoidAnimeMesh(profile).apply { buildAllParts() }
        builtNpcName = snap.npcName
    }

    private fun loadOptionalObjAssets() {
        objModels.values.forEach { it.release() }
        objModels.clear()
        val entries = listOf(
            Triple("kauan", "models/kauan.obj", "models/kauan.png"),
            Triple("npc", "models/severino.obj", "models/severino.png"),
            Triple("common", "models/espreitador.obj", "models/espreitador.png"),
            Triple("fast", "models/espreitador.obj", "models/espreitador.png"),
            Triple("boss", "models/boss.obj", "models/boss.png"),
            Triple("arvore", "models/arvore.obj", "models/arvore.png"),
            Triple("casa", "models/casa.obj", "models/casa.png")
        )
        for ((key, objPath, pngPath) in entries) {
            val h = when (key) {
                "boss" -> 2.45f
                "arvore" -> 4.6f
                "casa" -> 4.0f
                else -> 1.75f
            }
            ObjModelLoader.loadFromAssets(assetManager, objPath, pngPath, targetHeightMeters = h)?.let {
                objModels[key] = it
            }
        }
    }

    private fun drawStaticObjProp(obj: LoadedObjMesh, x: Float, y: Float, z: Float, yawDeg: Float) {
        GLES20.glUseProgram(toonProg)
        GLES20.glCullFace(GLES20.GL_BACK)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, if (obj.textureId != 0) obj.textureId else atlasTexId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(toonProg, "uTexture"), 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(toonProg, "uMVPMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(toonProg, "uModelMatrix"), 1, false, modelMatrix, 0)
        GLES20.glUniform4f(GLES20.glGetUniformLocation(toonProg, "uBaseColor"), 1f, 1f, 1f, 1f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uUseTexture"), if (obj.textureId != 0) 1f else 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uAlphaCutoff"), 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uEmissive"), 0.08f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uRimStrength"), 0.25f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(toonProg, "uUnlit"), 0f)
        obj.draw(toonProg, isOutlinePass = false)
    }

    private fun drawObjActor(
        obj: LoadedObjMesh,
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        speedRatio: Float,
        outlinePass: Boolean,
        isHitFlash: Boolean = false
    ) {
        val prog = if (outlinePass) outlineProg else toonProg
        GLES20.glUseProgram(prog)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 10f * speedRatio, 1f, 0f, 0f)

        if (outlinePass) {
            GLES20.glCullFace(GLES20.GL_FRONT)
            GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uOutlineColor"), 0.08f, 0.05f, 0.06f, 1f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uOutlineWidth"), 0.012f)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uVPMatrix"), 1, false, vpMatrix, 0)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uModelMatrix"), 1, false, modelMatrix, 0)
        } else {
            GLES20.glCullFace(GLES20.GL_BACK)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, if (obj.textureId != 0) obj.textureId else atlasTexId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "uTexture"), 0)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uMVPMatrix"), 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "uModelMatrix"), 1, false, modelMatrix, 0)
            val tint = if (isHitFlash) 1.55f else 1.0f
            GLES20.glUniform4f(GLES20.glGetUniformLocation(prog, "uBaseColor"), tint, tint, tint, 1f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUseTexture"), if (obj.textureId != 0) 1f else 0f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uAlphaCutoff"), 0f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uEmissive"), if (isHitFlash) 0.45f else 0.12f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uRimStrength"), 0.42f)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "uUnlit"), 0f)
        }
        obj.draw(prog, outlinePass)
        GLES20.glCullFace(GLES20.GL_BACK)
    }

    private fun buildEnemyMeshes() {
        enemyMeshes.values.forEach { it.release() }
        enemyMeshes.clear()
        for (kind in EnemyVisualKind.values()) {
            enemyMeshes[kind] = HumanoidAnimeMesh(SertaoCharacterCatalog.forEnemy(kind)).apply { buildAllParts() }
        }
    }

    private fun styleRgb(style: CombatStyle): Triple<Float, Float, Float> {
        val hex = style.colorHex
        return Triple(
            ((hex shr 16) and 0xFF) / 255f,
            ((hex shr 8) and 0xFF) / 255f,
            (hex and 0xFF) / 255f
        )
    }

    private fun loadShaderPair(
        assetPath: String,
        fallbackVert: String,
        fallbackFrag: String
    ): Pair<String, String> {
        val text = runCatching {
            assetManager?.open(assetPath)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return fallbackVert to fallbackFrag

        val vertMarker = "// === VERTEX ==="
        val fragMarker = "// === FRAGMENT ==="
        val vIdx = text.indexOf(vertMarker)
        val fIdx = text.indexOf(fragMarker)
        if (vIdx == -1 || fIdx == -1 || fIdx <= vIdx) return fallbackVert to fallbackFrag

        val vert = text.substring(vIdx + vertMarker.length, fIdx).trim()
        val frag = text.substring(fIdx + fragMarker.length).trim()
        return if (vert.isNotEmpty() && frag.isNotEmpty()) vert to frag else fallbackVert to fallbackFrag
    }

    private fun createProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        return GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
        }
    }

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, src)
            GLES20.glCompileShader(shader)
        }

    companion object {
        private const val DEFAULT_TOON_VERT = """
            uniform mat4 uMVPMatrix; uniform mat4 uModelMatrix;
            attribute vec3 aPosition; attribute vec3 aNormal; attribute vec2 aTexCoord; attribute vec3 aVertexColor;
            varying vec3 vWorldPos; varying vec3 vNormal; varying vec2 vUv; varying vec3 vCol;
            void main() {
                vec4 wp = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = wp.xyz; vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vUv = aTexCoord; vCol = aVertexColor;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val DEFAULT_TOON_FRAG = """
            precision mediump float;
            uniform sampler2D uTexture; uniform vec4 uBaseColor; uniform vec4 uShadeTint;
            uniform vec3 uSunDir; uniform vec3 uCameraPos; uniform vec3 uRimColor; uniform vec3 uFogColor;
            uniform float uRimStrength; uniform float uUseTexture; uniform float uAlphaCutoff;
            uniform float uEmissive; uniform float uUnlit;
            varying vec3 vWorldPos; varying vec3 vNormal; varying vec2 vUv; varying vec3 vCol;
            void main() {
                vec4 tex = mix(vec4(1.0), texture2D(uTexture, vUv), uUseTexture);
                if (uAlphaCutoff > 0.0 && tex.a < uAlphaCutoff) discard;
                vec3 N = normalize(vNormal); vec3 L = normalize(uSunDir); vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 albedo = tex.rgb * uBaseColor.rgb * vCol;
                float ndotl = dot(N, L);
                float band = smoothstep(0.02, 0.11, ndotl);
                vec3 lit = mix(albedo * uShadeTint.rgb, albedo * 1.09, band);
                float rim = pow(1.0 - clamp(dot(N, V), 0.0, 1.0), 3.0) * smoothstep(0.0, 0.42, ndotl + 0.35);
                vec3 shaded = mix(lit + uRimColor * rim * uRimStrength, albedo * 1.05, uUnlit) + albedo * uEmissive;
                float fog = clamp((length(uCameraPos - vWorldPos) - 24.0) / 56.0, 0.0, 0.74);
                gl_FragColor = vec4(mix(shaded, uFogColor, fog * (1.0 - uUnlit * 0.45)), uBaseColor.a * tex.a);
            }
        """

        private const val DEFAULT_OUTLINE_VERT = """
            uniform mat4 uVPMatrix; uniform mat4 uModelMatrix; uniform float uOutlineWidth;
            attribute vec3 aPosition; attribute vec3 aSmoothNormal;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec3 worldNormal = normalize(mat3(uModelMatrix) * aSmoothNormal);
                vec4 clipPos = uVPMatrix * worldPos;
                float distScale = clamp(clipPos.w * 0.14, 0.55, 2.25);
                gl_Position = uVPMatrix * vec4(worldPos.xyz + worldNormal * (uOutlineWidth * distScale), 1.0);
            }
        """

        private const val DEFAULT_OUTLINE_FRAG = """
            precision mediump float; uniform vec4 uOutlineColor;
            void main() { gl_FragColor = uOutlineColor; }
        """
    }
}
