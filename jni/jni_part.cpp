#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <vector>
#include <string>
#include "Detector.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>

using namespace std;
using namespace cv;

extern "C" {
	jfloatArray GoDetector(JNIEnv* env, Mat& mbgra, jboolean i_am, jboolean _flip, jboolean _debug);
	
	Detector detector;
	
	JNIEXPORT jfloatArray JNICALL Java_edu_mit_media_fluid_royshil_headfollower_CharacterTrackerView_FindFeatures(
				JNIEnv* env, 
				jobject thiz, 
				jint width, 
				jint height, 
				jbyteArray yuv, 
				jintArray bgra,
				jint i_am, 
				jboolean _flip, 
				jboolean _debug )
	{
		jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
		jint*  _bgra = env->GetIntArrayElements(bgra, 0);

		Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
		Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
		Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);

		//Please make attention about BGRA byte order
		//ARGB stored in java as int array becomes BGRA at native level
		cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
		
		env->ReleaseByteArrayElements(yuv, _yuv, 0);
		
	//    vector<KeyPoint> v;
	//
	//    FastFeatureDetector fastdetector(50);
	//    fastdetector.detect(mgray, v);
	//    for( size_t i = 0; i < v.size(); i++ )
	//        circle(mbgra, Point(v[i].pt.x, v[i].pt.y), 10, Scalar(0,0,255,255));
		detector.shouldResize = true;
		
		jfloatArray returnfa = GoDetector(env, mbgra, i_am, _flip, _debug);
		env->ReleaseIntArrayElements(bgra, _bgra, 0);

		return returnfa;
	}
	
	JNIEXPORT jint JNICALL Java_edu_mit_media_fluid_royshil_headfollower_CharacterTrackerView_CalibrateSelf(
				  JNIEnv* env, 
				  jobject thiz, 
				  jint width, 
				  jint height, 
				  jbyteArray yuv, 
				  jintArray bgra,
				  jint i_am, 
				  jboolean _flip, 
				  jboolean _debug )
	{
		jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
		jint*  _bgra = env->GetIntArrayElements(bgra, 0);
		
		Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
		Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
		cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
		env->ReleaseByteArrayElements(yuv, _yuv, 0);
		
		detector.shouldResize = true;
		
		jint retval = detector.calibrateSelfCharacter(mbgra,i_am,_flip,_debug);
		
		env->ReleaseIntArrayElements(bgra, _bgra, 0);
		
		return retval;
	}
	
		
	jfloatArray GoDetector(JNIEnv* env, Mat& mbgra, jboolean i_am, jboolean _flip, jboolean _debug) {
		detector.findCharacter(mbgra, i_am, _flip, _debug);
				
		int slfChrSz = detector.selfCharacter.size();
		int othrChrSz = detector.otherCharacter.size();
		jfloat a[11] = //{0.0f};
		{ 
			(slfChrSz>0) ? detector.selfCharacter[0].x : -1.0f,
			(slfChrSz>0) ? detector.selfCharacter[0].y : -1.0f,
			(slfChrSz>1) ? detector.selfCharacter[1].x : -1.0f,
			(slfChrSz>1) ? detector.selfCharacter[1].y : -1.0f,
			(othrChrSz>0) ? detector.otherCharacter[0].x : -1.0f,
			(othrChrSz>0) ? detector.otherCharacter[0].y : -1.0f,
			(othrChrSz>1) ? detector.otherCharacter[1].x : -1.0f,
			(othrChrSz>1) ? detector.otherCharacter[1].y : -1.0f,
			detector.getWaveTimer(),
			detector.tracking ? 1.0f : 0.0f,
			(float)detector.getSizeOfSelf()
		};
		
		jfloatArray state = env->NewFloatArray(11);
		env->SetFloatArrayRegion(state,0,11,a);
		return state;	
	}
		
//	VideoCapture vc;
	Mat frame;
	int frame_index;
	string frame_prefix;
	Mat alpha;

#define ERROR_FILE_DOESNT_EXIST -1
#define ERROR_FRAME_DATA_NULL -2
#define ERROR_VIDEOCAPTURE_NOT_OPEN -3
		
	JNIEXPORT jintArray JNICALL Java_edu_mit_media_fluid_royshil_headfollower_FileFrameProcessor_OpenFromFile(
				JNIEnv* env, 
				jobject thiz,
				jstring filelocation
				)
	{
		const char* _str = env->GetStringUTFChars(filelocation, 0);
		frame_prefix = _str;
		env->ReleaseStringUTFChars(filelocation, _str);

		jintArray retinta = env->NewIntArray(2);
		int reta[2];

		frame_index = 1;
		stringstream frame_name; frame_name << frame_prefix << frame_index << ".png";
		struct stat BUF;
        if(stat(frame_name.str().c_str(),&BUF)==0)
        {
//			if(vc.open(ret)) 
			{
//				if (vc.isOpened()) 
				{
//					vc >> frame;
					frame = imread(frame_name.str());
					
					if(frame.data) {
						frame_index++;
						detector.shouldResize = false;
						reta[0] = frame.cols;
						reta[1] = frame.rows;
						
						alpha.create(frame.size(),CV_8UC1);
						alpha.setTo(255);
					} else 
						reta[0] = ERROR_FRAME_DATA_NULL;
				} 
//				else 
//					reta[0] = ERROR_VIDEOCAPTURE_NOT_OPEN;
			} 
//			else
//				reta[0] = ERROR_VIDEOCAPTURE_NOT_OPEN;
		} else
			reta[0] = ERROR_FILE_DOESNT_EXIST;

		env->SetIntArrayRegion(retinta,0,2,reta);
		return retinta;
	}
	
	JNIEXPORT jfloatArray JNICALL Java_edu_mit_media_fluid_royshil_headfollower_CharacterTrackerView_ProcessFileFrame(
				JNIEnv* env, 
				jobject thiz,
				jint width, 
				jint height, 
				jintArray bgra,
				jint i_am, 
				jboolean _flip, 
				jboolean _debug )
	{
		//get the output byte array to work on
		jint*  _bgra = env->GetIntArrayElements(bgra, 0);		
		Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
		
//		vc >> frame;
		stringstream frame_name; frame_name << frame_prefix << frame_index << ".png";
		frame = imread(frame_name.str());
		frame_index++;
				
		jfloatArray retval = GoDetector(env, frame, i_am, _flip, _debug);

		int fromTo[8] = {0,0, 1,1, 2,2, 3,3};
		Mat srcs[2] = {frame,alpha};
		mixChannels(srcs,2,&mbgra,1,fromTo,4); // fill the buffer..

//		jfloatArray retval = env->NewFloatArray(11);
//		jfloat flta[11] = {1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f};
//		env->SetFloatArrayRegion(retval,0,11,flta);
		
		env->ReleaseIntArrayElements(bgra, _bgra, 0);
		
		return retval;
	}
	
	JNIEXPORT void JNICALL Java_edu_mit_media_fluid_royshil_headfollower_CharacterTrackerView_WriteFrame(
			  JNIEnv* env, 
			  jobject thiz,
			  jint width, 
			  jint height, 
			  jbyteArray yuv
				)	
	{
		jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
		
		Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
		Mat bgr;
		
		cvtColor(myuv, bgr, CV_YUV420sp2BGR);
		
		env->ReleaseByteArrayElements(yuv, _yuv, 0);
		
		stringstream ss; ss << "/sdcard/saved/frame" << frame_index++ << ".png";
		imwrite(ss.str(),bgr);
	}
																													  
}
