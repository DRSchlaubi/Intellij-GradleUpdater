package me.schlaubi.intellij_gradle_version_checker.error_handling

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import me.schlaubi.intellij_gradle_version_checker.GradleUpdaterBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.runWithProgressBar
import java.awt.Component

class SentryErrorHandler : ErrorReportSubmitter() {

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        runWithProgressBar("Submitting error") {
            val event = SentryEvent().apply {
                if (additionalInfo != null) {
                    message = Message().apply { message = additionalInfo }
                }

                exceptions = events.map {
                    getSentryException(it.throwable)
                }
            }

            Sentry.captureEvent(event)

            val report = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
            consumer.consume(report)
        }

        return true
    }

    override fun getReportActionText(): String =
        GradleUpdaterBundle.getMessage("error_reporter.report_to_gradle_updater")
}
