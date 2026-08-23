# Keep generic signatures and runtime annotations used by libraries such as
# Retrofit, Kotlin serialization, Room, and Hilt. Library-specific rules are
# supplied by those dependencies through their consumer ProGuard files.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
