package com.tick.magna.data.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherInterface {

    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}