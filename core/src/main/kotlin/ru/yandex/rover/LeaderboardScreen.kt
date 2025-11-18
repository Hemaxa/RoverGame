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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

class LeaderboardScreen(private val game: YandexRoverGame) : ScreenAdapter() {
    private val stage = Stage(ScreenViewport())

    override fun show() {
        Gdx.input.inputProcessor = stage
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        game.setMusicTargetVolume(0.3f)

        val table = Table()
        table.setFillParent(true)
        // table.debug() // Раскомментируй, чтобы видеть границы таблицы при отладке
        stage.addActor(table)

        val skin = game.skin

        // Заголовок
        table.add(Label("STATISTICS", skin, "header")).padBottom(50f).colspan(2).row()

        val currentUser = game.currentUser

        if (currentUser != null) {
            // Если пользователь вошел, показываем его данные
            addStatRow(table, skin, "Name:", currentUser.display_name)
            addStatRow(table, skin, "Username:", "@${currentUser.username}")
            addStatRow(table, skin, "Best Score:", currentUser.best_score.toString())

            // Переводим секунды в часы/минуты
            val hours = currentUser.total_playtime / 3600
            val minutes = (currentUser.total_playtime % 3600) / 60
            addStatRow(table, skin, "Total Time:", "${hours}h ${minutes}m")

        } else {
            // Если не вошел
            val errorLabel = Label("You are not logged in.", skin)
            errorLabel.color = Color.RED
            table.add(errorLabel).colspan(2).padBottom(20f).row()

            val loginBtn = TextButton("Go to Login", skin)
            loginBtn.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    game.screen = LoginScreen(game)
                }
            })
            table.add(loginBtn).colspan(2).width(300f).height(60f).row()
        }

        // Кнопка назад
        val backBtn = TextButton("<< BACK", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.screen = MenuScreen(game)
            }
        })
        table.add(backBtn).padTop(50f).colspan(2).width(250f).height(60f)
    }

    // Вспомогательная функция для красивого добавления строк
    private fun addStatRow(table: Table, skin: com.badlogic.gdx.scenes.scene2d.ui.Skin, title: String, value: String) {
        table.add(Label(title, skin)).right().padRight(20f).padBottom(10f)
        val valueLabel = Label(value, skin)
        valueLabel.color = Color.CYAN // Значения подсветим голубым
        table.add(valueLabel).left().padBottom(10f).row()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f) // Темно-синий фон, приятнее глазу
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.screen = MenuScreen(game)
        }

        stage.act()
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
    }
}
