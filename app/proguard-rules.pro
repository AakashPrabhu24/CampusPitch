# CampusPitch R8 / ProGuard Configuration
# 2026 copyright Child Chiller Ventures

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep data lake models serialized with Firestore
-keep class com.childchiller.campuspitch.core.models.** { *; }

# Keep Firebase and Play Integrity models
-keepattributes EnclosingMethod
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
