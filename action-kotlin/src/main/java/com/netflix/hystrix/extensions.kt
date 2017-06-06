package com.netflix.hystrix

internal fun <T> AbstractCommand<T>.isTimedOut(): Boolean {
    return this.isCommandTimedOut?.get() == AbstractCommand.TimedOutStatus.TIMED_OUT
}
