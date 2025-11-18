package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils

// Мы используем ScreenAdapter вместо ApplicationAdapter
class GameScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    // --- Сюда скопирован ВЕСЬ твой код из MainGame.kt ---
    // Единственное изменение: `batch` и `font` мы берем из класса `game`

    private enum class ObstacleType { BAG, CONE }
    private class Obstacle(val type: ObstacleType, val rect: Rectangle)

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

    // Адаптивные размеры
    private var GROUND_LEVEL = 0f
    private var ROVER_SIZE = 0f
    private var OBSTACLE_SIZE = 0f

    // Физика
    // <<< ИЗМЕНЕНО: Более сильная гравитация для быстрого падения
    private val GRAVITY = -3000f
    // <<< ИЗМЕНЕНО: Сила подобрана под новую гравитацию (прыжок ~1.2 сек)
    private val JUMP_FORCE = 1800f

    private val roverRect = Rectangle()
    private var roverY = 0f
    private var roverVelocityY = 0f
    private var isJumping = false

    // Препятствия
    private val obstacles = Array<Obstacle>()
    private var nextSpawnTime = 0L

    // <<< ИЗМЕНЕНО: Разделяем длительность кадров для бега и прыжка
    // Анимация бега: 30 кадров, 1 секунда (1 / 30 = 0.0333)
    private val RUN_FRAME_DURATION = 1f / 30f
    // Анимация прыжка: 90 кадров, 1.2 секунды (1.2 / 90 = 0.0133)
    private val JUMP_FRAME_DURATION = 1.2f / 90f


    // `create()` из ApplicationAdapter становится `show()` в Screen
    override fun show() {
        // Настраиваем шрифт из game
        game.font.color = Color.BLACK
        // game.font.data.setScale(3f) // Это больше не нужно, шрифт генерируется

        // Адаптивные размеры
        // <<< ИЗМЕНЕНО: Опускаем "землю"
        GROUND_LEVEL = Gdx.graphics.height * 0.02f
        // <<< ИЗМЕНЕНО: Уменьшаем робота
        //ROVER_SIZE = Gdx.graphics.height * 0.3f
        ROVER_SIZE = Gdx.graphics.height * 0.5f

        // <<< ИЗМЕНЕНО: Уменьшаем препятствия
        OBSTACLE_SIZE = Gdx.graphics.height * 0.2f

        // Загрузка Анимации Бега
        runSheet = Texture("robot_run_sheet.png")
        runSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val RUN_FRAME_COLS = 6
        val RUN_FRAME_ROWS = 5
        var tmp = TextureRegion.split(runSheet, runSheet.width / RUN_FRAME_COLS, runSheet.height / RUN_FRAME_ROWS)
        var frames = tmp.flatten()
        // <<< ИЗМЕНЕНО: Используем новую константу
        runAnimation = Animation(RUN_FRAME_DURATION, *frames.toTypedArray())

        // Загрузка Анимации Прыжка
        jumpSheet = Texture("robot_jump_sheet.png")
        jumpSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val JUMP_FRAME_COLS = 10
        val JUMP_FRAME_ROWS = 9
        tmp = TextureRegion.split(jumpSheet, jumpSheet.width / JUMP_FRAME_COLS, jumpSheet.height / JUMP_FRAME_ROWS)
        frames = tmp.flatten()
        // <<< ИЗМЕНЕНО: Используем новую константу (анимация ускорилась)
        jumpAnimation = Animation(JUMP_FRAME_DURATION, *frames.toTypedArray())

        // Загружаем текстуры препятствий
        bagTexture = Texture("Bag.png")
        bagTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        coneTexture = Texture("Cone.png")
        coneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        // Настройка хитбокса ровера
        // (Формула осталась той же, но применится к новому ROVER_SIZE)
        roverRect.width = ROVER_SIZE * 0.8f
        roverRect.height = ROVER_SIZE * 0.8f
        roverRect.x = Gdx.graphics.width * 0.1f

        // Сброс игры при показе экрана
        resetGame()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (gameState == 0) {
            updateGame(delta) // Используем 'delta' вместо Gdx.graphics.deltaTime
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
        // Центрируем хитбокс относительно спрайта
        roverRect.y = roverY + (ROVER_SIZE * 0.1f)
        roverRect.x = (Gdx.graphics.width * 0.1f) + (ROVER_SIZE * 0.1f)


        if (TimeUtils.nanoTime() > nextSpawnTime) {
            spawnObstacle()
            // Уменьшаем минимальное время спавна, т.к. игра стала быстрее
            nextSpawnTime = TimeUtils.nanoTime() + MathUtils.random(1200000000L, 2500000000L)
        }

        // Оптимизированный цикл (без GC лагов)
        for (i in obstacles.size - 1 downTo 0) {
            val obstacle = obstacles[i]
            obstacle.rect.x -= 1200f * dt
            if (obstacle.rect.x + obstacle.rect.width < 0) {
                obstacles.removeIndex(i) // Безопасное удаление
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
        game.batch.begin() // Используем game.batch
        if (gameState == 0) {
            val currentFrame: TextureRegion = if (isJumping) {
                // <<< ИЗМЕНЕНО: Ставим `looping = false`
                // Теперь анимация остановится на последнем кадре, если прыжок вдруг
                // продлится дольше 1.2 сек (например, при падении с чего-либо)
                jumpAnimation.getKeyFrame(jumpStateTime, false)
            } else {
                // `looping = true` для бега
                runAnimation.getKeyFrame(runStateTime, true)
            }
            // Рисуем изначальный размер, а не хитбокс
            game.batch.draw(currentFrame, Gdx.graphics.width * 0.1f, roverY, ROVER_SIZE, ROVER_SIZE)

            for (obstacle in obstacles) {
                val textureToDraw = if (obstacle.type == ObstacleType.BAG) bagTexture else coneTexture
                game.batch.draw(textureToDraw, obstacle.rect.x, obstacle.rect.y, obstacle.rect.width, obstacle.rect.height)
            }

            // Сдвигаем очки чуть ниже, т.к. шрифт больше
            game.font.draw(game.batch, "Score: $score", 50f, Gdx.graphics.height - 50f)
        } else {
            // Центрируем текст "GAME OVER"
            game.font.draw(game.batch, "GAME OVER! Tap to restart",
                0f, Gdx.graphics.height / 2f, // x, y
                Gdx.graphics.width.toFloat(), // targetWidth
                Align.center, // Горизонтальное выравнивание
                false) // Не переносить
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
        startTime = TimeUtils.nanoTime()
        gameState = 0
        runStateTime = 0f
        jumpStateTime = 0f
        nextSpawnTime = TimeUtils.nanoTime() + 1500000000L
    }

    // `dispose()` из ApplicationAdapter становится `dispose()` в Screen
    override fun dispose() {
        // Освобождаем ТОЛЬКО ресурсы этого экрана
        runSheet.dispose()
        jumpSheet.dispose()
        bagTexture.dispose()
        coneTexture.dispose()
    }
}
