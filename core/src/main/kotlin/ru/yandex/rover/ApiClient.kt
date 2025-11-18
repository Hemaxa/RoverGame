package ru.yandex.rover

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Json

// --- Data-классы для общения с сервером ---
// Они должны соответствовать твоим Pydantic-схемам

// 1. То, что мы ОТПРАВЛЯЕМ (схема UserCreate)
// @JvmField нужен, чтобы libGDX's Json мог работать с Kotlin data class
data class RegistrationRequest(
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var password: String = ""
)

// 2. То, что мы ПОЛУЧАЕМ (схема User)
data class UserResponse(
    @JvmField var id: Int = 0,
    @JvmField var username: String = "",
    @JvmField var display_name: String = "",
    @JvmField var best_score: Int = 0,
    @JvmField var total_playtime: Int = 0
)

// --- Интерфейс для колбэков (ответов от сервера) ---
interface ApiListener {
    fun onSuccess(user: UserResponse)
    fun onFailure(message: String)
}

// --- Наш главный HTTP-клиент (синглтон) ---
object ApiClient {

    // !!! ЗАМЕНИ ЭТОТ IP, ЕСЛИ НУЖНО (10.0.2.2 для эмулятора) !!!
    private const val BASE_URL = "http://10.0.2.2:8000"

    private val json = Json()
    private val requestData = RegistrationRequest() // Переиспользуем объект

    fun registerUser(username: String, displayName: String, password: String, listener: ApiListener) {

        // 1. Готовим данные для отправки
        requestData.username = username
        requestData.display_name = displayName
        requestData.password = password
        val requestJsonString = json.toJson(requestData)

        // 2. Создаем HTTP-запрос
        val request = Net.HttpRequest(Net.HttpMethods.POST)
        request.url = "$BASE_URL/register"
        request.setHeader("Content-Type", "application/json")
        request.setContent(requestJsonString)

        // 3. Асинхронно отправляем запрос
        Gdx.net.sendHttpRequest(request, object : Net.HttpResponseListener {

            override fun handleHttpResponse(httpResponse: Net.HttpResponse) {
                val statusCode = httpResponse.status.statusCode
                val responseString = httpResponse.getResultAsString()

                if (statusCode == 200) {
                    // УСПЕХ!
                    try {
                        val user = json.fromJson(UserResponse::class.java, responseString)
                        // ВАЖНО: Выполняем колбэк в главном потоке libGDX
                        Gdx.app.postRunnable { listener.onSuccess(user) }
                    } catch (e: Exception) {
                        Gdx.app.postRunnable { listener.onFailure("Failed to parse response: ${e.message}") }
                    }
                } else {
                    // ОШИБКА (например, 400 "Username already registered")
                    Gdx.app.postRunnable { listener.onFailure("Server error $statusCode: $responseString") }
                }
            }

            override fun failed(t: Throwable) {
                // ОШИБКА СЕТИ (нет интернета, сервер не отвечает)
                Gdx.app.postRunnable { listener.onFailure("Network error: ${t.message}") }
            }

            override fun cancelled() {
                Gdx.app.postRunnable { listener.onFailure("Request cancelled.") }
            }
        })
    }
}
