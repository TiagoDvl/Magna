package com.tick.magna.data.repository.result

sealed interface AsyncResult<out T> {

    data object Loading : AsyncResult<Nothing>

    data class Success<T>(val data: T) : AsyncResult<T>

    data class Failure(val error: Throwable?) : AsyncResult<Nothing>
}
