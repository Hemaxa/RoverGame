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
    // Основные инструменты для рисовки
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    // Ресурсы (картинки)
    private lateinit var roverSheet: Texture
    private lateinit var obstacleTexture: Texture

    // Анимация
    private lateinit var runAnimation: Animation<TextureRegion>
    private var stateTime = 0f // Время для отслеживания кадров анимации

    // Параметры игры
    private var gameState = 0 // 0 - Играем, 1 - Game Over
    private var score = 0L
    private var startTime = 0L

    // Физика Ровера
    private val roverRect = Rectangle() // Хитбокс робота
    private var roverY = 0f // Позиция по Y
    private var roverVelocityY = 0f // Скорость по вертикали
    private val GRAVITY = -1200f // Сила тяжести
    private val JUMP_FORCE = 600f // Сила прыжка
    private val GROUND_LEVEL = 100f // Уровень земли
    private var isJumping = false

    // Препятствия (сугробы или машины)
    private val obstacles = Array<Rectangle>()
    private var lastObstacleTime = 0L

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont() // Стандартный шрифт
        font.color = Color.BLACK
        font.data.setScale(3f)

        // --- ЗАГРУЗКА СПРАЙТА РОБОТА ---
        // Допустим, у тебя файл rover_sheet.png, где 4 кадра в ряд
        roverSheet = Texture("robot_drive_sheet.png")

        // Разрезаем лист на кадры.
        // FRAME_COLS - сколько кадров по горизонтали, FRAME_ROWS - по вертикали
        val FRAME_COLS = 10
        val FRAME_ROWS = 9

        val tmp = TextureRegion.split(
            roverSheet,
            roverSheet.width / FRAME_COLS,
            roverSheet.height / FRAME_ROWS
        )

        // Превращаем двумерный массив в одномерный для анимации
        val walkFrames = arrayOfNulls<TextureRegion>(FRAME_COLS * FRAME_ROWS)
        var index = 0
        for (i in 0 until FRAME_ROWS) {
            for (j in 0 until FRAME_COLS) {
                walkFrames[index++] = tmp[i][j]
            }
        }

        // Создаем анимацию: 0.1f - время показа одного кадра
        runAnimation = Animation(0.1f, *walkFrames)

        // Создаем текстуру препятствия (можно просто красный квадрат для теста)
        obstacleTexture = Texture("obstacle.png")

        // Настройка хитбокса ровера
        roverRect.width = 200f
        roverRect.height = 200f
        roverRect.x = 100f // Ровер всегда слева

        startTime = TimeUtils.nanoTime()
    }

    override fun render() {
        // Очистка экрана (Белый фон, как снег)
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
        stateTime += dt
        score = (TimeUtils.nanoTime() - startTime) / 100000000

        // --- ЛОГИКА ПРЫЖКА ---
        // Если нажали на экран ИЛИ пробел, и мы на земле -> Прыжок
        if ((Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && !isJumping) {
            roverVelocityY = JUMP_FORCE
            isJumping = true
        }

        // Применяем гравитацию
        roverVelocityY += GRAVITY * dt
        roverY += roverVelocityY * dt

        // Проверка земли
        if (roverY < GROUND_LEVEL) {
            roverY = GROUND_LEVEL
            roverVelocityY = 0f
            isJumping = false
        }

        // Обновляем хитбокс
        roverRect.y = roverY

        // --- ЛОГИКА ПРЕПЯТСТВИЙ ---
        // Спавн нового препятствия каждые 1.5 - 3 секунды
        if (TimeUtils.nanoTime() - lastObstacleTime > 1500000000) {
            spawnObstacle()
        }

        // Движение препятствий
        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val obstacle = iter.next()
            obstacle.x -= 600f * dt // Скорость движения мира влево

            // Если ушло за экран - удаляем
            if (obstacle.x + 64 < 0) iter.remove()

            // Проверка столкновения
            if (obstacle.overlaps(roverRect)) {
                gameState = 1 // Game Over
            }
        }
    }

    private fun spawnObstacle() {
        val obstacle = Rectangle()
        obstacle.x = Gdx.graphics.width.toFloat()
        obstacle.y = GROUND_LEVEL
        obstacle.width = 80f
        obstacle.height = 80f
        obstacles.add(obstacle)
        lastObstacleTime = TimeUtils.nanoTime()
    }

    private fun drawGame() {
        batch.begin()

        if (gameState == 0) {
            // Получаем текущий кадр анимации
            // Если прыгаем, можно зафиксировать один кадр, но тут оставим бег
            val currentFrame = runAnimation.getKeyFrame(stateTime, true)

            batch.draw(currentFrame, roverRect.x, roverRect.y, roverRect.width, roverRect.height)

            // Рисуем препятствия
            for (obstacle in obstacles) {
                batch.draw(obstacleTexture, obstacle.x, obstacle.y, obstacle.width, obstacle.height)
            }

            font.draw(batch, "Score: $score", 50f, Gdx.graphics.height - 50f)
        } else {
            font.draw(batch, "GAME OVER! Tap to restart", Gdx.graphics.width / 2f - 280f, Gdx.graphics.height / 2f)
        }

        batch.end()
    }

    private fun resetGame() {
        roverY = GROUND_LEVEL
        roverVelocityY = 0f
        isJumping = false
        obstacles.clear()
        score = 0
        startTime = TimeUtils.nanoTime()
        gameState = 0
    }

    override fun dispose() {
        // Очень важно очищать память!
        batch.dispose()
        roverSheet.dispose()
        obstacleTexture.dispose()
        font.dispose()
    }
}
