// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("example");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("example")
//      }
//    }
#include <android/log.h>
#include <jni.h>
#define TAG "NativeCodeExample"
extern "C" JNIEXPORT void JNICALL Java_com_example_limbo_ExampleActivity_hello( JNIEnv* env, jclass clazz) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "Hello from C++");
}