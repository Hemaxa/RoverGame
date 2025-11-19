package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

//MenuScreen - класс главного окна
class MenuScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())

    //переменные для хранения текстур (картинок)
    private lateinit var playTexture: Texture
    private lateinit var statsTexture: Texture
    private lateinit var userTexture: Texture
    private lateinit var logoTexture: Texture
    private var avatarTexture: Texture? = null

    override fun show() {
        Gdx.input.inputProcessor = stage

        game.setMusicTargetVolume(0.3f)

        //логотип
        try {
            logoTexture = Texture("menu_game_logo.png")
            logoTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        catch (e: Exception) {
            Gdx.app.error("MenuScreen", "Logo not found")
            logoTexture = Texture(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGB888) // Заглушка
        }
        val logoImage = Image(logoTexture)

        playTexture = Texture("menu_play_icon.png")
        statsTexture = Texture("menu_leaderboard_icon.png")
        userTexture = Texture("menu_user_icon.png")

        //иконки
        val playBtn = createButton(playTexture, 300f) { game.setScreen(GameScreen(game)) }
        val statsBtn = createButton(statsTexture, 200f) { game.setScreen(LeaderboardScreen(game)) }
        val userBtn = createButton(userTexture, 200f) { game.setScreen(LoginScreen(game)) }

        //анимация пульсации для кнопки play
        playBtn.addAction(Actions.forever(Actions.sequence(
            Actions.scaleTo(1.10f, 1.10f, 0.6f),
            Actions.scaleTo(1.0f, 1.0f, 0.6f)
        )))

        //создание таблицы
        val table = Table()
        table.setFillParent(true)

        table.add(logoImage).colspan(3).padBottom(100f).width(1600f).height(200f).row()
        table.add(statsBtn).size(200f).padRight(30f)
        table.add(playBtn).size(300f)
        table.add(userBtn).size(200f).padLeft(30f)

        stage.addActor(table)

        //аватарка пользователя
        if (game.currentUser != null) {
            try {
                // 1. Загружаем исходную квадратную картинку
                val rawAvatar = Texture("menu_avatar_icon.png")

                // 2. Превращаем её в круглую с черной рамкой (толщина рамки 5px)
                // Результат сохраняем в переменную класса avatarTexture, чтобы потом очистить в dispose()
                avatarTexture = createCircularTextureWithBorder(rawAvatar, 8, Color.BLACK)

                // Исходная "сырая" текстура больше не нужна, так как мы создали новую
                rawAvatar.dispose()

                val avatarImage = Image(avatarTexture)

                val uiTable = Table()
                uiTable.setFillParent(true)
                uiTable.top().right()

                // 3. Увеличиваем размер: было 80f -> стало 120f
                // pad(20f) — отступ от краев экрана
                uiTable.add(avatarImage).size(120f).pad(20f)

                stage.addActor(uiTable)
            }
            catch (e: Exception) {
                Gdx.app.error("MenuScreen", "Avatar icon error: ${e.message}")
            }
        }
    }

    //функция создания кнопки
    private fun createButton(texture: Texture, size: Float, onClick: () -> Unit): ImageButton {
        val style = ImageButton.ImageButtonStyle()
        style.up = TextureRegionDrawable(TextureRegion(texture))
        val btn = ImageButton(style)

        //задаем конечный размер кнопки
        btn.setSize(size, size)

        //вычисляем точку вращения/масштабирования (центр этого размера)
        btn.setOrigin(Align.center)

        //разрешаем трансформации
        btn.setTransform(true)

        btn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                //эффект нажатия: чуть уменьшаем и запускаем действие
                btn.addAction(Actions.sequence(
                    Actions.scaleTo(0.8f, 0.8f, 0.1f),
                    Actions.scaleTo(1.0f, 1.0f, 0.1f),
                    Actions.run { onClick() }
                ))
            }
        })
        return btn
    }

    //функция для создания круглой текстуры с рамкой
    private fun createCircularTextureWithBorder(original: Texture, borderThickness: Int, borderColor: Color): Texture {
        if (!original.textureData.isPrepared) {
            original.textureData.prepare()
        }
        val sourcePixmap = original.textureData.consumePixmap()

        val size = Math.min(sourcePixmap.width, sourcePixmap.height)
        val resultPixmap = com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)

        val radius = size / 2
        val centerX = size / 2
        val centerY = size / 2
        val radiusSquared = radius * radius
        val innerRadiusSquared = (radius - borderThickness) * (radius - borderThickness)

        //попиксельный проход для вырезания круга
        for (x in 0 until size) {
            for (y in 0 until size) {
                val dx = x - centerX
                val dy = y - centerY
                val distSquared = dx * dx + dy * dy

                if (distSquared <= innerRadiusSquared) {
                    resultPixmap.drawPixel(x, y, sourcePixmap.getPixel(x, y))
                }
                else if (distSquared <= radiusSquared) {
                    resultPixmap.drawPixel(x, y, Color.rgba8888(borderColor))
                }
            }
        }

        val resultTexture = Texture(resultPixmap)

        //очищаем память
        resultPixmap.dispose()

        return resultTexture
    }

    override fun render(delta: Float) {
        val bgColor = Color.valueOf("dff2fe")
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        playTexture.dispose()
        statsTexture.dispose()
        userTexture.dispose()
        if (::logoTexture.isInitialized) logoTexture.dispose()
        avatarTexture?.dispose()
    }
}
