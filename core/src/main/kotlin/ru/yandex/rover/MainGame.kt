package ru.yandex.rover

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils

class MainGame : ApplicationAdapter() {

    // Вспомогательный класс
    private enum class ObstacleType { BAG, CONE }
    private class Obstacle(val type: ObstacleType, val rect: Rectangle)

    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    // Текстуры
    private lateinit var runSheet: Texture
    private lateinit var jumpSheet: Texture
    private lateinit var bagTexture: Texture
    private lateinit var coneTexture: Texture

    // Анимация
    private lateinit var runAnimation: Animation<TextureRegion>
    private lateinit var jumpAnimation: Animation<TextureRegion>
    private var runStateTime = 0f
    private var jumpStateTime = 0f

    // Параметры игры
    private var gameState = 0 // 0 - Играем, 1 - Game Over
    private var score = 0L
    private var startTime = 0L

    // Адаптивные размеры (настраиваются в create())
    private var GROUND_LEVEL = 0f
    private var ROVER_SIZE = 0f
    private var OBSTACLE_SIZE = 0f

    // Твои параметры физики
    private val GRAVITY = -1500f
    private val JUMP_FORCE = 1300f

    private val roverRect = Rectangle()
    private var roverY = 0f
    private var roverVelocityY = 0f
    private var isJumping = false

    // Препятствия
    private val obstacles = Array<Obstacle>()
    private var nextSpawnTime = 0L

    // Длительность кадра (30 кадров/сек)
    private val FRAME_DURATION = 1f / 30f

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.BLACK
        font.data.setScale(3f)

        // Твои адаптивные размеры
        GROUND_LEVEL = Gdx.graphics.height * 0.15f
        ROVER_SIZE = Gdx.graphics.height * 0.6f
        OBSTACLE_SIZE = Gdx.graphics.height * 0.25f

        // Загрузка Анимации Бега (30 кадров, 6x5)
        runSheet = Texture("robot_run_sheet.png")
        // --- ИЗМЕНЕНО: Добавляем фильтрацию ---
        runSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        val RUN_FRAME_COLS = 6
        val RUN_FRAME_ROWS = 5
        var tmp = TextureRegion.split(runSheet, runSheet.width / RUN_FRAME_COLS, runSheet.height / RUN_FRAME_ROWS)
        var frames = tmp.flatten()
        runAnimation = Animation(FRAME_DURATION, *frames.toTypedArray())

        // Загрузка Анимации Прыжка (90 кадров, 10x9)
        jumpSheet = Texture("robot_jump_sheet.png")
        // --- ИЗМЕНЕНО: Добавляем фильтрацию ---
        jumpSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        val JUMP_FRAME_COLS = 10
        val JUMP_FRAME_ROWS = 9
        tmp = TextureRegion.split(jumpSheet, jumpSheet.width / JUMP_FRAME_COLS, jumpSheet.height / JUMP_FRAME_ROWS)
        frames = tmp.flatten()
        jumpAnimation = Animation(FRAME_DURATION, *frames.toTypedArray())

