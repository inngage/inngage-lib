package br.com.inngage.sdk.internal.core.http

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin HTTP client for Inngage API calls.
 *
 * All calls are `suspend` functions executed on [dispatcher] (defaults to [Dispatchers.IO]).
 * Replaces the callback-based ExecutorService pattern from InngageUtils.java.
 *
 * @param dispatcher Override for testing; defaults to [Dispatchers.IO].
 * @param connectionFactory Factory that opens an [HttpURLConnection] for a given URL string.
 *        Override in tests to supply a mock connection without touching real network.
 */
internal class HttpClient(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectionFactory: (String) -> HttpURLConnection = { urlStr ->
        URL(urlStr).openConnection() as HttpURLConnection
    }
) {
    private val tag = InngageConfig.TAG_NOTIFY

    /**
     * Performs an HTTP POST sending [body] as JSON to [endpoint].
     *
     * @return [Result.success] with the response body string on any 2xx status,
     *         or [Result.failure] wrapping an [IOException] otherwise.
     */
    suspend fun post(body: JSONObject, endpoint: String): Result<String> =
        withContext(dispatcher) {
            runCatching {
                Log.d(tag, "HTTP POST → $endpoint\nBody: ${body.toString(2)}")

                val conn = connectionFactory(endpoint).apply {
                    readTimeout = 10_000
                    connectTimeout = 20_000
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json;charset=utf-8")
                    setRequestProperty("X-Requested-With", "XMLHttpRequest")
                }

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val responseBody = BufferedInputStream(conn.inputStream).use { it.readBytes().toString(Charsets.UTF_8) }
                    Log.d(tag, "HTTP POST ← $endpoint [$code]\nResponse: $responseBody")
                    responseBody
                } else {
                    val errorBody = try {
                        BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                    } catch (_: Exception) { "(no error body)" }
                    Log.e(tag, "HTTP POST ← $endpoint [$code]\nError body: $errorBody")
                    throw IOException("HTTP $code from $endpoint")
                }
            }.also { result ->
                result.onFailure { Log.e(tag, "HTTP POST failed → $endpoint: ${it.message}") }
            }
        }

    /**
     * Performs an HTTP GET request to [endpoint].
     *
     * @return [Result.success] with the response body string on any 2xx status,
     *         or [Result.failure] wrapping an [IOException] otherwise.
     */
    suspend fun get(endpoint: String): Result<String> =
        withContext(dispatcher) {
            runCatching {
                val conn = connectionFactory(endpoint).apply {
                    readTimeout = 10_000
                    connectTimeout = 20_000
                    requestMethod = "GET"
                    doInput = true
                    setRequestProperty("Content-Type", "application/json;charset=utf-8")
                    setRequestProperty("X-Requested-With", "XMLHttpRequest")
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    BufferedInputStream(conn.inputStream).use { it.readBytes().toString(Charsets.UTF_8) }
                } else {
                    throw IOException("HTTP $code from $endpoint")
                }
            }.also { result ->
                result.onFailure { Log.e(tag, "HTTP GET failed → $endpoint: ${it.message}") }
            }
        }
}


