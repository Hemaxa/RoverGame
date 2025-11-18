package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils

class GameScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    private enum class ObstacleType { BAG, CONE }
    private class Obstacle(val type: ObstacleType, val rect: Rectangle)

    private lateinit var runSheet: Texture
    private lateinit var jumpSheet: Texture
    private lateinit var bagTexture: Texture
    private lateinit var coneTexture: Texture
    private lateinit var backTexture: Texture
    private lateinit var midTexture: Texture
    private lateinit var roadTexture: Texture
    private lateinit var frontTexture: Texture
    private lateinit var panelNinePatch: NinePatch

    // --- Переменные музыки удалены, используем game.setMusicTargetVolume ---

    private var backX = 0f
    private var midX = 0f
    private var roadX = 0f
    private var frontX = 0f

    private lateinit var runAnimation: Animation<TextureRegion>
    private lateinit var jumpAnimation: Animation<TextureRegion>
    private var runStateTime = 0f
    private var jumpStateTime = 0f

    private var gameState = 0
    private var score = 0L
    private var obstaclesPassedCounter = 0L
    private var startTime = 0L

    private var GROUND_LEVEL = 0f
    private var ROVER_SIZE = 0f
    private var OBSTACLE_SIZE = 0f

    private val GRAVITY = -3000f
    private val JUMP_FORCE = 1800f
    private val GAME_SPEED = 1200f
    private val SPEED_BACK = GAME_SPEED * 0.1f
    private val SPEED_MID = GAME_SPEED * 0.6f
    private val SPEED_ROAD = GAME_SPEED
    private val SPEED_FRONT = GAME_SPEED * 1.25f

    private val roverRect = Rectangle()
    private var roverY = 0f
    private var roverVelocityY = 0f
    private var isJumping = false

    private val obstacles = Array<Obstacle>()
    private var nextSpawnTime = 0L
    private val RUN_FRAME_DURATION = 1f / 30f
    private val JUMP_FRAME_DURATION = 1.2f / 90f

    private var statsSent = false

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        game.font.color = Color.BLACK

        // --- МУЗЫКА: Игра началась, включаем громко! ---
        game.setMusicTargetVolume(1.0f)
        // -----------------------------------------------

        GROUND_LEVEL = Gdx.graphics.height * 0.02f
        ROVER_SIZE = Gdx.graphics.height * 0.5f
        OBSTACLE_SIZE = Gdx.graphics.height * 0.2f

        val radius = 20
        val size = radius * 2 + 2
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fillCircle(radius, radius, radius)
        pixmap.fillCircle(size - radius - 1, radius, radius)
        pixmap.fillCircle(radius, size - radius - 1, radius)
        pixmap.fillCircle(size - radius - 1, size - radius - 1, radius)
        pixmap.fillRectangle(radius, 0, size - 2 * radius, size)
        pixmap.fillRectangle(0, radius, size, size - 2 * radius)
        val roundedTexture = Texture(pixmap)
        pixmap.dispose()
        panelNinePatch = NinePatch(roundedTexture, radius, radius, radius, radius)

        runSheet = Texture("robot_run_sheet.png")
        runSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val runTmp = TextureRegion.split(runSheet, runSheet.width / 6, runSheet.height / 5)
        runAnimation = Animation(RUN_FRAME_DURATION, *runTmp.flatten().toTypedArray())

        jumpSheet = Texture("robot_jump_sheet.png")
        jumpSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val jumpTmp = TextureRegion.split(jumpSheet, jumpSheet.width / 10, jumpSheet.height / 9)
        jumpAnimation = Animation(JUMP_FRAME_DURATION, *jumpTmp.flatten().toTypedArray())

        bagTexture = Texture("Bag.png")
        bagTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        coneTexture = Texture("Cone.png")
        coneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        backTexture = Texture("back.png")
        midTexture = Texture("mid.png")
        roadTexture = Texture("road.png")
        frontTexture = Texture("front.png")
        backTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        midTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        roadTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        frontTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        roverRect.width = ROVER_SIZE * 0.4f
        roverRect.height = ROVER_SIZE * 0.6f

        resetGame()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // При смене состояния меняем громкость через главный класс
        when (gameState) {
            0 -> {
                // Если играем - громко
                game.setMusicTargetVolume(1.0f)
                updateGame(delta)
            }
            1 -> {
                // Game Over - можно потише
                game.setMusicTargetVolume(0.5f)

                // --- НОВОЕ: Отправка статистики ---
                if (game.currentUser != null && !statsSent) {
                    sendGameStats()
                }
                // ----------------------------------

                if (Gdx.input.justTouched()) resetGame()
            }
            2 -> {
                // ПАУЗА - тихо
                game.setMusicTargetVolume(0.3f)
                if (Gdx.input.isKeyJustPressed(Input.Keys.P)) gameState = 0
            }
        }

        drawGame()
    }

    // --- НОВАЯ ФУНКЦИЯ: Отправка данных на сервер ---
    private fun sendGameStats() {
        // 1. Проверяем, есть ли зарегистрированный пользователь
        val username = game.currentUser?.username
        if (username == null) {
            Gdx.app.log("GameScreen", "User not registered, skipping stat update.")
            statsSent = true // Чтобы не повторять запрос
            return
        }

        // 2. Рассчитываем время игры в секундах
        // Время в наносекундах / 1,000,000,000 = время в секундах
        val totalPlaytimeSeconds = (TimeUtils.nanoTime() - startTime) / 1000000000L

        Gdx.app.log("GameScreen", "Sending stats: Score=$score, Time=$totalPlaytimeSeconds")

        // 3. Вызываем сетевой клиент
        ApiClient.updateStats(
            username,
            score.toInt(), // Ваш счет уже Long, переводим в Int
            totalPlaytimeSeconds.toInt(),
            object : ApiListener {
                override fun onSuccess(user: UserResponse) {
                    Gdx.app.log("GameScreen", "Stats updated! New Best Score: ${user.best_score}")
                    // Опционально, можно обновить game.currentUser новыми данными
                    game.currentUser = user
                }

                override fun onFailure(message: String) {
                    Gdx.app.error("GameScreen", "Failed to update stats: $message")
                }
            }
        )
        statsSent = true
    }

    private fun updateGame(dt: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            gameState = 2
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(MenuScreen(game))
            return
        }

        backX -= SPEED_BACK * dt
        midX -= SPEED_MID * dt
        roadX -= SPEED_ROAD * dt
        frontX -= SPEED_FRONT * dt

        if (backX <= -Gdx.graphics.width) backX = 0f
        if (midX <= -Gdx.graphics.width) midX = 0f
        if (roadX <= -Gdx.graphics.width) roadX = 0f
        if (frontX <= -Gdx.graphics.width) frontX = 0f

        if (isJumping) jumpStateTime += dt else runStateTime += dt
        score = (TimeUtils.nanoTime() - startTime) / 100000000

        if ((Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && !isJumping) {
            roverVelocityY = JUMP_FORCE
            isJumping = true
            jumpStateTime = 0f
        }

        roverVelocityY += GRAVITY * dt
        roverY += roverVelocityY * dt

        if (roverY < GROUND_LEVEL) {
            roverY = GROUND_LEVEL
            roverVelocityY = 0f
            if (isJumping) {
                isJumping = false
                runStateTime = 0f
            }
        }

        roverRect.x = (Gdx.graphics.width * 0.1f) + (ROVER_SIZE * 0.3f)
        roverRect.y = roverY + (ROVER_SIZE * 0.1f)

        if (TimeUtils.nanoTime() > nextSpawnTime) {
            spawnObstacle()
            nextSpawnTime = TimeUtils.nanoTime() + MathUtils.random(1200000000L, 2500000000L)
        }

        for (i in obstacles.size - 1 downTo 0) {
            val obstacle = obstacles[i]
            obstacle.rect.x -= GAME_SPEED * dt
            if (obstacle.rect.x + obstacle.rect.width < 0) {
                obstaclesPassedCounter++
                obstacles.removeIndex(i)
            } else if (obstacle.rect.overlaps(roverRect)) {
                gameState = 1
            }
        }
    }

    private fun spawnObstacle() {
        val obstacleRect = Rectangle()
        obstacleRect.x = Gdx.graphics.width.toFloat()
        obstacleRect.y = GROUND_LEVEL
        obstacleRect.width = OBSTACLE_SIZE
        obstacleRect.height = OBSTACLE_SIZE
        val type = if (MathUtils.randomBoolean()) ObstacleType.BAG else ObstacleType.CONE
        obstacles.add(Obstacle(type, obstacleRect))
    }

    private fun drawGame() {
        game.batch.begin()

        fun drawLayer(texture: Texture, x: Float) {
            game.batch.draw(texture, x, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            game.batch.draw(texture, x + Gdx.graphics.width, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }

        drawLayer(backTexture, backX)
        drawLayer(midTexture, midX)
        drawLayer(roadTexture, roadX)

        if (gameState == 0 || gameState == 2) {
            val currentFrame = if (isJumping) jumpAnimation.getKeyFrame(jumpStateTime, false) else runAnimation.getKeyFrame(runStateTime, true)
            game.batch.draw(currentFrame, Gdx.graphics.width * 0.1f, roverY, ROVER_SIZE, ROVER_SIZE)

            for (obstacle in obstacles) {
                val textureToDraw = if (obstacle.type == ObstacleType.BAG) bagTexture else coneTexture
                game.batch.draw(textureToDraw, obstacle.rect.x, obstacle.rect.y, obstacle.rect.width, obstacle.rect.height)
            }
        }

        drawLayer(frontTexture, frontX)

        fun drawPanel(x: Float, y: Float, w: Float, h: Float) {
            game.batch.setColor(1f, 1f, 1f, 0.6f)
            panelNinePatch.draw(game.batch, x, y, w, h)
            game.batch.setColor(1f, 1f, 1f, 1f)
        }

        if (gameState == 0 || gameState == 2) {
            drawPanel(20f, Gdx.graphics.height - 160f, 400f, 125f)
            game.font.draw(game.batch, "Score: $score", 50f, Gdx.graphics.height - 50f)
            game.font.draw(game.batch, "Passed: $obstaclesPassedCounter", 50f, Gdx.graphics.height - 110f)

            if (gameState == 2) {
                drawPanel(Gdx.graphics.width * 0.2f, Gdx.graphics.height * 0.4f, Gdx.graphics.width * 0.6f, 200f)
                game.font.draw(game.batch, "PAUSED", 0f, Gdx.graphics.height / 2f + 30f, Gdx.graphics.width.toFloat(), Align.center, false)
            }
        } else if (gameState == 1) {
            drawPanel(Gdx.graphics.width * 0.15f, Gdx.graphics.height * 0.3f, Gdx.graphics.width * 0.7f, 400f)
            game.font.draw(game.batch, "GAME OVER!", 0f, Gdx.graphics.height / 2f + 120f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Score: $score", 0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Tap to restart", 0f, Gdx.graphics.height / 2f - 100f, Gdx.graphics.width.toFloat(), Align.center, false)
        }

        game.batch.end()
    }

    private fun resetGame() {
        roverY = GROUND_LEVEL
        roverRect.y = roverY
        roverVelocityY = 0f
        isJumping = false
        obstacles.clear()
        score = 0
        obstaclesPassedCounter = 0L
        startTime = TimeUtils.nanoTime()
        gameState = 0
        runStateTime = 0f
        jumpStateTime = 0f
        nextSpawnTime = TimeUtils.nanoTime() + 1500000000L

        // --- НОВОЕ: Сброс флага статистики при рестарте ---
        statsSent = false
        // -------------------------------------------------
    }

    override fun hide() {
        // Мы больше НЕ останавливаем музыку здесь!
        // Просто говорим ей стать тихой, так как мы уходим в меню или другой экран
        game.setMusicTargetVolume(0.3f)
    }

    override fun dispose() {
        runSheet.dispose()
        jumpSheet.dispose()
        bagTexture.dispose()
        coneTexture.dispose()
        backTexture.dispose()
        midTexture.dispose()
        roadTexture.dispose()
        frontTexture.dispose()
        panelNinePatch.texture.dispose()
        // Музыку здесь не трогаем, она в Game!
    }
}
