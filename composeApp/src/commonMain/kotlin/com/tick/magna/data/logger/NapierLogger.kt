package com.tick.magna.data.logger

import io.github.aakira.napier.Napier

class NapierLogger : AppLoggerInterface {
    override fun d(message: String, tag: String?) {
        Napier.d(message, tag = tag)
    }
    
    override fun i(message: String, tag: String?) {
        Napier.i(message, tag = tag)
    }
    
    override fun w(message: String, tag: String?) {
        Napier.w(message, tag = tag)
    }
    
    override fun e(message: String, throwable: Throwable?, tag: String?) {
        Napier.e(message, throwable, tag = tag)
    }
}