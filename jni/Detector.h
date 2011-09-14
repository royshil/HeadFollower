
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

#define CALIBRATE_NO_MARKERS_FOUND 0
#define CALIBRATE_SEND_EXTRA_MARKER 1
#define CALIBRATE_FOUND 2
#define CALIBRATE_NO_EXTRA_MARKER_FOUND 3

#define DETECTOR_EPSILON 0.05
#define DETECTOR_TIGHT_EPSILON 0.01

#define Point2Vec2f(p) Vec2f((p).x,(p).y)
#define Vec2f2Point(v) Point((v)[0],(v)[1])

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
	int hsize[3];
    float hranges[2],sranges[2],vranges[2];
    const float* phranges[3];
	Mat hist;
	Mat trackMask;
	Mat hue;
	Mat histimg;
	Mat backproj;
	bool other_extra_marker_found;
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
	
	int calibration_state;
	int look_for_extra_marker_count;
	
	vector<vector<Point> > calib_history;
	
	float character_to_world_ang;
public:
	bool shouldResize;
	bool tracking;
	vector<Point> otherCharacter;
	vector<Point> selfCharacter;

	Detector():waveTimer(0),
				trackObject(-1),
				kalman_setup(false),
				calibration_state(CALIBRATE_NO_MARKERS_FOUND),
				look_for_extra_marker_count(0),
				shouldResize(false) 
	{ 
		//Setup Meanshift tracker
		hranges[0] = 0; hranges[1] = 180; 
		sranges[0] = 0; sranges[1] = 255;
		vranges[0] = 0; vranges[1] = 255;
		phranges[0] = hranges; 
		phranges[1] = sranges; 
		phranges[2] = vranges; 
		hsize[0] = 20; hsize[1] = 28; hsize[2] = 28;
		
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
	
	vector<int> findCharacter(Mat& img, int i_am, bool _flip, bool _debug);
	
	vector<int> calibrateSelfCharacter(Mat& img, int i_am, bool _flip, bool _debug);
	vector<int> calibrateOtherCharacter(Mat& img, int i_am, bool _flip, bool _debug);
	
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
	bool FindExtraMarker(vector<Point>& pts);
	bool FindExtraMarkerUsingBlobs(int i_am);
	
	void setCalibrationState(int state) { calibration_state = state;}
	
	void setupImages(Mat& _img, bool _flip) { 
#ifndef _PC_COMPILE
		if(shouldResize)
			resize(_img,img,Size(),0.5,0.5);
		else
			_img.copyTo(img);
		
		//rotate 90 degrees CCW
		double angle = -90.0;
		Point2f src_center(img.rows/2.0, img.rows/2.0);
		Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
		Mat dst;
		warpAffine(img, dst, rot_mat, Size(img.rows,img.cols));
		if(_flip) flip(dst,dst,0);
		dst.copyTo(img);
#else
		_img.copyTo(img);
#endif
		
		//	cvtColor(img, img, CV_RGB2BGR);
		//	cvtColor(img, gray, CV_RGB2GRAY);
		cvtColor(img, hsv, CV_BGR2HSV);

//		if(!hue.data)
//			hue.create(hsv.size(), hsv.depth());
//		int ch[] = {0, 0};
//		mixChannels(&hsv, 1, &hue, 1, ch, 1); //prepare hue data for extra marker
	}		
	
};

#endif