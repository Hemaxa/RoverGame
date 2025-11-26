package ru.yandex.rover

import com.badlogic.gdx.Gdx

//StatsRepository - класс, который решает, откуда брать данные (LocalSource или Server) и куда их складывать
class StatsRepository(private val localSource: LocalStatsSource) {

    //сохранение результата сессии (вызывается при Game Over)
    fun saveSessionResults(score: Int, playtimeSeconds: Int) {
        //сначала всегда сохранение локально, чтобы данные не пропали
        localSource.addPendingPlaytime(playtimeSeconds)
        localSource.updateBestScore(score)

        Gdx.app.log("StatsRepository", "Saved locally. Pending time: ${localSource.getPendingPlaytime()}, Best Score: ${localSource.getLocalBestScore()}")
    }

    //логика синхронизации с сервером
    //onSyncComplete - это функция обратного вызова (callback), которая сработает при успехе
    fun syncData(currentUser: UserResponse?, onSyncComplete: (UserResponse) -> Unit) {
        //определяем имя пользователя (либо текущий залогиненный, либо сохраненный локально)
        val username = currentUser?.username ?: localSource.getUsername()

        if (username == null) {
            Gdx.app.log("StatsRepository", "Skipping sync: No username found")
            return
        }

        //берем накопленные данные из локального источника
        val pendingTime = localSource.getPendingPlaytime()
        val localBestScore = localSource.getLocalBestScore()

        //если отправлять нечего (время 0 и очков нет), выходим
        if (pendingTime == 0 && localBestScore == 0) return

        Gdx.app.log("StatsRepository", "Syncing... Sending Time: $pendingTime, Score: $localBestScore")

        //отправляем через ApiClient (Remote Data Source)
        ApiClient.updateStats(
            username,
            localBestScore,
            pendingTime,
            object : ApiListener {
                override fun onSuccess(user: UserResponse) {
                    Gdx.app.log("StatsRepository", "Sync successful!")

                    //при успехе очищаем накопленное время в локальном хранилище, чтобы не отправить его дважды
                    localSource.resetPendingPlaytime()

                    //если на сервер рекорд оказался выше (играли с другого телефона), обновляем локальный
                    if (user.best_score > localBestScore) {
                        localSource.updateBestScore(user.best_score)
                    }

                    //возвращаем обновленного пользователя в игру через callback
                    onSyncComplete(user)
                }

                override fun onFailure(message: String) {
                    //при ошибке данные остаются в LocalSource (pendingTime не сбрасывается)
                    Gdx.app.error("StatsRepository", "Sync failed: $message. Data kept locally.")
                }
            }
        )
    }

    //обработка данных при входе в аккаунт
    fun processLoginData(serverUser: UserResponse) {
        //сохраняем имя пользователя и подтягиваем рекорд с сервера
        localSource.saveUsername(serverUser.username)
        localSource.updateBestScore(serverUser.best_score)
    }
}
