package com.fachri.audiostreamingcompose.network

import android.content.Context
import com.fachri.audiostreamingcompose.network.model.VoiceOption
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class API(private val context: Context) {
    companion object {

        fun soundUrlString(voiceId: Int, sampleId: Int): String {
            return "https://static.dailyfriend.ai/conversations/samples/$voiceId/$sampleId/audio.mp3"
        }
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    private val authEndpoint = "https://static.dailyfriend.ai/api/auth"
    private val greetingsEndpoint = "https://static.dailyfriend.ai/api/greetings"

    suspend fun fetchGreetings(): Result<List<VoiceOption>> = withContext(Dispatchers.IO) {
        try {
//            val token = getValidJWTToken()
            val request = Request.Builder()
                .url(greetingsEndpoint)
//                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(BaseError.InvalidResponse)
            }

            val body = response.body?.string()
            val voices = gson.fromJson<List<VoiceOption>>(
                body,
                object : TypeToken<List<VoiceOption>>() {}.type
            )
            Result.success(voices)
        } catch (e: Exception) {
            Result.failure(BaseError.NetworkError(e))
        }
    }

    suspend fun fetchTranscription(voiceId: Int, sampleId: Int): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                //            val token = getValidJWTToken()
                val urlString =
                    "https://static.dailyfriend.ai/conversations/samples/$voiceId/$sampleId/transcription.txt"
                val request = Request.Builder()
                    .url(urlString)
                    //                .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(BaseError.InvalidResponse)
                }

                val fetchedText = response.body?.string()
                if (fetchedText != null) {
                    Result.success(fetchedText)
                } else {
                    Result.failure(BaseError.FailedDecoded)
                }
            } catch (e: Exception) {
                Result.failure(BaseError.NetworkError(e))
            }
        }

    private suspend fun getValidJWTToken(): String {
        return getJWTToken() ?: fetchJWTToken()
    }

    private suspend fun fetchJWTToken(): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("username", "your_username")
                put("password", "your_password")
            }.toString()

            val request = Request.Builder()
                .url(authEndpoint)
                .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), body))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw BaseError.InvalidResponse
            }

            val json = JSONObject(response.body?.string() ?: "")
            val token = json.getString("token")
            storeJWTToken(token)
            return@withContext token
        } catch (e: IOException) {
            throw BaseError.NetworkError(e)
        }
    }

    private fun storeJWTToken(token: String) {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .apply()
    }

    private fun getJWTToken(): String? {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwt_token", null)
    }
}

sealed class BaseError : Throwable() {
    object MissingToken : BaseError()
    object InvalidResponse : BaseError()
    object FailedDecoded : BaseError()
    data class NetworkError(val error: Throwable) : BaseError()
}

