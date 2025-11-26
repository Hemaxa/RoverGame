package ru.yandex.rover

//StatsManager - класс, который предосталяет интерфейс для остальных экранов приложения
object StatsManager {
    //инициализация
    private val localSource = LocalStatsSource()
    private val repository = StatsRepository(localSource)

    //методы вызываемые из самой игры
    fun saveLocalResults(score: Int, playtimeSeconds: Int) {
        repository.saveSessionResults(score, playtimeSeconds)
    }

    fun trySync(game: YandexRoverGame) {
        repository.syncData(game.currentUser) { updatedUser ->
            game.currentUser = updatedUser
        }
    }

    fun updateLocalBestScore(serverUser: UserResponse) {
        repository.processLoginData(serverUser)
    }
}
