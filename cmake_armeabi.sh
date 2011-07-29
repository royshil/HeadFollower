cmake -DARM_TARGET="armeabi" -DCMAKE_TOOLCHAIN_FILE=$OPENCV_PACKAGE_DIR/android.toolchain.cmake ..
make clean ; make -j4
