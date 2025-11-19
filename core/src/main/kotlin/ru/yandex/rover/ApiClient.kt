//ApiClient - сетевой клиент для общения с Python-сервером

package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

//DTO (Data Transfer Objects) - классы для хранения данных запросов, которые автоматически превразаются в JSON библиотекой LibGDX
data class RegistrationRequest(
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var password: String = ""
)

data class LoginRequest(
    @JvmField var username: String = "",
    @JvmField var password: String = ""
)

data class StatsRequest(
    @JvmField var username: String = "",
    @JvmField var score: Int = 0,
    @JvmField var playtime_delta: Int = 0
)

data class UserResponse(
    @JvmField var id: Int = 0,
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var best_score: Int = 0,
    @JvmField var total_playtime: Int = 0
)

//интерфейс для получения результата (Callback)
interface ApiListener {
    //вызывается при успешном получении данных
    fun onSuccess(user: UserResponse)
    //вызывается при ошибке
    fun onFailure(message: String)
}

//объект для сетевых операций
object ApiClient {
    //адрес сервера (для эмулятора Android localhost - это 10.0.2.2)
    private const val BASE_URL = "http://10.0.2.2:8000"

    //создание JSON для взаимодействия
    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
    }

    //создание переиспользуемых объектов
    private val registrationData = RegistrationRequest()
    private val loginData = LoginRequest()
    private val statsData = StatsRequest()

    //функция регистрации
    fun registerUser(username: String, displayName: String, password: String, listener: ApiListener) {
        registrationData.username = username
        registrationData.display_name = displayName
        registrationData.password = password

        val requestJsonString = json.toJson(registrationData)

        val request = Net.HttpRequest(Net.HttpMethods.POST)
        request.url = "$BASE_URL/register" // эндпоинт регистрации
        request.setHeader("Content-Type", "application/json")
        request.setContent(requestJsonString)

        Gdx.net.sendHttpRequest(request, createHttpResponseListener(listener))
    }

    //функция входа
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

    //функция обновления статистики
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

    //вспомогательная функция для обработки ответа (используется и для регистрации, и для статистики)
    private fun createHttpResponseListener(listener: ApiListener) = object : Net.HttpResponseListener {
        override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
            val statusCode = httpResponse.status.statusCode
            val responseString = httpResponse.getResultAsString()

            if (statusCode == 200) {
                try {
                    //парсим ответ сервера в объект UserResponse
                    val user = json.fromJson(UserResponse::class.java, responseString)
                    //вызываем onSuccess в потоке рендеринга LibGDX
                    Gdx.app.postRunnable { listener.onSuccess(user) }
                } catch (e: Exception) {
                    Gdx.app.postRunnable { listener.onFailure("Parse error: ${e.message}") }
                }
            }
            else {
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
