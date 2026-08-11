package com.resonote.catalog

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
    fun manifestUsesCompanionIconsAndSplashTheme() {
        val applicationInfo = context.applicationInfo

        assertThat(applicationInfo.icon).isEqualTo(R.mipmap.ic_launcher)
        assertThat(context.resources.getResourceTypeName(R.mipmap.ic_launcher_round)).isEqualTo("mipmap")
        assertThat(tagAttribute(R.drawable.ic_launcher_foreground, "group", "scaleX")).isEqualTo("0.62")
        assertThat(tagAttribute(R.drawable.ic_launcher_foreground, "group", "scaleY")).isEqualTo("0.62")
        assertThat(applicationInfo.theme).isEqualTo(R.style.Theme_Resonote_Catalog_Splash)
    }

    @Test
    fun splashThemeUsesStaticCompanionMarkAndPostTheme() {
        val attributes = context.obtainStyledAttributes(
            R.style.Theme_Resonote_Catalog_Splash,
            intArrayOf(
                SplashScreenR.attr.windowSplashScreenBackground,
                SplashScreenR.attr.windowSplashScreenAnimatedIcon,
                SplashScreenR.attr.postSplashScreenTheme,
            ),
        )

        try {
            assertThat(attributes.getResourceId(0, 0)).isEqualTo(R.color.splash_background)
            assertThat(attributes.getResourceId(1, 0)).isEqualTo(R.drawable.ic_splash)
            assertThat(attributes.getResourceId(2, 0)).isEqualTo(R.style.Theme_Resonote_Catalog)
        } finally {
            attributes.recycle()
        }
    }

    @Test
    fun catalogSplashRemainsStaticOnLatestApi() {
        assertThat(rootTag(R.drawable.ic_splash)).isEqualTo("vector")
        assertThat(tagAttribute(R.drawable.ic_splash, "group", "scaleX")).isEqualTo("0.72")
        assertThat(tagAttribute(R.drawable.ic_splash, "group", "scaleY")).isEqualTo("0.72")
    }

    @Test
    fun lightSplashUsesHarmonicViolet() {
        assertThat(context.getColor(R.color.splash_background)).isEqualTo(Color.rgb(255, 251, 255))
        assertThat(context.getColor(R.color.splash_mark)).isEqualTo(Color.rgb(102, 85, 143))
    }

    @Test
    @Config(sdk = [35], qualifiers = "night")
    fun darkSplashUsesSecondaryTone80() {
        assertThat(context.getColor(R.color.splash_background)).isEqualTo(Color.rgb(32, 26, 27))
        assertThat(context.getColor(R.color.splash_mark)).isEqualTo(Color.rgb(208, 188, 254))
    }

    private fun rootTag(resourceId: Int): String {
        val parser = context.resources.getXml(resourceId)
        while (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
        }
        return parser.name
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

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
