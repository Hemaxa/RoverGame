package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

// --- Схемы данных для отправки на сервер ---

data class RegistrationRequest(
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var password: String = ""
)

// --- НОВЫЙ data class для отправки статистики ---
// Должен точно соответствовать UserStatsUpdate на сервере
data class StatsRequest(
    @JvmField var username: String = "",
    @JvmField var score: Int = 0,           // Счет за текущую игру
    @JvmField var playtime_delta: Int = 0  // Время, проведенное в последней игре
)

// --- Схема данных для получения от сервера ---

data class UserResponse(
    @JvmField var id: Int = 0,
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var best_score: Int = 0,
    @JvmField var total_playtime: Int = 0
)

data class LoginRequest(
    @JvmField var username: String = "",
    @JvmField var password: String = ""
)

// --- Интерфейс для обработки ответов ---

interface ApiListener {
    // Вызывается при успешном получении данных
    fun onSuccess(user: UserResponse)
    // Вызывается при ошибке (сеть, сервер, парсинг)
    fun onFailure(message: String)
}

// --- Объект для сетевых операций ---

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000"

    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
    }

    // Объекты для реюза, чтобы избежать лишнего создания
    private val registrationData = RegistrationRequest()
    private val statsData = StatsRequest()
    private val loginData = LoginRequest()

    // --- ФУНКЦИЯ РЕГИСТРАЦИИ ---
    fun registerUser(username: String, displayName: String, password: String, listener: ApiListener) {
        registrationData.username = username
        registrationData.display_name = displayName
        registrationData.password = password
        val requestJsonString = json.toJson(registrationData)

        val request = Net.HttpRequest(Net.HttpMethods.POST)
        request.url = "$BASE_URL/register" // Эндпоинт регистрации
        request.setHeader("Content-Type", "application/json")
        request.setContent(requestJsonString)

        Gdx.net.sendHttpRequest(request, createHttpResponseListener(listener))
    }

    fun loginUser(username: String, password: String, listener: ApiListener) {
        loginData.username = username
        loginData.password = password

        val requestJsonString = json.toJson(loginData)

        val request = Net.HttpRequest(Net.HttpMethods.POST)
        request.url = "$BASE_URL/login"
        request.setHeader("Content-Type", "application/json")
        request.setContent(requestJsonString)

        Gdx.net.sendHttpRequest(request, createHttpResponseListener(listener))
    }

    // --- НОВАЯ ФУНКЦИЯ: ОБНОВЛЕНИЕ СТАТИСТИКИ ---
    fun updateStats(username: String, currentScore: Int, playedTime: Int, listener: ApiListener) {
        statsData.username = username
        statsData.score = currentScore
        statsData.playtime_delta = playedTime

        val requestJsonString = json.toJson(statsData)

        val request = Net.HttpRequest(Net.HttpMethods.POST)
        request.url = "$BASE_URL/update_stats" // НОВЫЙ эндпоинт
        request.setHeader("Content-Type", "application/json")
        request.setContent(requestJsonString)

        Gdx.net.sendHttpRequest(request, createHttpResponseListener(listener))
    }

    // Вспомогательная функция для обработки ответа (используется и для регистрации, и для статистики)
    private fun createHttpResponseListener(listener: ApiListener) = object : Net.HttpResponseListener {
        override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
            val statusCode = httpResponse.status.statusCode
            val responseString = httpResponse.getResultAsString()

            if (statusCode == 200) {
                try {
                    // Парсим ответ сервера в объект UserResponse
                    val user = json.fromJson(UserResponse::class.java, responseString)
                    // Вызываем onSuccess в потоке рендеринга LibGDX
                    Gdx.app.postRunnable { listener.onSuccess(user) }
                } catch (e: Exception) {
                    Gdx.app.postRunnable { listener.onFailure("Parse error: ${e.message}") }
                }
            } else {
                Gdx.app.postRunnable { listener.onFailure("Server error $statusCode: $responseString") }
            }
        }

        override fun failed(t: Throwable) {
            Gdx.app.postRunnable { listener.onFailure("Network error: ${t.message}") }
        }

        override fun cancelled() {
            Gdx.app.postRunnable { listener.onFailure("Cancelled") }
        }
    }
}
