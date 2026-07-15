package com.aivision

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(jpeg: ByteArray, mode: String): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "frame.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_URL}/analyze?mode=$mode")
            .header("X-Auth-Token", BuildConfig.AUTH_TOKEN)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext "Нет ответа от сервера"

        if (!response.isSuccessful) {
            return@withContext "Ошибка ${response.code}: $responseBody"
        }

        JSONObject(responseBody).getString("text")
    }
}
