package com.tick.magna.data.logger

interface AppLoggerInterface {
    fun d(message: String, tag: String? = null)
    fun i(message: String, tag: String? = null)
    fun w(message: String, tag: String? = null)
    fun e(message: String, throwable: Throwable? = null, tag: String? = null)
}