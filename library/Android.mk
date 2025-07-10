# Define the local path where this file is located
LOCAL_PATH := $(call my-dir)
AbsolutePath := $(LOCAL_PATH)  # Avoid abspath issues in Windows

# Include NDK build system variables
include $(CLEAR_VARS)

# Define OPUS library path
OPUS := $(LOCAL_PATH)/src/main/jni/Opus

# Module name
LOCAL_MODULE    := opus_wrapper

# Source files
LOCAL_SRC_FILES := $(OPUS)/Opus.cpp

# Linking system libraries
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

# Include paths
LOCAL_C_INCLUDES += $(OPUS)/Include
LOCAL_CFLAGS = -D__STDC_CONSTANT_MACROS -I$(OPUS)/Include

# C++ flags
LOCAL_CPPFLAGS += -std=c++11
LOCAL_LDLIBS += -landroid -lOpenSLES

# Build as a shared library
include $(BUILD_SHARED_LIBRARY)
