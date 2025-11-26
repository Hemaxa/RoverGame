package ru.yandex.rover

import com.badlogic.gdx.Gdx

//LocalStatsSource - класс, который отвечает за сохранение и чтение данных на устройстве
class LocalStatsSource {
    private val PREFS_NAME = "YandexRoverPrefs"

    //ключи для сохранения данных, по которым ищутся ячейки
    private val KEY_PENDING_PLAYTIME = "pending_playtime"
    private val KEY_LOCAL_BEST_SCORE = "local_best_score"
    private val KEY_USERNAME = "username"

    //получение доступа к хранилищу настроек LibGDX
    private val prefs = Gdx.app.getPreferences(PREFS_NAME)

    //методы чтения настроек
    fun getPendingPlaytime(): Int {
        return prefs.getInteger(KEY_PENDING_PLAYTIME, 0)
    }

    fun getLocalBestScore(): Int {
        return prefs.getInteger(KEY_LOCAL_BEST_SCORE, 0)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    //методы записи настроек
    //добавляется новое время игры к уже накопленному
    fun addPendingPlaytime(seconds: Int) {
        val current = getPendingPlaytime()
        prefs.putInteger(KEY_PENDING_PLAYTIME, current + seconds)
        prefs.flush()
    }

    //сбрасывается накопленное время после успешной отправки на сервер
    fun resetPendingPlaytime() {
        prefs.putInteger(KEY_PENDING_PLAYTIME, 0)
        prefs.flush()
    }

    //обновляется локальный рекорд, только если новый счет выше
    fun updateBestScore(score: Int) {
        if (score > getLocalBestScore()) {
            prefs.putInteger(KEY_LOCAL_BEST_SCORE, score)
            prefs.flush()
        }
    }

    //сохранение имени пользователя
    fun saveUsername(username: String) {
        prefs.putString(KEY_USERNAME, username)
        prefs.flush()
    }
}
