NDK=/Users/wangrenxing/ffmpeg/android-ndk-r17c
PREFIX=`pwd`/android/armeabi-v7a
TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64
CROSS_COMPILE="$TOOLCHAIN/bin/arm-linux-androideabi-"
CFLAGS="-isysroot $NDK/sysroot -isystem $NDK/sysroot/usr/include/arm-linux-androideabi -D__ANDROID_API__=16 -fPIC "
PLATFORM=$NDK/platforms/android-16/arch-arm

export CC="${CROSS_COMPILE}gcc --sysroot=${PLATFORM}"
export CXX="${CROSS_COMPILE}g++ --sysroot=${PLATFORM}"
export CPPFLAGS="$CFLAGS"
export CFLAGS="$CFLAGS"
export CXXFLAGS="$CFLAGS"
export NM="${CROSS_COMPILE}nm"
export STRIP="${CROSS_COMPILE}strip"
export RANLIB="${CROSS_COMPILE}ranlib"
export AR="${CROSS_COMPILE}ar"

echo $CC
./configure \
--prefix=$PREFIX \
--host=arm-linux \
--with-pic \
--with-sysroot="$PLATFORM" \
--enable-shared=no

make clean
make -j4
make install