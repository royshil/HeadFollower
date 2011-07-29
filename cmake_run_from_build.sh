cmake -DARM_TARGET="armeabi-v7a with NEON" -DCMAKE_TOOLCHAIN_FILE=$OPENCV_PACKAGE_DIR/android.toolchain.cmake ..
make clean ; make -j4
