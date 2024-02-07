package dev.fabik.merossble

import android.app.Application
import com.google.android.material.color.DynamicColors

class MerossBleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

}