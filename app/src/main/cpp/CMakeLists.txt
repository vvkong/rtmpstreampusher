cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(${CMAKE_SOURCE_DIR}/rtmp)

file(GLOB SOURCE_FILES ${CMAKE_SOURCE_DIR} *.cpp)

add_library(
        livepush
        SHARED
        ${SOURCE_FILES})

find_library(
        log-lib
        log
        pthread-lib
        pthread
        faac-lib
        facc
)

# 设置链接库搜索路径
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}")

add_library(x264 SHARED IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libx264.so")

#add_library(faac STATIC IMPORTED)
#set_target_properties(faac PROPERTIES IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libfaac.a")

target_link_libraries(
        livepush
        rtmp
        x264
        faac
        ${pthread-lib}
        ${log-lib})

target_include_directories(livepush PUBLIC
        "${CMAKE_SOURCE_DIR}"
        "${CMAKE_SOURCE_DIR}/rtmp"
        "${CMAKE_SOURCE_DIR}/include/x264"
        "${CMAKE_SOURCE_DIR}/include/aac"
        )