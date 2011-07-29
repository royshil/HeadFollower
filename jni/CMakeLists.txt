#########################################################
# Find opencv and android-opencv
#########################################################

#set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../../build 
#    CACHE PATH "The path where you built opencv for android")
#set(AndroidOpenCV_DIR ${CMAKE_SOURCE_DIR}/../../android-opencv/build_neon 
#    CACHE PATH "The path where you built android-opencv")

if( ARMEABI_V7A )
  if ( NEON )
    set(OpenCV_DIR $ENV{OPENCV_PACKAGE_DIR}/armeabi-v7a-neon/share/opencv/)
  else()
    set(OpenCV_DIR $ENV{OPENCV_PACKAGE_DIR}/armeabi-v7a/share/opencv/)
  endif()
else()
  set(OpenCV_DIR $ENV{OPENCV_PACKAGE_DIR}/armeabi/share/opencv/)
endif()


find_package(OpenCV REQUIRED NO_DEFAULT_PATH PATHS ${OpenCV_DIR})
message("OpenCV dir: " ${OpenCV_DIR})

if( ARMEABI_V7A )
  set(AndroidOpenCV_DIR ${CMAKE_SOURCE_DIR}/../../android-opencv/build_neon/) 
else()
  set(AndroidOpenCV_DIR $ENV{HOME}/Downloads/trunk/opencv/android/android-opencv/build_armeabi)
endif()

message ("android-opencv dir " ${AndroidOpenCV_DIR})
FIND_PACKAGE(AndroidOpenCV REQUIRED PATHS(${AndroidOpenCV_DIR}) )
message("android-opencv lib-dir: " ${AndroidOpenCV_LIB_DIR})


#########################################################
#c flags, included, and lib dependencies
#########################################################

#notice the "recycling" of CMAKE_C_FLAGS
#this is necessary to pick up android flags
set( CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -pedantic -fPIC" )

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})

set( LIBRARY_DEPS ${AndroidOpenCV_LIBS} ${OpenCV_LIBS} )
if(ANDROID)
  set( LIBRARY_DEPS ${LIBRARY_DEPS} log dl)
endif(ANDROID)

message("library deps: " ${LIBRARY_DEPS})

#########################################################
#SWIG STUFF
#########################################################
#the java package to place swig generated java files in
set(MY_PACKAGE edu.mit.media.fluid.charactertracker.jni)

if(NOT ANDROID)
  #non android swig and jni
  #jni is available by default on android
  find_package(JNI REQUIRED)
  include_directories(${JNI_INCLUDE_DIRS})
  FIND_PACKAGE(SWIG)
endif()

INCLUDE(${SWIG_USE_FILE}) #on android this is found by the cmake toolchain

if(ANDROID)
  #this will set the output path for the java package
  #and properly create the package declarations in generated java sources
  SET_SWIG_JAVA_PACKAGE( ${MY_PACKAGE} ) #defined in the android toolchain
endif(ANDROID)

#this add's the swig path for the opencv wrappers
SET(CMAKE_SWIG_FLAGS ${CMAKE_SWIG_FLAGS} "-I${AndroidOpenCV_SWIG_DIR}" )

SET_SOURCE_FILES_PROPERTIES(charactertracker.i PROPERTIES CPLUSPLUS ON)

#add the swig module, giving it the name, java, and then all of the source files
SWIG_ADD_MODULE(charactertracker java 
                charactertracker.i #swig file
                Detector.cpp #cpp files can be compiled to
                )
             
#link the module like any other   
target_link_libraries(charactertracker ${LIBRARY_DEPS} )
