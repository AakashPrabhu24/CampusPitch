package com.childchiller.campuspitch

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class CampusPitchApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseSafe()
    }

    private fun initFirebaseSafe() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:304963557913:android:9d2ebdb508f2dc2cbfe888")
                    .setApiKey("AIzaSyBq75L32WgUbUdmLtwqa_xnMJc6InULkqY")
                    .setProjectId("campuspitch-childchiller")
                    .setStorageBucket("campuspitch-childchiller.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }

            // App Check setup: Debug provider for dev builds, Play Integrity for release
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                android.util.Log.d("CampusPitchApp", "Installing DebugAppCheckProviderFactory. For demo, keep Firestore App Check enforcement OFF in Firebase Console.")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            // Logged; allow app execution to continue smoothly
            e.printStackTrace()
        }
    }
}
