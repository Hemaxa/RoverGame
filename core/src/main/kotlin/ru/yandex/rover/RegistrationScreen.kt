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
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

//RegistrationScreen - класс формы регистрации аккаунта
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

        //инициализация полей
        usernameField = TextField("", skin)
        usernameField.setAlignment(Align.center)

        displayField = TextField("", skin)
        displayField.setAlignment(Align.center)

        passwordField = TextField("", skin)
        passwordField.isPasswordMode = true
        passwordField.setAlignment(Align.center)

        registerButton = TextButton("Register", skin, "primary")

        statusLabel = Label("", skin)
        statusLabel.color = Color.RED
        statusLabel.setAlignment(Align.center)

        val loginBtn = TextButton("Go to Login", skin)
        loginBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.screen = LoginScreen(game)
            }
        })

        val backBtn = TextButton("Back", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.setScreen(MenuScreen(game))
            }
        })

        //верстка таблицы
        //заголовок
        table.add(Label("REGISTRATION", skin, "header")).colspan(2).padBottom(50f).row()

        //поле логина
        table.add(Label("Username", skin)).colspan(2).padBottom(5f).row()
        table.add(usernameField).colspan(2).width(480f).height(65f).padBottom(15f).row()

        //поле имени
        table.add(Label("Display Name", skin)).colspan(2).padBottom(5f).row()
        table.add(displayField).colspan(2).width(480f).height(65f).padBottom(15f).row()

        //поле пароля
        table.add(Label("Password", skin)).colspan(2).padBottom(5f).row()
        table.add(passwordField).colspan(2).width(480f).height(65f).padBottom(35f).row()

        //кнопки действий
        table.add(registerButton).colspan(2).width(450f).height(70f).padBottom(15f).row()
        table.add(loginBtn).colspan(2).width(450f).height(70f).padBottom(15f).row()
        table.add(backBtn).colspan(2).width(250f).height(70f).padBottom(15f).row()

        //статус
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
            statusLabel.setText("Fields cannot be empty")
            return
        }
        statusLabel.color = Color.YELLOW
        statusLabel.setText("Registering...")
        registerButton.isDisabled = true

        ApiClient.registerUser(username, displayName, password, this)
    }

    override fun onSuccess(user: UserResponse) {
        game.currentUser = user
        statusLabel.color = Color.GREEN
        statusLabel.setText("Success! Welcome ${user.display_name}")
    }

    override fun onFailure(message: String) {
        statusLabel.color = Color.RED
        statusLabel.setText("Error: $message")
        registerButton.isDisabled = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
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
