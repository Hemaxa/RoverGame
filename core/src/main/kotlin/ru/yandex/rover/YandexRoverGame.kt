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

//YandexRoverGame - главный класс приложения (точка входа)
class YandexRoverGame : Game() {

    lateinit var batch: SpriteBatch //объект для отрисовки 2D-графики (рендерит на видеокарте)
    lateinit var font: BitmapFont //объект обычного шрифта для всего приложения
    lateinit var headerFont: BitmapFont //объект шрифта для заголовков
    lateinit var skin: Skin //объект стиля интерфейса для всего приложения

    //глобальная переменная пользователя
    var currentUser: UserResponse? = null

    //глобальнаые переменные музыки
    private lateinit var bgMusic: Music
    private var currentVolume = 0f
    private var targetVolume = 0.3f

    //метод, вызываемый один раз при создании приложения
    override fun create() {
        //инициализация отрисовщика
        batch = SpriteBatch()

        //установка шрифта
        val generator = FreeTypeFontGenerator(Gdx.files.internal("font.woff"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()

        //генерация основного шрифта
        parameter.size = 48 //размер шрифта
        font = generator.generateFont(parameter)

        //генерирация шрифта для заголовков
        parameter.size = 96
        headerFont = generator.generateFont(parameter)

        generator.dispose()

        //определение стиля
        createBasicSkin()

        // запуск музыки
        try {
            bgMusic = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"))
            bgMusic.isLooping = true
            bgMusic.volume = 0f //начинаем с тишины и плавно нарастаем
            bgMusic.play()
        }
        catch (e: Exception) {
            //обработка ошибки отсутствия файла
            Gdx.app.error("MUSIC", "Error loading music.mp3", e)
        }

        //попытка входа в аккаунт (автовход)
        val prefs = Gdx.app.getPreferences("YandexRoverPrefs")
        val savedUser = prefs.getString("username", null)
        val savedPass = prefs.getString("password", null)

        //ссли есть данные в переменной пользователя, то пытаемся войти через сервер
        if (savedUser != null && savedPass != null) {
            ApiClient.loginUser(savedUser, savedPass, object : ApiListener {
                override fun onSuccess(user: UserResponse) {
                    currentUser = user
                    Gdx.app.log("Auth", "Auto-login successful: ${user.username}, Best Score: ${user.best_score}")
                }

                override fun onFailure(message: String) {
                    Gdx.app.error("Auth", "Auto-login failed: $message")
                }
            })
        }

        //запуск меню
        this.setScreen(MenuScreen(this))
    }

    //метод, который вызывается каждый кадр во всей игре
    override fun render() {
        super.render() //рисует текущий экран (вызывается у каждого текущего украна)

        //плавное изменение громкости музыки
        if (::bgMusic.isInitialized) {
            currentVolume = MathUtils.lerp(currentVolume, targetVolume, Gdx.graphics.deltaTime * 2f)
            bgMusic.volume = currentVolume
        }
    }

    //метод, вызываемый для смены громкости
    fun setMusicTargetVolume(volume: Float) {
        targetVolume = volume
    }

    //метод отрисовки интерфейса
    private fun createBasicSkin() {
        skin = Skin()

        //добавление шрифтов
        skin.add("default", font)
        skin.add("header", headerFont)

        //плашка статистики игры
        //генерируется текстура с закругленными углами
        val radius = 20
        val size = radius * 2 + 2
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)

        //отрисовка кругов по углам
        pixmap.fillCircle(radius, radius, radius)
        pixmap.fillCircle(size - radius - 1, radius, radius)
        pixmap.fillCircle(radius, size - radius - 1, radius)
        pixmap.fillCircle(size - radius - 1, size - radius - 1, radius)

        //превращение в texture
        pixmap.fillRectangle(radius, 0, size - 2 * radius, size)
        pixmap.fillRectangle(0, radius, size, size - 2 * radius)

        val roundedTexture = Texture(pixmap)
        pixmap.dispose()

        skin.add("rounded", com.badlogic.gdx.graphics.g2d.NinePatch(roundedTexture, radius, radius, radius, radius))

        //настройка стиля LABEL
        //обычный текст
        val labelStyle = Label.LabelStyle()
        labelStyle.font = skin.getFont("default")
        labelStyle.fontColor = Color.WHITE
        skin.add("default", labelStyle)

        //стиль заголовка
        val headerStyle = Label.LabelStyle(labelStyle)
        headerStyle.font = skin.getFont("header")
        headerStyle.fontColor = Color.valueOf("ec003f")
        skin.add("header", headerStyle)

        //настройка стиля TEXT BUTTON
        //обычная кнопка
        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.up = skin.newDrawable("rounded", Color.DARK_GRAY)
        textButtonStyle.down = skin.newDrawable("rounded", Color.BLACK)
        textButtonStyle.over = skin.newDrawable("rounded", Color.GRAY)
        textButtonStyle.font = skin.getFont("default")
        textButtonStyle.fontColor = Color.WHITE
        skin.add("default", textButtonStyle)

        //акцентная кнопка
        val primaryButtonStyle = TextButton.TextButtonStyle(textButtonStyle) // Копируем настройки обычного
        primaryButtonStyle.up = skin.newDrawable("rounded", Color.valueOf("ec003f"))
        primaryButtonStyle.down = skin.newDrawable("rounded", Color.valueOf("b0002f"))
        skin.add("primary", primaryButtonStyle)

        //настройка стиля TEXT FIELD
        val textFieldStyle = TextField.TextFieldStyle()
        textFieldStyle.font = skin.getFont("default")
        textFieldStyle.fontColor = Color.BLACK
        textFieldStyle.background = skin.newDrawable("rounded", Color.WHITE)

        val cursorPix = Pixmap(2, 20, Pixmap.Format.RGBA8888)
        cursorPix.setColor(Color.BLACK)
        cursorPix.fill()
        textFieldStyle.cursor = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(Texture(cursorPix)))

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
