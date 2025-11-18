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

class LoginScreen(private val game: YandexRoverGame) : ScreenAdapter(), ApiListener {

    private lateinit var stage: Stage
    private lateinit var usernameField: TextField
    private lateinit var passwordField: TextField
    private lateinit var loginButton: TextButton
    private lateinit var statusLabel: Label

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        val table = Table()
        table.setFillParent(true)
        table.center()
        stage.addActor(table)

        val skin = game.skin

        // Поля ввода
        usernameField = TextField("", skin)
        usernameField.messageText = "Username"
        // Исправлено: используем setAlignment вместо присваивания свойства
        usernameField.setAlignment(com.badlogic.gdx.utils.Align.center)

        passwordField = TextField("", skin)
        passwordField.messageText = "Password"
        passwordField.isPasswordMode = true
        // Исправлено: используем setAlignment вместо присваивания свойства
        passwordField.setAlignment(com.badlogic.gdx.utils.Align.center)

        loginButton = TextButton("LOG IN", skin)
        statusLabel = Label("", skin)
        statusLabel.setWrap(true) // Чтобы длинный текст ошибки переносился
        // Исправлено: используем setAlignment вместо присваивания свойства
        statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center)

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

        // --- Верстка таблицы ---

        // 1. Заголовок (используем стиль header, который добавили в createBasicSkin)
        table.add(Label("AUTHORIZATION", skin, "header")).colspan(2).padBottom(40f).row()

        // 2. Поля ввода (делаем их широкими и высокими)
        table.add(usernameField).colspan(2).width(500f).height(70f).padBottom(20f).row()
        table.add(passwordField).colspan(2).width(500f).height(70f).padBottom(30f).row()

        // 3. Основная кнопка (самая большая)
        table.add(loginButton).colspan(2).width(300f).height(80f).padBottom(20f).row()

        // 4. Дополнительные кнопки
        table.add(registerBtn).colspan(2).width(250f).height(60f).padBottom(10f).row()
        table.add(backBtn).colspan(2).width(200f).height(50f).padBottom(20f).row()

        // 5. Статус (растягиваем по ширине, чтобы текст центрировался)
        table.add(statusLabel).colspan(2).width(600f).center()

        // ... листенер кнопки loginButton остается прежним ...
        loginButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                performLogin()
            }
        })
    }

    private fun performLogin() {
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

        // Отправляем запрос
        ApiClient.loginUser(username, password, this)
    }

    override fun onSuccess(user: UserResponse) {
        // 1. Сохраняем пользователя в игру (оперативная память)
        game.currentUser = user

        // 2. Сохраняем данные в память телефона (чтобы не вводить каждый раз)
        val prefs = Gdx.app.getPreferences("YandexRoverPrefs")
        prefs.putString("username", user.username)
        // Внимание: хранить пароль в открытом виде небезопасно в продакшене,
        // но для учебного проекта допустимо. В реальности используют токены.
        prefs.putString("password", passwordField.text)
        prefs.flush()

        statusLabel.color = Color.GREEN
        statusLabel.setText("Welcome back, ${user.display_name}!")

        // Переходим в меню через секунду или сразу
        game.screen = MenuScreen(game)
    }

    override fun onFailure(message: String) {
        statusLabel.color = Color.RED
        statusLabel.setText("Error: $message")
        loginButton.isDisabled = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
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
