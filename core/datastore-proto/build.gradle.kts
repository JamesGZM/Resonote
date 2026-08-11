plugins {
    alias(libs.plugins.resonote.android.library)
}

android {
    namespace = "com.resonote.core.datastore.proto"
}

dependencies {
    api(libs.protobuf.kotlin.lite)
}
