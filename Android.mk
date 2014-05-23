LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := CMAccount
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v13 \
    volley \
    gson \
    play \
    spongycastle-light-jdk15on \
	libphonenumbergoogle

LOCAL_JAVA_LIBRARIES += org.cyanogenmod.hardware

# Include res dir from chips
google_play_dir := ../../../external/google/google_play_services/libproject/google-play-services_lib/res
res_dir := $(google_play_dir) res

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.google.android.gms

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    play:../../../external/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar

include $(BUILD_MULTI_PREBUILT)
