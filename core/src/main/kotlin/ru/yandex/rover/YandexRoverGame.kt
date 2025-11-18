package ru.yandex.rover

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField

class YandexRoverGame : Game() {

    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var skin: Skin

    var currentUser: UserResponse? = null

    // --- МУЗЫКА (Глобальная) ---
    private lateinit var bgMusic: Music
    private var currentVolume = 0f
    private var targetVolume = 0.3f // По умолчанию тихо (для меню)

    override fun create() {
        batch = SpriteBatch()

        // 1. Шрифт
        val generator = FreeTypeFontGenerator(Gdx.files.internal("font.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 48
        parameter.borderWidth = 2f
        parameter.borderColor = Color.BLACK
        font = generator.generateFont(parameter)
        generator.dispose()

        // 2. Скин
        createBasicSkin()

        // 3. Запуск музыки (ОДИН РАЗ)
        try {
            bgMusic = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"))
            bgMusic.isLooping = true
            bgMusic.volume = 0f // Начинаем с тишины и плавно нарастаем
            bgMusic.play()
        } catch (e: Exception) {
            Gdx.app.error("MUSIC", "Error loading music.mp3", e)
        }

        val prefs = Gdx.app.getPreferences("YandexRoverPrefs")
        val savedUser = prefs.getString("username", null)
        val savedPass = prefs.getString("password", null)

        if (savedUser != null && savedPass != null) {
            // Пробуем войти тихо
            ApiClient.loginUser(savedUser, savedPass, object : ApiListener {
                override fun onSuccess(user: UserResponse) {
                    currentUser = user
                    Gdx.app.log("Auth", "Auto-login successful: ${user.username}, Best Score: ${user.best_score}")
                }

                override fun onFailure(message: String) {
                    Gdx.app.error("Auth", "Auto-login failed: $message")
                    // Если пароль сменился или ошибка, можно очистить префы
                    // prefs.clear(); prefs.flush()
                }
            })
        }

        // Запускаем меню
        this.setScreen(MenuScreen(this))
    }

    // Метод, который вызывается каждый кадр во всей игре
    override fun render() {
        super.render() // Рисует текущий экран

        // Плавное изменение громкости
        if (::bgMusic.isInitialized) {
            currentVolume = MathUtils.lerp(currentVolume, targetVolume, Gdx.graphics.deltaTime * 2f)
            bgMusic.volume = currentVolume
        }
    }

    // Экраны будут вызывать этот метод, чтобы менять громкость
    fun setMusicTargetVolume(volume: Float) {
        targetVolume = volume
    }

    private fun createBasicSkin() {
        skin = Skin()
        skin.add("default", font)

        // 1. Генерируем текстуру с закругленными углами (Round Rect)
        // Это позволит кнопкам и полям быть любого размера, сохраняя красивые углы.
        val radius = 20
        val size = radius * 2 + 2
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)

        // Рисуем круги по углам
        pixmap.fillCircle(radius, radius, radius)
        pixmap.fillCircle(size - radius - 1, radius, radius)
        pixmap.fillCircle(radius, size - radius - 1, radius)
        pixmap.fillCircle(size - radius - 1, size - radius - 1, radius)

        // Заполняем пространство между ними
        pixmap.fillRectangle(radius, 0, size - 2 * radius, size)
        pixmap.fillRectangle(0, radius, size, size - 2 * radius)

        val roundedTexture = Texture(pixmap)
        pixmap.dispose()

        // Создаем Drawable на основе NinePatch (чтобы тянулось без искажений углов)
        // Отступы (left, right, top, bottom) равны радиусу
        skin.add("rounded", com.badlogic.gdx.graphics.g2d.NinePatch(roundedTexture, radius, radius, radius, radius))


        // 2. Настраиваем стиль LABEL
        val labelStyle = Label.LabelStyle()
        labelStyle.font = skin.getFont("default")
        labelStyle.fontColor = Color.WHITE // По умолчанию белый текст
        skin.add("default", labelStyle)
        // Добавим стиль для заголовков (желтый)
        val headerStyle = Label.LabelStyle(labelStyle)
        headerStyle.fontColor = Color.YELLOW
        skin.add("header", headerStyle)


        // 3. Настраиваем стиль TEXT BUTTON (Кнопка)
        val textButtonStyle = TextButton.TextButtonStyle()
        // Обычное состояние - закругленный белый, но немного прозрачный или серый
        textButtonStyle.up = skin.newDrawable("rounded", Color.DARK_GRAY)
        // Нажатое состояние - потемнее
        textButtonStyle.down = skin.newDrawable("rounded", Color.BLACK)
        // Состояние "мышь наведена" (если на ПК) или фокус
        textButtonStyle.over = skin.newDrawable("rounded", Color.GRAY)
        // Текст
        textButtonStyle.font = skin.getFont("default")
        textButtonStyle.fontColor = Color.WHITE
        skin.add("default", textButtonStyle)


        // 4. Настраиваем стиль TEXT FIELD (Поле ввода)
        val textFieldStyle = TextField.TextFieldStyle()
        textFieldStyle.font = skin.getFont("default")
        textFieldStyle.fontColor = Color.BLACK
        // Фон поля - белый закругленный
        textFieldStyle.background = skin.newDrawable("rounded", Color.WHITE)
        // Курсор
        val cursorPix = Pixmap(2, 20, Pixmap.Format.RGBA8888)
        cursorPix.setColor(Color.BLACK)
        cursorPix.fill()
        textFieldStyle.cursor = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(Texture(cursorPix)))

        // Выделение текста
        val selectionPix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        selectionPix.setColor(Color.CYAN)
        selectionPix.fill()
        textFieldStyle.selection = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(Texture(selectionPix)))

        skin.add("default", textFieldStyle)
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        skin.dispose()
        if (::bgMusic.isInitialized) {
            bgMusic.dispose()
        }
    }
}
