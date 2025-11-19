package ru.yandex.rover

import com.badlogic.gdx.Gdx

object StatsManager {
    private const val PREFS_NAME = "YandexRoverPrefs"
    private const val KEY_PENDING_PLAYTIME = "pending_playtime"
    private const val KEY_LOCAL_BEST_SCORE = "local_best_score"
    private const val KEY_USERNAME = "username"

    private val prefs = Gdx.app.getPreferences(PREFS_NAME)

    //сохраняем результаты игры локально (вызывается при Game Over)
    fun saveLocalResults(score: Int, playtimeSeconds: Int) {
        //накапливаем время (добавляем новое к уже сохраненному)
        val currentPendingTime = prefs.getInteger(KEY_PENDING_PLAYTIME, 0)
        prefs.putInteger(KEY_PENDING_PLAYTIME, currentPendingTime + playtimeSeconds)

        //проверяем, побит ли локальный рекорд
        val currentBestScore = prefs.getInteger(KEY_LOCAL_BEST_SCORE, 0)
        if (score > currentBestScore)
        {
            prefs.putInteger(KEY_LOCAL_BEST_SCORE, score)
        }

        prefs.flush() //принудительно записываем на диск
        Gdx.app.log("StatsManager", "Saved locally. Pending time: ${currentPendingTime + playtimeSeconds}, Best Score: ${prefs.getInteger(KEY_LOCAL_BEST_SCORE)}")
    }

    //пытаемся отправить данные на сервер (синхронизация)
    fun trySync(game: YandexRoverGame) {
        val username = game.currentUser?.username ?: prefs.getString(KEY_USERNAME, null)
        if (username == null) return

        val pendingPlaytime = prefs.getInteger(KEY_PENDING_PLAYTIME, 0)
        val localBestScore = prefs.getInteger(KEY_LOCAL_BEST_SCORE, 0)

        //если отправлять нечего (время 0 и очков нет), выходим
        if (pendingPlaytime == 0 && localBestScore == 0) return

        Gdx.app.log("StatsManager", "Syncing... Sending Time: $pendingPlaytime, Score: $localBestScore")

        ApiClient.updateStats(
            username,
            localBestScore,
            pendingPlaytime,
            object : ApiListener {
                override fun onSuccess(user: UserResponse) {
                    Gdx.app.log("StatsManager", "Sync successful!")

                    prefs.putInteger(KEY_PENDING_PLAYTIME, 0)

                    if (user.best_score > localBestScore) {
                        prefs.putInteger(KEY_LOCAL_BEST_SCORE, user.best_score)
                    }
                    prefs.flush()

                    game.currentUser = user
                }

                override fun onFailure(message: String) {
                    Gdx.app.error("StatsManager", "Sync failed: $message. Data kept locally.")
                }
            }
        )
    }

    //вспомогательный метод для обновления локальных данных при входе (login)
    fun updateLocalBestScore(serverBestScore: Int) {
        val localBest = prefs.getInteger(KEY_LOCAL_BEST_SCORE, 0)
        if (serverBestScore > localBest) {
            prefs.putInteger(KEY_LOCAL_BEST_SCORE, serverBestScore)
            prefs.flush()
        }
    }
}
