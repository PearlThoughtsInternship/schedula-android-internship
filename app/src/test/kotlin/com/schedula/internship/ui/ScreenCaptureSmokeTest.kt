package com.schedula.internship.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.common.truth.Truth.assertThat
import com.schedula.internship.LoginScreen
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ScreenCaptureSmokeTest {

    @Test
    fun capturesLoginScreen() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()

        activity.setContent {
            LoginScreen(
                phone = "",
                otp = "",
                otpSent = false,
                error = null,
                onPhoneChange = {},
                onOtpChange = {},
                onSendOtp = {},
                onVerifyOtp = {},
            )
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val outputDir = File("build/reports/screen-captures").apply { mkdirs() }
        val outputFile = File(outputDir, "00-login-smoke.png")

        drawActivityToPng(activity, outputFile)

        assertThat(outputFile.exists()).isTrue()
        assertThat(outputFile.length()).isGreaterThan(0L)

        activityController.pause().stop().destroy()
    }

    private fun drawActivityToPng(activity: ComponentActivity, outputFile: File) {
        val root = activity.window.decorView
        val width = 1080
        val height = 2200

        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        root.draw(canvas)
        outputFile.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
