package uk.ac.dmu.koffeecraft.util.rewards

data class BeansBoosterProgressState(
    val progress: Int,
    val pendingBoosters: Int
)

object BeansBoosterManager {

    const val BOOSTER_STEP = 10
    const val BOOSTER_REWARD = 5

    fun applyEarnedBeans(
        currentProgress: Int,
        currentPendingBoosters: Int,
        earnedBeans: Int
    ): BeansBoosterProgressState {
        if (earnedBeans <= 0) {
            return BeansBoosterProgressState(
                progress = currentProgress.coerceIn(0, BOOSTER_STEP - 1),
                pendingBoosters = currentPendingBoosters.coerceAtLeast(0)
            )
        }

        val safeProgress = currentProgress.coerceIn(0, BOOSTER_STEP - 1)
        val safePending = currentPendingBoosters.coerceAtLeast(0)

        val total = safeProgress + earnedBeans
        val additionalBoosters = total / BOOSTER_STEP
        val newProgress = total % BOOSTER_STEP

        return BeansBoosterProgressState(
            progress = newProgress,
            pendingBoosters = safePending + additionalBoosters
        )
    }

    fun progressStatusText(progress: Int, pendingBoosters: Int): String {
        val safeProgress = progress.coerceIn(0, BOOSTER_STEP - 1)
        val safePending = pendingBoosters.coerceAtLeast(0)

        return if (safePending > 0) {
            "$safePending ready • $safeProgress/$BOOSTER_STEP"
        } else {
            "$safeProgress/$BOOSTER_STEP"
        }
    }

    fun rewardMetaLine(progress: Int, pendingBoosters: Int): String {
        val safeProgress = progress.coerceIn(0, BOOSTER_STEP - 1)
        val safePending = pendingBoosters.coerceAtLeast(0)

        return if (safePending > 0) {
            "$safePending ready • $safeProgress/$BOOSTER_STEP"
        } else {
            "Progress: $safeProgress/$BOOSTER_STEP"
        }
    }
}