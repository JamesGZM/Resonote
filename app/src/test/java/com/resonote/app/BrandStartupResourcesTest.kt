package com.resonote.app

import android.graphics.Color
import androidx.core.splashscreen.R as SplashScreenR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BrandStartupResourcesTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun manifestUsesBrandIconsAndSplashTheme() {
        val applicationInfo = context.applicationInfo

        assertThat(applicationInfo.icon).isEqualTo(R.mipmap.ic_launcher)
        assertThat(context.resources.getResourceTypeName(R.mipmap.ic_launcher_round)).isEqualTo("mipmap")
        assertThat(tagAttribute(R.drawable.ic_launcher_foreground, "group", "scaleX")).isEqualTo("0.72")
        assertThat(applicationInfo.theme).isEqualTo(R.style.Theme_Resonote_Splash)
    }

    @Test
    fun splashThemeUsesBrandResourcesAndPostTheme() {
        val attributes = context.obtainStyledAttributes(
            R.style.Theme_Resonote_Splash,
            intArrayOf(
                SplashScreenR.attr.windowSplashScreenBackground,
                SplashScreenR.attr.windowSplashScreenAnimatedIcon,
                SplashScreenR.attr.windowSplashScreenAnimationDuration,
                SplashScreenR.attr.postSplashScreenTheme,
            ),
        )

        try {
            assertThat(attributes.getResourceId(0, 0)).isEqualTo(R.color.splash_background)
            assertThat(attributes.getResourceId(1, 0)).isEqualTo(R.drawable.ic_splash)
            assertThat(attributes.getInt(2, 0)).isEqualTo(SPLASH_DURATION_MILLIS)
            assertThat(attributes.getResourceId(3, 0)).isEqualTo(R.style.Theme_Resonote)
        } finally {
            attributes.recycle()
        }
    }

    @Test
    fun api31AndAboveUsesAnimatedVector() {
        assertThat(rootTag(R.drawable.ic_splash)).isEqualTo("animated-vector")
        assertThat(tagAttribute(R.drawable.ic_splash_animated_vector, "group", "scaleX")).isEqualTo("0.72")
        assertThat(tagAttribute(R.drawable.ic_splash_animated_vector, "group", "scaleY")).isEqualTo("0.72")
        assertThat(rootTag(R.animator.ic_splash_main_draw)).isEqualTo("set")
        assertTrimPathContract(R.animator.ic_splash_main_draw, duration = "550", startOffset = null)
        assertStrokeAlphaContract(R.animator.ic_splash_main_draw, startOffset = null)
        assertTrimPathContract(R.animator.ic_splash_finish_draw, duration = "200", startOffset = "550")
        assertStrokeAlphaContract(R.animator.ic_splash_finish_draw, startOffset = "570")
    }

    @Test
    @Config(sdk = [30])
    fun api30UsesStaticVectorFallback() {
        assertThat(rootTag(R.drawable.ic_splash)).isEqualTo("vector")
    }

    @Test
    fun lightSplashUsesFrozenSemanticColors() {
        assertThat(context.getColor(R.color.splash_background)).isEqualTo(Color.rgb(255, 251, 255))
        assertThat(context.getColor(R.color.splash_mark)).isEqualTo(Color.rgb(174, 42, 75))
    }

    @Test
    @Config(sdk = [35], qualifiers = "night")
    fun darkSplashUsesFrozenSemanticColors() {
        assertThat(context.getColor(R.color.splash_background)).isEqualTo(Color.rgb(32, 26, 27))
        assertThat(context.getColor(R.color.splash_mark)).isEqualTo(Color.rgb(255, 178, 188))
    }

    private fun rootTag(resourceId: Int): String {
        val parser = context.resources.getXml(resourceId)
        while (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
        }
        return parser.name
    }

    private fun attributesForProperty(resourceId: Int, propertyName: String): Map<String, String?> {
        val parser = context.resources.getXml(resourceId)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (
                parser.eventType == XmlPullParser.START_TAG &&
                parser.getAttributeValue(ANDROID_NAMESPACE, "propertyName") == propertyName
            ) {
                return mapOf(
                    "duration" to parser.getAttributeValue(ANDROID_NAMESPACE, "duration"),
                    "startOffset" to parser.getAttributeValue(ANDROID_NAMESPACE, "startOffset"),
                    "valueFrom" to parser.getAttributeValue(ANDROID_NAMESPACE, "valueFrom"),
                    "valueTo" to parser.getAttributeValue(ANDROID_NAMESPACE, "valueTo"),
                )
            }
            parser.next()
        }
        error("Animator property $propertyName not found")
    }

    private fun tagAttribute(resourceId: Int, tagName: String, attributeName: String): String? {
        val parser = context.resources.getXml(resourceId)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == tagName) {
                return parser.getAttributeValue(ANDROID_NAMESPACE, attributeName)
            }
            parser.next()
        }
        error("XML tag $tagName not found")
    }

    private fun assertTrimPathContract(resourceId: Int, duration: String, startOffset: String?) {
        val attributes = attributesForProperty(resourceId, "trimPathEnd")
        assertThat(attributes["duration"]).isEqualTo(duration)
        assertThat(attributes["startOffset"]).isEqualTo(startOffset)
        assertThat(attributes["valueFrom"]).isEqualTo("0")
        assertThat(attributes["valueTo"]).isEqualTo("1")
    }

    private fun assertStrokeAlphaContract(resourceId: Int, startOffset: String?) {
        val attributes = attributesForProperty(resourceId, "strokeAlpha")
        assertThat(attributes["duration"]).isEqualTo("1")
        assertThat(attributes["startOffset"]).isEqualTo(startOffset)
        assertThat(attributes["valueFrom"]).isEqualTo("0")
        assertThat(attributes["valueTo"]).isEqualTo("1")
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val SPLASH_DURATION_MILLIS = 750
    }
}
