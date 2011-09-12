
#ifndef DETECTOR_H_
#define DETECTOR_H_

#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
//#include <opencv2/calib3d/calib3d.hpp>
//#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/video/tracking.hpp>

#include <vector>
using namespace std;

using namespace cv;

#define IAM_BLUE 1
#define IAM_RED 2

class Detector {

//	vector<Point2f> points,nextPoints;
//	vector<uchar> ptsstatus;
//	vector<float> ptserror;
//	bool gatherPoints;
//	long framecount;
//	Mat prevgray;
//	VideoWriter writer;
	
	
	int waveTimer;
	
	Scalar blueHSVThresh, redHSVThresh;
	
	//Meanshift tracker
	int trackObject;
	int hsize;
    float hranges[2];
    const float* phranges;
	Mat hist;
	Mat trackMask;
	Mat hue;
	Mat histimg;
	Mat backproj;
	bool extra_marker_found;
	bool kalman_setup;
	
	Mat _img;
	Mat img;
	Mat gray;
	Mat hsv;
	
	//Kalman filter
	KalmanFilter KF[2];
	Mat_<float> state;
	Mat processNoise;
	Mat_<float> measurement;
	
public:
	bool tracking;
	vector<Point> otherCharacter;
	vector<Point> selfCharacter;

	Detector():waveTimer(0),hsize(16),trackObject(-1),kalman_setup(false) { 
		//Setup Meanshift tracker
		hranges[0] = 0; hranges[1] = 180; phranges = hranges; 
#ifdef _PC_COMPILE
		histimg = Mat::zeros(200, 320, CV_8UC3);
#endif		
	};
	~Detector() {};
	
	void setupKalmanFilter() {
		//Setup Kalman Filter
		for (int i=0; i<2; i++) {
			KF[i] = KalmanFilter(4, 2, 0);
			state = Mat_<float>(4, 1); // (x, y, Vx, Vy) 
			processNoise = Mat(4, 1, CV_32F);
			measurement = (Mat_<float>(2,1) << 0 , 0);
			setIdentity(KF[i].measurementMatrix);
			setIdentity(KF[i].processNoiseCov, Scalar::all(1e-4));
			setIdentity(KF[i].measurementNoiseCov, Scalar::all(1e-4));
			setIdentity(KF[i].errorCovPost, Scalar::all(.1));	
			
			KF[i].transitionMatrix = *(Mat_<float>(4, 4) << 1,0,1,0,   0,1,0,1,  0,0,1,0,  0,0,0,1);
		}
		kalman_setup = true;
	}		
	
	bool findCharacter(Mat& img, int i_am, bool _flip, bool _debug);
	
	void TrackPoints(Rect markers[], bool _debug);
	void KalmanSmooth();
	
	int getPtX(Point* p) { return p->x;}
	int getPtY(Point* p) { return p->y;}
	
	Point getPointFromVector(vector<Point>* v, int idx) { return ((v->size() > idx) ? (*v)[idx] : Point(-1,-1)); }
	
	double getSizeOfSelf();
	
	Point getSelfCenter() { if(selfCharacter.size()>=2) { return (selfCharacter[0]+selfCharacter[1])*.5; } else { return Point(-1,-1); } }
	Point getOtherCenter() { if(otherCharacter.size()>=2) { return (otherCharacter[0]+otherCharacter[1])*.5; } else { return Point(-1,-1); } }
	float getSelfAngle() { return 1.0; }
	int getWaveTimer() { return waveTimer; }
	void FindExtraMarker();
	
};

#endif