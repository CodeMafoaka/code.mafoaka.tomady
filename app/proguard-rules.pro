# Tomady Nutrition App ProGuard Rules
# Keep models serialized by Gson via HTTP JSON responses
-keepclassmembers class com.tomady.nutrition.service.diet.** { *; }
-keepclassmembers class com.tomady.nutrition.service.foodb.** { *; }
-keepclassmembers class com.tomady.nutrition.service.gemma.** { *; }
-keepclassmembers class com.tomady.nutrition.data.local.diet.entity.** { *; }
-keepclassmembers class com.tomady.nutrition.data.local.foodb.entity.** { *; }

# Keep NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
