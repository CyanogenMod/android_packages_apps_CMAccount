LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := CMID

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v13

include $(BUILD_PACKAGE)
