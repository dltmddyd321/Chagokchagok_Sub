package com.windrr.hyundaesub.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

private val errorGson = Gson()

suspend fun <T> apiCall(request: suspend () -> Response<ApiResponse<T>>): ApiResult<T> {
    val response = try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        return ApiResult.NetworkError(e)
    }
    val body = response.body()
    val data = body?.data
    if (response.isSuccessful && data != null) return ApiResult.Success(data)
    val error = body ?: parseErrorBody(response)
    return ApiResult.Failure(response.code(), error?.code, error?.message)
}

private fun parseErrorBody(response: Response<*>): ApiResponse<Unit>? {
    val raw = response.errorBody()?.string() ?: return null
    return runCatching { errorGson.fromJson(raw, ApiResponse::class.java) }
        .getOrNull()
        ?.let { ApiResponse(it.code, it.message, null) }
}
