package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          color = com.example.ui.theme.GhostBackground,
          modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
          androidx.compose.foundation.layout.Box(
            contentAlignment = androidx.compose.ui.Alignment.Center,
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
          ) {
            androidx.compose.material3.Text(
              text = "GHOST STORE",
              color = com.example.ui.theme.GhostSecondary,
              fontSize = 32.sp,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
              letterSpacing = 4.sp
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
