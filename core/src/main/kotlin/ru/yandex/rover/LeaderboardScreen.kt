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

//LeaderboardScreen - класс окна статистики
class LeaderboardScreen(private val game: YandexRoverGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())

    override fun show() {
        Gdx.input.inputProcessor = stage
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        game.setMusicTargetVolume(0.3f)

        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        val skin = game.skin

        //заголовок
        table.add(Label("STATISTICS", skin, "header")).padBottom(50f).colspan(2).row()

        val currentUser = game.currentUser

        if (currentUser != null) {
            //если пользователь вошел, показываем его данные
            addStatRow(table, skin, "Name:", currentUser.display_name)
            addStatRow(table, skin, "Username:", "@${currentUser.username}")
            addStatRow(table, skin, "Best Score:", currentUser.best_score.toString())

            val hours = currentUser.total_playtime / 3600
            val minutes = (currentUser.total_playtime % 3600) / 60
            addStatRow(table, skin, "Total Time:", "${hours}h ${minutes}m")

        }
        else {
            //если не вошел
            val errorLabel = Label("You are not logged in.", skin)
            errorLabel.color = Color.RED
            table.add(errorLabel).colspan(2).padBottom(20f).row()

            //кнопка логина
            val loginBtn = TextButton("Go to Login", skin)
            loginBtn.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    game.screen = LoginScreen(game)
                }
            })
            table.add(loginBtn).colspan(2).width(450f).height(70f).padBottom(15f).row()
        }

        //кнопка назад
        val backBtn = TextButton("Back", skin)
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                game.screen = MenuScreen(game)
            }
        })
        table.add(backBtn).colspan(2).width(250f).height(70f).padBottom(15f).row()
    }

    private fun addStatRow(table: Table, skin: com.badlogic.gdx.scenes.scene2d.ui.Skin, title: String, value: String) {
        table.add(Label(title, skin)).right().padRight(20f).padBottom(15f)
        val valueLabel = Label(value, skin)
        valueLabel.color = Color.CYAN
        table.add(valueLabel).left().padBottom(15f).row()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
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
