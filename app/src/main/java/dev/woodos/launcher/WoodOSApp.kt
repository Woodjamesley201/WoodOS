package dev.woodos.launcher

import android.app.Application
import dev.woodos.launcher.core.Apps
import dev.woodos.launcher.core.IconCache
import dev.woodos.launcher.core.Perf
import dev.woodos.launcher.core.Prefs

class WoodOSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Perf.init(this)
        Prefs.init(this)
        IconCache.init(this)
        Apps.initAsync(this)
    }
}
