-keep class co.samepinch.android.** { *; }

-keep public class android.widget.ShareActionProvider {
  public *;
}

-dontwarn com.facebook.**
-dontwarn com.facebook.fresco**
-dontwarn it.sephiroth.**
-dontwarn com.squareup.**
-dontwarn org.springframework.**
-dontwarn org.apache.http.**
-dontwarn com.parse.**
-dontwarn com.aviary.android.**
#-dontwarn org.apache.lang.**
#-dontwarn org.apache.commons.**
#-dontwarn com.nhaarman.**
#-dontwarn se.emilsjolander.**
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-dontobfuscate
-dontoptimize
-allowaccessmodification
-repackageclasses ''
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }

-keep class android.support.v7.app.** { *; }
-keep interface android.support.v7.app.** { *; }

-keep class android.support.v13.app.** { *; }
-keep interface android.support.v13.app.** { *; }