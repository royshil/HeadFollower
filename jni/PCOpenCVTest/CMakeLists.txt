cmake_minimum_required(VERSION 2.8)

project(HeadFollower-PCTest)

find_package(OpenCV REQUIRED)
message("OpenCV dir: " ${OpenCV_DIR})

include_directories(${OpenCV_INCLUDE_DIRS} ..)

add_definitions(-D_PC_COMPILE)
add_executable(HeadFollower-PCTest main.cpp ../Detector.cpp)

target_link_libraries(HeadFollower-PCTest ${OpenCV_LIBS})
