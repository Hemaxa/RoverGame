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

//GameScreen - класс самого окна игры
class GameScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    //объекты для препятствий
    private enum class ObstacleType { BAG, CONE }
    private class Obstacle(val type: ObstacleType, val rect: Rectangle)

    //поля текстур
    private lateinit var runSheet: Texture
    private lateinit var jumpSheet: Texture
    private lateinit var bagTexture: Texture
    private lateinit var coneTexture: Texture

    private lateinit var backTexture: Texture
    private lateinit var midTexture: Texture
    private lateinit var roadTexture: Texture
    private lateinit var frontTexture: Texture

    //растягивающая рамка для интерфейса
    private lateinit var panelNinePatch: NinePatch

    //начальные координаты каждого слоя фона
    private var backX = 0f
    private var midX = 0f
    private var roadX = 0f
    private var frontX = 0f

    //анимации
    private lateinit var runAnimation: Animation<TextureRegion>
    private lateinit var jumpAnimation: Animation<TextureRegion>

    //таймеры для анимаций (счетчик кадров)
    private var runStateTime = 0f
    private var jumpStateTime = 0f

    //состояние игры (0 = Playing, 1 = Game Over, 2 = Paused)
    private var gameState = 0

    //текущие счет и количество пройденных препятствий
    private var score = 0L
    private var obstaclesPassedCounter = 0L

    //время начала игры
    private var startTime = 0L

    //размеры объектов (вычисляются относительно размера окна устройства)
    private var GROUND_LEVEL = 0f
    private var ROVER_SIZE = 0f
    private var OBSTACLE_SIZE = 0f

    //параметры физики
    private val GRAVITY = -3000f //сила гравитации
    private val JUMP_FORCE = 1800f //сила прыжка
    private val GAME_SPEED = 1200f //скорость игры

    //скорости движения слоев фона
    private val SPEED_BACK = GAME_SPEED * 0.1f
    private val SPEED_MID = GAME_SPEED * 0.6f
    private val SPEED_ROAD = GAME_SPEED
    private val SPEED_FRONT = GAME_SPEED * 1.25f

    //прямоугольник игрока
    private val roverRect = Rectangle()
    private var roverY = 0f //высота игрока
    private var roverVelocityY = 0f
    private var isJumping = false //флаг состояния прыжка

    //список активных на экране препятствий
    private val obstacles = Array<Obstacle>()
    private var nextSpawnTime = 0L

    //настройки скорости анимации
    private val RUN_FRAME_DURATION = 1f / 30f
    private val JUMP_FRAME_DURATION = 1.2f / 90f

    //флак отправки/задержки статистки
    private var statsSent = false

    override fun show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        game.font.color = Color.BLACK

        //музыка на полную громкость
        game.setMusicTargetVolume(1.0f)

        //вычисление размеров объектов
        GROUND_LEVEL = Gdx.graphics.height * 0.02f
        ROVER_SIZE = Gdx.graphics.height * 0.5f
        OBSTACLE_SIZE = Gdx.graphics.height * 0.2f

        //создание рамки счета
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

        //нарезка файлов анимации на кадры
        runSheet = Texture("robot_run_sheet.png")
        runSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val runTmp = TextureRegion.split(runSheet, runSheet.width / 6, runSheet.height / 5)
        runAnimation = Animation(RUN_FRAME_DURATION, *runTmp.flatten().toTypedArray())

        jumpSheet = Texture("robot_jump_sheet.png")
        jumpSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val jumpTmp = TextureRegion.split(jumpSheet, jumpSheet.width / 10, jumpSheet.height / 9)
        jumpAnimation = Animation(JUMP_FRAME_DURATION, *jumpTmp.flatten().toTypedArray())

        //загрузка текстур препятствий и фона
        bagTexture = Texture("obstacle_bag.png")
        bagTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        coneTexture = Texture("obstacle_cone.png")
        coneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        backTexture = Texture("game_back_layer.png")
        midTexture = Texture("game_mid_layer.png")
        roadTexture = Texture("game_road_layer.png")
        frontTexture = Texture("game_front_layer.png")

        //установка фильтров для фона
        backTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        midTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        roadTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        frontTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        roverRect.width = ROVER_SIZE * 0.4f
        roverRect.height = ROVER_SIZE * 0.6f

        resetGame()
    }

    //игровой цикл
    override fun render(delta: Float) {
        //очистка экрана
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        when (gameState) {
            //идет игра
            0 -> {
                game.setMusicTargetVolume(1.0f)
                updateGame(delta)
            }
            //игра закончена
            1 -> {
                game.setMusicTargetVolume(0.5f)

                //отправка статистики
                if (game.currentUser != null && !statsSent) {
                    sendGameStats()
                }

                //работа кнопки ESC для выхода в меню
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                    game.setScreen(MenuScreen(game))
                    return
                }

                //обработка нажатий на экран
                if (Gdx.input.justTouched()) {
                    val touchX = Gdx.input.x.toFloat()
                    //инвертируем Y, т.к. input.y считается сверху, а координаты отрисовки снизу
                    val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

                    //координаты кнопки "Back to Menu" (должны совпадать с отрисовкой)
                    val menuButtonY = Gdx.graphics.height / 2f - 160f
                    val centerX = Gdx.graphics.width / 2f

                    //проверяем, попал ли клик в зону кнопки "back to Menu"
                    if (touchX > centerX - 200 && touchX < centerX + 200 &&
                        touchY > menuButtonY - 40 && touchY < menuButtonY + 40) {
                        game.setScreen(MenuScreen(game))
                    }
                    else {
                        //если кликнули в любом другом месте — перезапуск
                        resetGame()
                    }
                }
            }
            //пауза
            2 -> {
                game.setMusicTargetVolume(0.3f)
                if (Gdx.input.isKeyJustPressed(Input.Keys.P)) gameState = 0
            }
        }

        drawGame()
    }

    private fun sendGameStats() {
        //считаем время текущей сессии
        val sessionPlaytime = (TimeUtils.nanoTime() - startTime) / 1000000000L

        //сначала сохраняем в память телефона
        StatsManager.saveLocalResults(score.toInt(), sessionPlaytime.toInt())

        //пытаемся отправить
        StatsManager.trySync(game)

        statsSent = true
    }

    //логика игры
    private fun updateGame(dt: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            gameState = 2
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(MenuScreen(game))
            return
        }

        //движение фона
        backX -= SPEED_BACK * dt
        midX -= SPEED_MID * dt
        roadX -= SPEED_ROAD * dt
        frontX -= SPEED_FRONT * dt

        //если фон уехал полностью, его позиция сбрасывается
        if (backX <= -Gdx.graphics.width) backX = 0f
        if (midX <= -Gdx.graphics.width) midX = 0f
        if (roadX <= -Gdx.graphics.width) roadX = 0f
        if (frontX <= -Gdx.graphics.width) frontX = 0f

        //обновление таймеров анимации
        if (isJumping) jumpStateTime += dt else runStateTime += dt

        //подсчет очков
        score = (TimeUtils.nanoTime() - startTime) / 100000000

        //если нажата кнопка прыжка
        if ((Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && !isJumping) {
            roverVelocityY = JUMP_FORCE
            isJumping = true
            jumpStateTime = 0f
        }

        //меняется физика и задается и задается импульс прыжка
        roverVelocityY += GRAVITY * dt
        roverY += roverVelocityY * dt

        //проверка, не упал ли персонаж до пола
        if (roverY < GROUND_LEVEL) {
            roverY = GROUND_LEVEL
            roverVelocityY = 0f
            if (isJumping) {
                isJumping = false
                runStateTime = 0f
            }
        }

        //обновление позиции хитбокса
        roverRect.x = (Gdx.graphics.width * 0.1f) + (ROVER_SIZE * 0.3f)
        roverRect.y = roverY + (ROVER_SIZE * 0.1f)

        //спавн новых препятствий
        if (TimeUtils.nanoTime() > nextSpawnTime) {
            spawnObstacle()
            nextSpawnTime = TimeUtils.nanoTime() + MathUtils.random(1200000000L, 2500000000L)
        }

        //движение препятствий и проверка столкновений
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

    //метод создания нового препятствия
    private fun spawnObstacle() {
        val obstacleRect = Rectangle()
        obstacleRect.x = Gdx.graphics.width.toFloat()
        obstacleRect.y = GROUND_LEVEL
        obstacleRect.width = OBSTACLE_SIZE
        obstacleRect.height = OBSTACLE_SIZE
        val type = if (MathUtils.randomBoolean()) ObstacleType.BAG else ObstacleType.CONE
        obstacles.add(Obstacle(type, obstacleRect))
    }

    //метод отрисовки игры
    private fun drawGame() {
        game.batch.begin()

        //функция отрисовки бесконечного фона
        fun drawLayer(texture: Texture, x: Float) {
            game.batch.draw(texture, x, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            game.batch.draw(texture, x + Gdx.graphics.width, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        }

        //рисуем слои фона
        drawLayer(backTexture, backX)
        drawLayer(midTexture, midX)
        drawLayer(roadTexture, roadX)

        //если идет игра или активно меню паузы, то рисуем робота и препятствия
        if (gameState == 0 || gameState == 2) {
            val currentFrame = if (isJumping) jumpAnimation.getKeyFrame(jumpStateTime, false) else runAnimation.getKeyFrame(runStateTime, true)
            game.batch.draw(currentFrame, Gdx.graphics.width * 0.1f, roverY, ROVER_SIZE, ROVER_SIZE)

            for (obstacle in obstacles) {
                val textureToDraw = if (obstacle.type == ObstacleType.BAG) bagTexture else coneTexture
                game.batch.draw(textureToDraw, obstacle.rect.x, obstacle.rect.y, obstacle.rect.width, obstacle.rect.height)
            }
        }

        drawLayer(frontTexture, frontX)

        //вспомогательная функция для рисования панели
        fun drawPanel(x: Float, y: Float, w: Float, h: Float) {
            game.batch.setColor(1f, 1f, 1f, 0.6f)
            panelNinePatch.draw(game.batch, x, y, w, h)
            game.batch.setColor(1f, 1f, 1f, 1f)
        }

        //если игра или пауза, рисуем счет в углу
        if (gameState == 0 || gameState == 2) {
            drawPanel(20f, Gdx.graphics.height - 160f, 400f, 125f)
            game.font.draw(game.batch, "Score: $score", 50f, Gdx.graphics.height - 50f)
            game.font.draw(game.batch, "Passed: $obstaclesPassedCounter", 50f, Gdx.graphics.height - 110f)

            if (gameState == 2) {
                drawPanel(Gdx.graphics.width * 0.2f, Gdx.graphics.height * 0.4f, Gdx.graphics.width * 0.6f, 200f)
                game.font.draw(game.batch, "PAUSED", 0f, Gdx.graphics.height / 2f + 30f, Gdx.graphics.width.toFloat(), Align.center, false)
            }
        }
        //иначе отрисовка финального меню
        else if (gameState == 1) {
            drawPanel(Gdx.graphics.width * 0.15f, Gdx.graphics.height * 0.25f, Gdx.graphics.width * 0.7f, 500f)
            game.font.draw(game.batch, "GAME OVER!", 0f, Gdx.graphics.height / 2f + 140f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Score: $score", 0f, Gdx.graphics.height / 2f + 70f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Passed: $obstaclesPassedCounter", 0f, Gdx.graphics.height / 2f + 10f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Tap to restart", 0f, Gdx.graphics.height / 2f - 70f, Gdx.graphics.width.toFloat(), Align.center, false)
            game.font.draw(game.batch, "Back to Menu", 0f, Gdx.graphics.height / 2f - 160f, Gdx.graphics.width.toFloat(), Align.center, false)
        }
        game.batch.end()
    }

    //сброс параметров для начала новой игры
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
        statsSent = false
    }

    //при скрытии возвращаем громкость музыки
    override fun hide() {
        game.setMusicTargetVolume(0.3f)
    }

    //освобождение ресурсов
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
    }
}