        // Загружаем текстуры препятствий
        bagTexture = Texture("Bag.png")
        // --- ИЗМЕНЕНО: Добавляем фильтрацию ---
        bagTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        coneTexture = Texture("Cone.png")
        // --- ИЗМЕНЕНО: Добавляем фильтрацию ---
        coneTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)


        // Настройка хитбокса ровера
        roverRect.width = ROVER_SIZE - 50
        roverRect.height = ROVER_SIZE - 50
        roverRect.x = Gdx.graphics.width * 0.1f
        roverY = GROUND_LEVEL // Начальная позиция
        roverRect.y = roverY

        startTime = TimeUtils.nanoTime()
        nextSpawnTime = TimeUtils.nanoTime() + 1500000000L
    }

    override fun render() {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val deltaTime = Gdx.graphics.deltaTime

        if (gameState == 0) {
            updateGame(deltaTime)
        } else {
            if (Gdx.input.justTouched()) {
                resetGame()
            }
        }

        drawGame()
    }

    private fun updateGame(dt: Float) {
        if (isJumping) {
            jumpStateTime += dt
        } else {
            runStateTime += dt
        }

        score = (TimeUtils.nanoTime() - startTime) / 100000000

        // --- ЛОГИКА ПРЫЖКА ---
        if ((Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && !isJumping) {
            roverVelocityY = JUMP_FORCE
            isJumping = true
            jumpStateTime = 0f
        }

        // Применяем твою гравитацию
        roverVelocityY += GRAVITY * dt
        roverY += roverVelocityY * dt

        // Проверка земли
        if (roverY < GROUND_LEVEL) {
            roverY = GROUND_LEVEL
            roverVelocityY = 0f
            if (isJumping) {
                isJumping = false
                runStateTime = 0f
            }
        }
        roverRect.y = roverY

        // --- ЛОГИКА СПАВНА ПРЕПЯТСТВИЙ ---
        if (TimeUtils.nanoTime() > nextSpawnTime) {
            spawnObstacle()
            // СЛУЧАЙНОЕ ВРЕМЯ СПАВНА
            nextSpawnTime = TimeUtils.nanoTime() + MathUtils.random(1500000000L, 3000000000L)
        }

        // Движение препятствий
        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val obstacle = iter.next()

            // --- ИЗМЕНЕНО: Увеличена скорость ---
            obstacle.rect.x -= 900f * dt // Было 600f

            if (obstacle.rect.x + obstacle.rect.width < 0) {
                iter.remove()
            }
            if (obstacle.rect.overlaps(roverRect)) {
                gameState = 1
            }
        }
    }

    private fun spawnObstacle() {
        val obstacleRect = Rectangle()
        obstacleRect.x = Gdx.graphics.width.toFloat()
        obstacleRect.y = GROUND_LEVEL

        // Используем твой адаптивный размер
        obstacleRect.width = OBSTACLE_SIZE
        obstacleRect.height = OBSTACLE_SIZE

        // СЛУЧАЙНЫЙ ВЫБОР ТИПА
        val type = if (MathUtils.randomBoolean()) ObstacleType.BAG else ObstacleType.CONE

        obstacles.add(Obstacle(type, obstacleRect))
    }

    private fun drawGame() {
        batch.begin()

        if (gameState == 0) {
            val currentFrame: TextureRegion = if (isJumping) {
                jumpAnimation.getKeyFrame(jumpStateTime, false)
            } else {
                runAnimation.getKeyFrame(runStateTime, true)
            }
            batch.draw(currentFrame, roverRect.x, roverRect.y, roverRect.width, roverRect.height)

            // Рисуем препятствия
            for (obstacle in obstacles) {
                val textureToDraw = if (obstacle.type == ObstacleType.BAG) {
                    bagTexture
                } else {
                    coneTexture
                }
                batch.draw(textureToDraw, obstacle.rect.x, obstacle.rect.y, obstacle.rect.width, obstacle.rect.height)
            }

            font.draw(batch, "Score: $score", 50f, Gdx.graphics.height - 50f)
        } else {
            font.draw(batch, "GAME OVER! Tap to restart", Gdx.graphics.width / 2f - 280f, Gdx.graphics.height / 2f)
        }

        batch.end()
    }

    private fun resetGame() {
        roverY = GROUND_LEVEL
        roverRect.y = roverY
        roverVelocityY = 0f
        isJumping = false
        obstacles.clear()
        score = 0
        startTime = TimeUtils.nanoTime()
        gameState = 0
        runStateTime = 0f
        jumpStateTime = 0f
        nextSpawnTime = TimeUtils.nanoTime() + 1500000000L
    }

    override fun dispose() {
        batch.dispose()
        runSheet.dispose()
        jumpSheet.dispose()
        font.dispose()
        bagTexture.dispose()
        coneTexture.dispose()
    }
}
