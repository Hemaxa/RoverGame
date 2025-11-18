package ru.yandex.rover

import com.badlogic.gdx.Gdx
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
        Gdx.input.inputProcessor = stage // Включаем управление UI

        val table = Table()
        table.setFillParent(true)
        table.center()
        stage.addActor(table)

        // Используем Skin из главного класса game
        val skin = game.skin

        // 1. Создаем поля
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

        // 2. Собираем форму
        table.add(Label("Yandex Rover Registration", skin, "title-white")).colspan(2).padBottom(30f).row()
        table.add(Label("Username:", skin)).right().pad(10f)
        table.add(usernameField).width(400f).pad(10f).row()

        table.add(Label("Display Name:", skin)).right().pad(10f)
        table.add(displayField).width(400f).pad(10f).row()

        table.add(Label("Password:", skin)).right().pad(10f)
        table.add(passwordField).width(400f).pad(10f).row()

        table.add(registerButton).colspan(2).padTop(20f).width(200f).row()
        table.add(statusLabel).colspan(2).padTop(10f)

        // 3. Добавляем логику кнопке
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

        statusLabel.color = Color.WHITE
        statusLabel.setText("Registering... please wait...")
        registerButton.isDisabled = true // Блокируем кнопку

        // Вызываем наш ApiClient и ждем ответа (this)
        ApiClient.registerUser(username, displayName, password, this)
    }

    // --- Ответы от ApiClient ---

    override fun onSuccess(user: UserResponse) {
        // Мы в главном потоке, т.к. ApiClient использовал postRunnable
        statusLabel.color = Color.GREEN
        statusLabel.setText("Success! Welcome, ${user.display_name}. Starting game...")

        // TODO: Тут можно сохранить токен или данные пользователя

        // Переходим на игровой экран
        game.setScreen(GameScreen(game))
    }

    override fun onFailure(message: String) {
        // Мы в главном потоке
        statusLabel.color = Color.RED
        statusLabel.setText("Error: $message")
        registerButton.isDisabled = false // Разблокируем кнопку
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null // Выключаем управление UI
    }

    override fun dispose() {
        stage.dispose() // Освобождаем ресурсы экрана
    }
}
