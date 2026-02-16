package com.irah.galleria.baselineprofile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @RequiresApi(Build.VERSION_CODES.P)
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @RequiresApi(Build.VERSION_CODES.P)
    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.irah.galleria",
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()

            // 1. Scroll Gallery List
            // The gallery grid items are clickable, so we can find by scrolling able container
            // We assume standard scrolling behavior for LazyGrid
            val galleryList = device.findObject(androidx.test.uiautomator.By.scrollable(true))
            if (galleryList != null) {
                galleryList.setGestureMargin(device.displayWidth / 5)
                galleryList.scroll(androidx.test.uiautomator.Direction.DOWN, 1f)
                galleryList.scroll(androidx.test.uiautomator.Direction.UP, 1f)
            }

            // 2. Open an Image (Media Viewer)
            // We look for any clickable object in the list that is likely a media item
            // Since we don't have explicit test tags ensuring internal availability,
            // we try to click the first available item.
            // Note: In a real scenario, use Modifier.testTag("media_item")
            if (galleryList != null) {
                 val children = galleryList.children
                 if (children.isNotEmpty()) {
                     children[0].click()
                     device.waitForIdle()
                     // Wait for media viewer
                     Thread.sleep(1000)
                     device.pressBack()
                     device.waitForIdle()
                 }
            }

            // 3. Navigate to Albums
            val albumsTab = device.findObject(androidx.test.uiautomator.By.text("Albums"))
            albumsTab?.click()
            device.waitForIdle()

            // 4. Scroll Albums List
            val albumsList = device.findObject(androidx.test.uiautomator.By.scrollable(true))
            if (albumsList != null) {
                albumsList.setGestureMargin(device.displayWidth / 5)
                albumsList.scroll(androidx.test.uiautomator.Direction.DOWN, 1f)
            }

            // 5. Open Album Detail
            if (albumsList != null) {
                val children = albumsList.children
                // Skip smart albums header if possible, or just click first available album/folder
                // Smart albums have specific icons, but text is easier. Let's just click the first child.
                 if (children.isNotEmpty()) {
                     children[0].click()
                     device.waitForIdle()
                     Thread.sleep(1000)
                     device.pressBack()
                 }
            }
        }
    }
}
