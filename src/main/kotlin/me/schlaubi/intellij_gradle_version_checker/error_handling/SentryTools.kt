/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Rittmeister
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.schlaubi.intellij_gradle_version_checker.error_handling

import io.sentry.protocol.Mechanism
import io.sentry.protocol.SentryException
import io.sentry.protocol.SentryStackFrame
import io.sentry.protocol.SentryStackTrace

private val mechanism = Mechanism().apply {
    type = "IntelliJ error reporter"
}

fun getSentryException(throwable: Throwable): SentryException {
    val exceptionPackage = throwable.javaClass.getPackage()
    val fullClassName = throwable.javaClass.name
    val exception = SentryException()
    val exceptionMessage = throwable.message
    val exceptionClassName =
        if (exceptionPackage != null) fullClassName.replace(exceptionPackage.name + ".", "") else fullClassName
    val exceptionPackageName = exceptionPackage?.name
    val frames= getStackFrames(throwable.stackTrace)
    if (frames != null && frames.isNotEmpty()) {
        val sentryStackTrace = SentryStackTrace(frames)
        exception.stacktrace = sentryStackTrace
    }
    exception.type = exceptionClassName
    exception.mechanism = mechanism
    exception.module = exceptionPackageName
    exception.value = exceptionMessage
    return exception
}

fun getStackFrames(elements: Array<StackTraceElement?>?): List<SentryStackFrame?>? {
    var sentryStackFrames: MutableList<SentryStackFrame?>? = null
    if (elements != null && elements.isNotEmpty()) {
        sentryStackFrames = ArrayList()
        for (item in elements) {
            if (item != null) {

                // we don't want to add our own frames
                val className = item.className
                if (className.startsWith("io.sentry.")
                    && !className.startsWith("io.sentry.samples.")
                    && !className.startsWith("io.sentry.mobile.")
                ) {
                    continue
                }
                val sentryStackFrame = SentryStackFrame()
                sentryStackFrame.module = className
                sentryStackFrame.function = item.methodName
                sentryStackFrame.filename = item.fileName
                // Protocol doesn't accept negative line numbers.
                // The runtime seem to use -2 as a way to signal a native method
                if (item.lineNumber >= 0) {
                    sentryStackFrame.lineno = item.lineNumber
                }
                sentryStackFrame.isNative = item.isNativeMethod
                sentryStackFrames.add(sentryStackFrame)
            }
        }
        sentryStackFrames.reverse()
    }
    return sentryStackFrames
}
