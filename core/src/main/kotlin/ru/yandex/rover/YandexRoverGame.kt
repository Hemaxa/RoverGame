package ru.yandex.rover

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator // <<< ИМПОРТ
import com.badlogic.gdx.scenes.scene2d.ui.Skin

class YandexRoverGame : Game() {

    // Эти объекты будут общими для всех экранов
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var skin: Skin

    override fun create() {
        batch = SpriteBatch()

        // --- ГЕНЕРИРУЕМ ЧЕТКИЙ ШРИФТ ---
        val generator = FreeTypeFontGenerator(Gdx.files.internal("font.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 64 // Задаем нужный размер в пикселях
        parameter.borderWidth = 2f // Легкая обводка для четкости
        font = generator.generateFont(parameter) // Генерируем
        generator.dispose() // Освобождаем генератор
        // --- КОНЕЦ ГЕНЕРАЦИИ ---


        // --- ВРЕМЕННО ОТКЛЮЧАЕМ ЗАГРУЗКУ СКИНА ---
        /* // Загружаем Skin для UI (кнопки, поля ввода)
        // Убедись, что эти файлы есть в android/assets!
        try {
            skin = Skin(Gdx.files.internal("uiskin.json"))
        } catch (e: Exception) {
            Gdx.app.error("SKIN_ERROR", "Could not load uiskin.json. Make sure skin files are in assets folder.", e)
            Gdx.app.exit() // Выходим, если UI загрузить не удалось
        }
        */ // --- КОНЕЦ ВРЕМЕННОГО ОТКЛЮЧЕНИЯ ---

        // Запускаем не игру, а СРАЗУ ИГРОВОЙ ЭКРАН
        this.setScreen(GameScreen(this))
    }

    override fun dispose() {
        // Освобождаем общие ресурсы
        batch.dispose()
        font.dispose()
        // skin.dispose() // Раскомментируешь, когда скин заработает
    }
}
