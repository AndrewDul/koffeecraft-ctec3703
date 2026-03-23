package uk.ac.dmu.koffeecraft.util.rewards

import org.junit.Assert.assertEquals
import org.junit.Test

class BeansBoosterManagerTest {

    @Test
    fun applyEarnedBeans_returnsClampedState_whenEarnedBeansIsZero() {
        val result = BeansBoosterManager.applyEarnedBeans(
            currentProgress = 15,
            currentPendingBoosters = -2,
            earnedBeans = 0
        )

        assertEquals(9, result.progress)
        assertEquals(0, result.pendingBoosters)
    }

    @Test
    fun applyEarnedBeans_updatesProgress_withoutAddingBooster_whenThresholdNotReached() {
        val result = BeansBoosterManager.applyEarnedBeans(
            currentProgress = 3,
            currentPendingBoosters = 1,
            earnedBeans = 4
        )

        assertEquals(7, result.progress)
        assertEquals(1, result.pendingBoosters)
    }

    @Test
    fun applyEarnedBeans_addsBooster_andKeepsRemainder_whenThresholdIsCrossed() {
        val result = BeansBoosterManager.applyEarnedBeans(
            currentProgress = 8,
            currentPendingBoosters = 1,
            earnedBeans = 7
        )

        assertEquals(5, result.progress)
        assertEquals(2, result.pendingBoosters)
    }

    @Test
    fun progressStatusText_returnsReadyFormat_whenPendingBoostersExist() {
        val result = BeansBoosterManager.progressStatusText(
            progress = 4,
            pendingBoosters = 2
        )

        assertEquals("2 ready • 4/10", result)
    }

    @Test
    fun rewardMetaLine_returnsProgressFormat_whenNoPendingBoostersExist() {
        val result = BeansBoosterManager.rewardMetaLine(
            progress = 6,
            pendingBoosters = 0
        )

        assertEquals("Progress: 6/10", result)
    }
}