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

//LoginScreen - класс формы входа в аккаунт
//ScreenAdapter — стандартная заготовка экрана в LibGDX
//ApiListener — интерфейс из ApiClient.kt
class LoginScreen(private val game: YandexRoverGame) : ScreenAdapter(), ApiListener {

    private lateinit var stage: Stage //контейнер для всех кнопок и полей ввода

    //элементы формы
    private lateinit var usernameField: TextField
    private lateinit var passwordField: TextField
    private lateinit var loginButton: TextButton
    private lateinit var statusLabel: Label

    //метод построения интерфейса
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

        passwordField = TextField("", skin)
        passwordField.isPasswordMode = true
        passwordField.setAlignment(Align.center)

        loginButton = TextButton("Log In", skin, "primary")

        statusLabel = Label("", skin)
        statusLabel.setWrap(true)
        statusLabel.setAlignment(Align.center)

        val registerBtn = TextButton("Create Account", skin)
        registerBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.screen = RegistrationScreen(game)
            }
        })

        val backBtn = TextButton("Back", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.screen = MenuScreen(game)
            }
        })

        //верстка таблицы
        //заголовок
        table.add(Label("AUTHORIZATION", skin, "header")).colspan(2).padBottom(50f).row()

        //поле логина
        table.add(Label("Username", skin)).colspan(2).padBottom(5f).row()
        table.add(usernameField).colspan(2).width(480f).height(65f).padBottom(15f).row()

        //поле пароля
        table.add(Label("Password", skin)).colspan(2).padBottom(5f).row()
        table.add(passwordField).colspan(2).width(480f).height(65f).padBottom(35f).row()

        //кнопки действий
        table.add(loginButton).colspan(2).width(450f).height(70f).padBottom(15f).row()
        table.add(registerBtn).colspan(2).width(450f).height(70f).padBottom(15f).row()
        table.add(backBtn).colspan(2).width(250f).height(70f).padBottom(15f).row()

        //статус
        table.add(statusLabel).colspan(2).width(600f).center()

        loginButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                login()
            }
        })
    }

    private fun login() {
        val username = usernameField.text
        val password = passwordField.text

        if (username.isBlank() || password.isBlank()) {
            statusLabel.color = Color.RED
            statusLabel.setText("Fields cannot be empty")
            return
        }

        statusLabel.color = Color.YELLOW
        statusLabel.setText("Logging in...")
        loginButton.isDisabled = true

        ApiClient.loginUser(username, password, this)
    }

    override fun onSuccess(user: UserResponse) {
        game.currentUser = user

        val prefs = Gdx.app.getPreferences("YandexRoverPrefs")
        prefs.putString("username", user.username)
        prefs.putString("password", passwordField.text)
        prefs.flush()

        //обновляем локальный рекорд данными с сервера
        StatsManager.updateLocalBestScore(user.best_score)

        statusLabel.color = Color.GREEN
        statusLabel.setText("Welcome back, ${user.display_name}!")
        game.screen = MenuScreen(game)
    }

    override fun onFailure(message: String) {
        statusLabel.color = Color.RED
        statusLabel.setText("Error: $message")
        loginButton.isDisabled = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.screen = MenuScreen(game)
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
