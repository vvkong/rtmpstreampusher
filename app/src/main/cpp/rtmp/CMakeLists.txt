cmake_minimum_required(VERSION 3.4.1)

SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
message("======== ${CMAKE_C_FLAGS}")

file(GLOB SOURCE_FIELS ${CMAKE_SOURCE_DIR} *.c)

add_library(
        rtmp
        STATIC
        ${SOURCE_FIELS})
