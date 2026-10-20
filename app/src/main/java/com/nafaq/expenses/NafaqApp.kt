package com.nafaq.expenses

import android.app.Application
import com.nafaq.expenses.data.AppContainer

class NafaqApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
