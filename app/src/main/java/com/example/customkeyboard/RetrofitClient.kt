package com.example.customkeyboard

import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object RetrofitClient {
    const val PRIMARY_URL = "http://192.168.254.163/"
    const val FALLBACK_URL = "http://126.209.7.246/"
    private const val MAX_RETRIES = 10
    private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second
    private const val REQUEST_TIMEOUT_SECONDS = 2L

    // Configuring the logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    // Create an OkHttpClient with logging, timeout settings, and retry logic
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(RetryInterceptor()) // Adding the retry interceptor
        .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Creating an instance of ApiService with lenient JSON parsing
    val instance: ApiService by lazy {
        val gson = GsonBuilder()
            .setLenient() // Enable lenient parsing to handle large or non-strict JSON
            .create()
        Retrofit.Builder()
            .baseUrl(PRIMARY_URL) // Start with the primary URL
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    // Interceptor to retry requests with exponential backoff
    class RetryInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response: Response? = null
            var exception: IOException? = null

            // Try both URLs with retries
            for (attempt in 1..MAX_RETRIES) {
                for (baseUrl in listOf(PRIMARY_URL, FALLBACK_URL)) {
                    try {
                        // Create new request with current base URL
                        val newUrl = request.url.newBuilder()
                            .host(baseUrl.substringAfter("://").substringBefore("/"))
                            .build()
                        val newRequest = request.newBuilder().url(newUrl).build()

                        response = chain.proceed(newRequest)

                        // If response is successful, return it
                        if (response.isSuccessful) {
                            return response
                        }

                        // Close the response if not successful
                        response.close()
                    } catch (e: IOException) {
                        exception = e
                        // If it's the last attempt, break and throw the exception
                        if (attempt == MAX_RETRIES && baseUrl == FALLBACK_URL) {
                            break
                        }
                    }

                    // If not the last attempt, wait with exponential backoff
                    if (attempt < MAX_RETRIES || baseUrl != FALLBACK_URL) {
                        try {
                            val delayMs = INITIAL_RETRY_DELAY_MS * (2.0.pow((attempt - 1).toDouble())).toLong()
                            Thread.sleep(delayMs)
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                            throw IOException("Interrupted during retry delay", e)
                        }
                    }
                }
            }
            throw exception ?: IOException("Unknown error occurred after $MAX_RETRIES attempts")
        }
    }
}
