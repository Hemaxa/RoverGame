package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport

class MenuScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())

    private lateinit var playTexture: Texture
    private lateinit var statsTexture: Texture
    private lateinit var userTexture: Texture
    private lateinit var backgroundTexture: Texture

    override fun show() {
        Gdx.input.inputProcessor = stage

        // --- МУЗЫКА: Включаем тихий режим для меню ---
        game.setMusicTargetVolume(0.3f)
        // ---------------------------------------------

        try {
            backgroundTexture = Texture("menu_bg.png")
            val bgImage = Image(backgroundTexture)
            bgImage.setScaling(Scaling.fill)
            bgImage.setFillParent(true)
            stage.addActor(bgImage)
        } catch (e: Exception) {
            Gdx.app.error("MenuScreen", "Background not found")
        }

        playTexture = Texture("play_icon.png")
        statsTexture = Texture("leaderboard_icon.png")
        userTexture = Texture("user_icon.png")

        val playBtn = createButton(playTexture) { game.setScreen(GameScreen(game)) }
        val statsBtn = createButton(statsTexture) { game.setScreen(LeaderboardScreen(game)) }
        val userBtn = createButton(userTexture) { game.setScreen(LoginScreen(game)) }

        playBtn.addAction(Actions.forever(Actions.sequence(
            Actions.scaleTo(1.05f, 1.05f, 0.8f),
            Actions.scaleTo(1.0f, 1.0f, 0.8f)
        )))

        val labelStyle = Label.LabelStyle(game.font, Color.WHITE)
        val titleLabel = Label("YandexRoverRun", labelStyle)
        titleLabel.setFontScale(1.5f)

        val table = Table()
        table.setFillParent(true)

        table.add(titleLabel).colspan(3).padBottom(80f).row()
        table.add(statsBtn).size(150f).padRight(40f)
        table.add(playBtn).size(220f)
        table.add(userBtn).size(150f).padLeft(40f)

        stage.addActor(table)
    }

    private fun createButton(texture: Texture, onClick: () -> Unit): ImageButton {
        val style = ImageButton.ImageButtonStyle()
        style.up = TextureRegionDrawable(TextureRegion(texture))
        val btn = ImageButton(style)
        btn.setOrigin(Align.center)

        btn.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                btn.addAction(Actions.scaleTo(0.9f, 0.9f, 0.1f))
                return true
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                btn.addAction(Actions.sequence(
                    Actions.scaleTo(1.0f, 1.0f, 0.1f),
                    Actions.run { onClick() }
                ))
            }

            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                if (pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1.1f, 1.1f, 0.2f),
                        Actions.sequence(
                            Actions.color(Color(1f, 1f, 1f, 1f), 0f),
                            Actions.color(Color(1.5f, 1.5f, 1.5f, 1f), 0.2f),
                            Actions.color(Color.WHITE, 0.2f)
                        )
                    ))
                }
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                if (pointer == -1) {
                    btn.addAction(Actions.parallel(
                        Actions.scaleTo(1.0f, 1.0f, 0.2f),
                        Actions.color(Color.WHITE, 0.2f)
                    ))
                }
            }
        })
        return btn
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
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
        if (::backgroundTexture.isInitialized) backgroundTexture.dispose()
    }
}
