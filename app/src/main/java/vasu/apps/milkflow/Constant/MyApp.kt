package vasu.apps.milkflow.Constant

import android.app.Application
import vasu.apps.milkflow.Services.Appwrite

class MyApp : Application() {
    override fun onCreate() {
        Appwrite.init(this)
        super.onCreate()
    }
}