-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.owl.minerva.stocklab.model.** { *; }
-keep class com.owl.minerva.stocklab.enums.** { *; }
-keep class com.owl.minerva.stocklab.dao.** { *; }
-keepclassmembers class com.owl.minerva.stocklab.database.StockLabDatabase {
    abstract <methods>;
}
-keepclassmembers class com.owl.minerva.stocklab.database.Converters {
    *;
}

-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
