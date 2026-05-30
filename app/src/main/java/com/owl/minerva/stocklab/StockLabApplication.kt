package com.owl.minerva.stocklab

import android.app.Application
import com.google.android.gms.ads.MobileAds

class StockLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
