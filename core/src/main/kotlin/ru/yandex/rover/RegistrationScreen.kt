package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

class RegistrationScreen(private val game: YandexRoverGame) : ScreenAdapter(), ApiListener {

    private lateinit var stage: Stage
    private lateinit var usernameField: TextField
    private lateinit var displayField: TextField
    private lateinit var passwordField: TextField
    private lateinit var registerButton: TextButton
    private lateinit var statusLabel: Label

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        game.setMusicTargetVolume(0.3f)

        val table = Table()
        table.setFillParent(true)
        table.center()
        stage.addActor(table)

        val skin = game.skin

        usernameField = TextField("", skin)
        usernameField.messageText = "login"
        displayField = TextField("", skin)
        displayField.messageText = "display name"
        passwordField = TextField("", skin)
        passwordField.messageText = "password"
        passwordField.isPasswordMode = true
        registerButton = TextButton("Register", skin)
        statusLabel = Label("", skin)
        statusLabel.color = Color.RED

        // Кнопка назад
        val backBtn = TextButton("Back", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.setScreen(MenuScreen(game))
            }
        })

        table.add(Label("Registration", skin)).colspan(2).padBottom(30f).row()

        table.add(Label("User:", skin)).right().pad(10f)
        table.add(usernameField).width(400f).pad(10f).row()

        table.add(Label("Name:", skin)).right().pad(10f)
        table.add(displayField).width(400f).pad(10f).row()

        table.add(Label("Pass:", skin)).right().pad(10f)
        table.add(passwordField).width(400f).pad(10f).row()

        table.add(registerButton).colspan(2).padTop(20f).width(200f).height(60f).row()
        table.add(backBtn).colspan(2).padTop(10f).width(200f).height(60f).row()
        table.add(statusLabel).colspan(2).padTop(10f)

        registerButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                register()
            }
        })
    }

    private fun register() {
        val username = usernameField.text
        val displayName = displayField.text
        val password = passwordField.text

        if (username.isBlank() || displayName.isBlank() || password.isBlank()) {
            statusLabel.setText("All fields are required.")
            return
        }
        statusLabel.color = Color.YELLOW
        statusLabel.setText("Registering...")
        registerButton.isDisabled = true
        ApiClient.registerUser(username, displayName, password, this)
    }

    override fun onSuccess(user: UserResponse) {
        // --- ИЗМЕНЕНИЕ: Сохраняем данные пользователя ---
        game.currentUser = user
        // ---------------------------------------------
        statusLabel.color = Color.GREEN
        statusLabel.setText("Success! Welcome ${user.display_name}")
        // game.setScreen(MenuScreen(game)) // Можете оставить эту строку, чтобы сразу перейти в меню
    }

    override fun onFailure(message: String) {
        statusLabel.color = Color.RED
        statusLabel.setText("Error: $message")
        registerButton.isDisabled = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(MenuScreen(game))
        }

        stage.act(delta)
        stage.draw()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        stage.dispose()
    }
}
