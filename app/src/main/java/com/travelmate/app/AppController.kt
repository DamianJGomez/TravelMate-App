package com.travelmate.app

import android.app.Application
import android.content.Context

class AppController : Application() {

    companion object {
        private lateinit var instance: AppController

        fun getInstance(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}