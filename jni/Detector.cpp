/*
 * Detector.cpp
 *
 *  Created on: Aug 1, 2010
 *      Author: roys
 */

#include "Detector.h"


#include <sys/stat.h>

#include <iostream>
#include <sstream>
#include <iterator>
#include <iomanip>

using namespace std;
using namespace cv;

//void GetCandidatePoints(vector<Point2f>& _points, Mat& img, Mat& gray, Mat& hsv, bool redOrBlue) {
//	vector<Point2f> corners;
//	_points.clear();
//	
//	Mat _tmp; img.copyTo(_tmp);
//	vector<Mat> chns; split(_tmp, chns);
//	
////	for (int cn=0; cn<chns.size(); cn++) 
//	int cn = (redOrBlue) ? 2 : 0;
//	{
////		{
////			stringstream ss; ss<<"channel "<<cn; imshow(ss.str(), chns[cn]);
////		}
//		
//		goodFeaturesToTrack(chns[cn], corners, 20, 0.01, 40.0);
//		cvtColor(chns[cn],img,CV_GRAY2RGB);
//		for (int i=0; i<corners.size(); i++) {
//			Vec3b hsvv = hsv.at<Vec3b>(corners[i]);
//			{	//high saturation
//				stringstream ss; ss << "(" << (int)hsvv.val[0] << "," << (int)hsvv.val[1] << "," << (int)hsvv.val[2] << ")";
//				putText(img, ss.str(), corners[i], CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255), 1);
//				int h = hsvv[0], s = hsvv[1], v = hsvv[2];
//				if((cn == 0 && h > 110 && h < 130 && s > 100 && v > 200)||		//red channel
//				   //(cn == 1 && hsvv[1] > 40 && hsvv[2] > 80 && hsvv[0] > 50 && hsvv[0] < 100)||		//green channel
//				   (cn == 2 && (h < 15 || h > 170) && s > 70 && v > 150)||			//blue channel
//				   false) {
//					circle(img, corners[i], 3, Scalar(0,255,0), 2);
//					
//					_points.push_back(corners[i]);
//				}
//				else {
//					circle(img, corners[i], 3, Scalar(0,255,255), 2);
//				}
//			}
//		}
//	}
//	stringstream ss; ss << "cn=" << cn << ",rOb=";
//	if(redOrBlue) ss << "red";
//			 else ss << "blue";
//	putText(img,ss.str(),Point(10,15),CV_FONT_HERSHEY_PLAIN,1.0,Scalar(255,255),1);
//}

vector<int> GetPointsUsingBlobs(vector<Point>& _points, Mat& img, Mat& hsv, bool get_all_blobs, int i_am, bool _debug) {
	_points.clear();
	
	vector<int> state(3);
	state[0] = state[1] = state[2] = 0;
	
	Mat blobmask;
	
	{		
		if(i_am == IAM_BLUE) {
			inRange(hsv, Scalar(0,80,210), Scalar(37,256,256), blobmask);
		} else if (i_am == IAM_RED) {
			inRange(hsv, Scalar(85,45,100), Scalar(120,256,256), blobmask);
		}
	}
	
//	cvtColor(blobmask,img,CV_GRAY2RGB);

//#ifdef _PC_COMPILE
//	imshow("blobmask",blobmask);
//#endif
	
	vector<vector<Point> > contours;
	{
		Mat __tmp; blobmask.copyTo(__tmp);
		findContours( __tmp, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE );
	}
		
	state[0] = contours.size();
	
    int idx = 0, largestComp = -1, secondlargest = -1;
	float maxarea = -1.0f, secondmaxarea = -1.0f;
	
	for (; idx<contours.size(); idx++)
    {
        const vector<Point>& c = contours[idx];
		float area = (float)(contourArea(Mat(c)));	//TODO: add previous detected marker distance
		if(area < 100 || area > 1000) continue;
		state[1]++;
		
		int num = contours[idx].size();
		Point* pts = &(contours[idx][0]);
		
		//make a "similar" circle to match to
		Scalar _mean = mean(Mat(contours[idx]));
				
//		circle(img, Point(_mean[0],_mean[1]),2,Scalar(0,0,255),1);
		vector<Point> circlepts;
		ellipse2Poly(Point(_mean[0],_mean[1]), Size(10,10),0,0,360,6,circlepts);
//		vector<vector<Point> > _circlepts; _circlepts.push_back(circlepts);
//		drawContours(img,_circlepts,0,Scalar(0,255,0));
		
		double ellipsematch = matchShapes(Mat(contours[idx]), Mat(circlepts),CV_CONTOURS_MATCH_I2,0.0);
		if (ellipsematch > 0.1) { //this is just not a circle..
			continue;
		}
		state[2]++;

		if(_debug) {
			fillPoly(img, (const Point**)(&pts), &num, 1, Scalar(255,255,0));
			Vec3b hsvv = hsv.at<Vec3b>(_mean[1],_mean[0]);
			stringstream ss; ss << "h " << (int)hsvv[0] << " s " << (int)hsvv[1] << " v " << (int)hsvv[2];
			putText(img,ss.str(),Point(_mean[0],_mean[1]),CV_FONT_HERSHEY_PLAIN,1.0,Scalar(255,255),1);
		}
		
//		if(_debug) {
//			stringstream ss; ss << setprecision(3) << "a = " << area << ", e = " << ellipsematch;
//			putText(img,ss.str(),Point(_mean[0],_mean[1]),CV_FONT_HERSHEY_PLAIN,1.0,Scalar(255,255),1);
//		}
		
		if(get_all_blobs) {
			_points.push_back(Point(_mean[0],_mean[1]));
			continue;
		}
		
		area = area / ellipsematch;
		
		if(area > maxarea) {  //largest overthrown
			secondlargest = largestComp; 
			secondmaxarea = maxarea; 
			largestComp = idx; 
			maxarea = area;
		} else if(area > secondmaxarea) { //second largest overthrown
			secondlargest = idx; 
			secondmaxarea = area; 
		}
    }	
	if (get_all_blobs) { //skip getting only the top two
		return state;
	}
	for (int i=0; i<contours.size(); i++) { 
		if(i==secondlargest || i==largestComp) {
			int num = contours[i].size();
			Point* pts = &(contours[i][0]);
			
			if(_debug) fillPoly(img, (const Point**)(&pts), &num, 1, Scalar((i_am == IAM_BLUE)?255:0,(i_am == IAM_RED)?255:0,255));
			
			Scalar _mean = mean(Mat(contours[i]));
			_points.push_back(Point(_mean[0],_mean[1]));
		}
	}
	return state;
}

double Detector::getSizeOfSelf() {
	if (otherCharacter.size()>=2 && selfCharacter.size()>=2) {
		return norm(Vec2i(otherCharacter[0]-otherCharacter[1]))/norm(Vec2i(selfCharacter[0]-selfCharacter[1]));
	} else {
		return 1.0;
	}
}

void Detector::TrackPoints(Rect markers[], bool _debug) {
	Rect trackWindow1 = markers[0],trackWindow2 = markers[1];
	
	//Create mask out of pixels in HSV value range
	inRange(hsv,	Scalar(0,		10,		80),
			//             V        V       V
					Scalar(180,		256,	256), 
			trackMask);
	//imshow("trackmask",trackMask);
	
	//Get only hue channel
	int ch[] = {0, 0};
	if(!hue.data)
		hue.create(hsv.size(), hsv.depth());
	mixChannels(&hsv, 1, &hue, 1, ch, 1);
	
	//New object selection - calculate new histogram
	if( this->trackObject < 0 )
	{
		cout << "NEW HISTOGRAM" << endl;
		//Get histogram over hue channel
//		int histchannels[] = {1};
		Mat roi(hsv, trackWindow1), maskroi(trackMask, trackWindow1);
		calcHist(&roi, 1, 0, maskroi, hist, 1, hsize, phranges);

		//Histogram of other candidate marker
		Mat hist1;
		Mat roi1(hsv, trackWindow2), maskroi1(trackMask, trackWindow2);
		
		calcHist(&roi1, 1, 0, maskroi1, hist1, 1, hsize, phranges);
		
		hist = hist + hist1; //combine histograms
		
		normalize(hist, hist, 0.0, 1.0, CV_MINMAX);
				
//		//Shift 8 cells of the histogram to the left, because red values are split on the 180 value line
//		Mat_<float> tmp(hist.size()); 
//		for (int i=0; i<hist.rows/2; i++) {
//			tmp(0,i) = hist.at<float>(hist.rows/2+i);
//			tmp(0,hist.rows/2+i) = hist.at<float>(i);
//		}
//		for (int i=0; i<hist.rows; i++) {
//			cout << (int)round(tmp(i)) << ",";
//		}
//		cout << endl;
		
#ifdef _PC_COMPILE
//		{
//			//Draw histogram image
//			histimg = Scalar::all(0);
//			int binW = histimg.cols / hsize[0];
//			Mat buf(1, hsize, CV_8UC3);
//			for( int i = 0; i < hsize[0]; i++ )
//				buf.at<Vec3b>(i) = Vec3b(saturate_cast<uchar>(i*180./hsize[0]), 255, 255);
//			cvtColor(buf, buf, CV_HSV2BGR);
//			
//			for( int i = 0; i < hsize[0]; i++ )
//			{
//				int val = saturate_cast<int>(hist.at<float>(i)*histimg.rows);
//				rectangle( histimg, Point(i*binW,histimg.rows),
//						  Point((i+1)*binW,histimg.rows - val),
//						  Scalar(buf.at<Vec3b>(i)), -1, 8 );
//			}
//		}
#endif
		
		//calculate variance of histogram, and this will be the measure to how good it is
		//low variance = a good capture of the color = a good location of the marker
		Scalar _mean,_stddev;
//		
//		//multiply histogram count by value to get E[X] (mean)
//		vector<float> mults;
//		{
//			float step_ = 180.0f / (float)hist.rows;
//			for (int i=0; i<hist.rows; i++) {
//				mults.push_back((float)i * step_ + step_/2.0f);
//			}
//		}
//		
//		tmp = tmp.mul(Mat(mults));
		
		//putting both marker sub-images in the same image
		Mat both(Size(roi.cols+roi1.cols,MAX(roi.rows,roi1.rows)),roi.type());
		Mat left = both(Rect(Point(0,0),roi.size())); roi.copyTo(left);
		Mat right = both(Rect(Point(roi.cols,0),roi1.size())); roi1.copyTo(right);
		
		//converto to [-90,90]
		Mat both_8SC(both.size(),CV_8SC1); 
		both.copyTo(both_8SC, both <= 90);
		Mat(both + -180).copyTo(both_8SC, both > 90);
		
		meanStdDev(both_8SC, _mean, _stddev);

#ifdef _PC_COMPILE		
		{
			stringstream ss; ss << "Stdv = " << _stddev[0];
			putText(histimg, ss.str(), Point(10,10), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255), 2);
			line(histimg, Point((_mean[0]+90)*histimg.cols/180.0,0), Point((_mean[0]+90)*histimg.cols/180.0,histimg.rows), Scalar(255), 4);
			imshow( "Histogram", histimg );
		}
#endif
		
		if (_stddev[0] > 20) { //std.deviation too high for a coherent marker
			this->trackObject = -1;
			this->tracking = false;
			if(_debug) { cout << "HISTOGRAM NOT COHERENT" << endl; }
			return;
		} else {
			this->trackObject = 1; //all good, Begin tracking
		}
	}
	
	//Calc histogram back-projection (can be shared between 2 markers, as they have the same color..)
	calcBackProject(&hsv, 1, 0, hist, backproj, phranges);
	backproj &= trackMask;
//	imshow("backproj",backproj);
	
	//Track object on back-projection
	RotatedRect trackBox1 = CamShift(backproj, trackWindow1, TermCriteria( CV_TERMCRIT_EPS | CV_TERMCRIT_ITER, 10, 1 ));
	RotatedRect trackBox2 = CamShift(backproj, trackWindow2, TermCriteria( CV_TERMCRIT_EPS | CV_TERMCRIT_ITER, 10, 1 ));
	
	if( trackWindow1.area() <= 1 || trackWindow2.area() <= 1) {
		this->tracking = false;
		this->trackObject = -1;
		cout << "LOST A MARKER" << endl;
		return;
	}
//	{
//		int cols = backproj.cols, rows = backproj.rows, r = (MIN(cols, rows) + 5)/6;
//		trackWindow = Rect(trackWindow.x - r, trackWindow.y - r,
//						   trackWindow.x + r, trackWindow.y + r) &
//		Rect(0, 0, cols, rows);
//	}
	
//	if( backprojMode )
//		cvtColor( backproj, image, CV_GRAY2BGR );
//	ellipse( img, trackBox1, Scalar(0,0,255), 3, CV_AA );	
//	ellipse( img, trackBox2, Scalar(0,0,255), 3, CV_AA );	
	
	this->otherCharacter[0] = trackBox1.center;
	this->otherCharacter[1] = trackBox2.center;
	
	cout << "TRACKING OK" << endl;
}

void Detector::KalmanSmooth() {
	for (int i=0; i<2; i++) {
		Mat prediction = KF[i].predict();
		Point predictPt(prediction.at<float>(0),prediction.at<float>(1));
		this->measurement.at<Point2f>(0) = this->otherCharacter[i];
//		Point2f* _ptr = &((this->measurement).at<Point2f>(0));
//		*_ptr = this->otherCharacter[i];
		//	this->measurement(1) = this->otherCharacter[0].y;
		
		//Point measPt(measurement(0),measurement(1));
		//mousev.push_back(measPt);
		// generate measurement
		//measurement += KF.measurementMatrix*state;
		
		Mat estimated = KF[i].correct(measurement);
		Point statePt(estimated.at<float>(0),estimated.at<float>(1));
		//kalmanv.push_back(statePt);
		
		//TODO: if error is very high - get out of tracking mode
		
		this->otherCharacter[i] = statePt;
	}
}	

vector<int> Detector::calibrateSelfCharacter(Mat& _img, int i_am, bool _flip, bool _debug) {
	vector<int> state(4);
	state[0] = state[1] = state[2] = state[3] = -1;
	
	if(!_img.data) return state;
	
	setupImages(_img,_flip);
	
	if(calibration_state == CALIBRATE_NO_MARKERS_FOUND) {
		//self localization, look for self markers
		vector<int> blos_state = GetPointsUsingBlobs(selfCharacter, img, hsv, false, (i_am==IAM_RED)?IAM_BLUE:IAM_RED, _debug);
		state[1] = blos_state[0]; state[2] = blos_state[1]; state[3] = blos_state[2];
	}
	
	if (selfCharacter.size() == 2) {
		if(calibration_state == CALIBRATE_NO_MARKERS_FOUND) {
			look_for_extra_marker_count = 0;
			calibration_state = CALIBRATE_SEND_EXTRA_MARKER;
		} else if (calibration_state == CALIBRATE_SEND_EXTRA_MARKER || calibration_state == CALIBRATE_NO_EXTRA_MARKER_FOUND) {
			look_for_extra_marker_count++;
			if (FindExtraMarkerUsingBlobs(i_am)) {
				//extra marker found -> position of self markers found
				calibration_state = CALIBRATE_FOUND;
			} else {
				//Give it 10 frames to look for the marker before giving up
				calibration_state = CALIBRATE_NO_EXTRA_MARKER_FOUND;
				if (look_for_extra_marker_count > 5) {
					calibration_state = CALIBRATE_NO_MARKERS_FOUND; //ok give up
				}
			}
		}
	} else //not enough points to start calibration
		calibration_state = CALIBRATE_NO_MARKERS_FOUND;
	
	img.copyTo(_img);
//	int fromTo[] = {0,0, 1,1, 2,2};
//	mixChannels(&img, 1, &_img, 1, fromTo, 3);
	state[0] = calibration_state;
	
	return state;
}

/**
 * Returns 4 points: first 2 is other character, second 2 is self character
 */
//#ifndef _PC_COMPILE
//bool Detector::findCharacter(int idx, image_pool* pool, int i_am, bool _flip, bool _debug) {
//	Mat _img = pool->getImage(idx),
//#else
vector<int> Detector::findCharacter(Mat& _img, int i_am, bool _flip, bool _debug) {
	vector<int> state(4,-1);
	
	if(!_img.data) return state;
	
	setupImages(_img,_flip);
	
	if(!tracking) {
		//Initialize position of markers
#ifdef _PC_COMPILE
		cout << "BLOB DETECT" << endl;
#endif
		vector<int> blobs_state = GetPointsUsingBlobs(otherCharacter, img, hsv, false, i_am, _debug);
		state[1] = blobs_state[0];
		state[2] = blobs_state[1];
		state[3] = blobs_state[2];
		tracking = otherCharacter.size() >= 2;
#ifdef _PC_COMPILE
		if(tracking)
			cout << "BEGIN TRACKING" << endl;
#endif
	} 
	
	tracking = false;
	
	if (tracking) {
		//Track position of markers
		Rect markers[2] = {	Rect(this->otherCharacter[0]-Point(10,10),Size(20,20)),
							Rect(this->otherCharacter[1]-Point(10,10),Size(20,20))};
//		rectangle(img, markers[0], Scalar(255), 2);
//		rectangle(img, markers[1], Scalar(255), 2);
		TrackPoints(markers, _debug); //this->tracking may change here
		
		//TODO: check if tracking died, converged to one point, or OK
		if (norm(this->otherCharacter[0] - this->otherCharacter[1]) < 100) {
			//seems like markers degenrated
			tracking = false;
		}
	}
	 

#define DRAW_CROSS(img,pt) line(img,pt-Point(5,0),pt+Point(5,0),Scalar(0,255,0),2); \
						   line(img,pt-Point(0,5),pt+Point(0,5),Scalar(0,255,0),2); 
	
	if (otherCharacter.size() >= 2) {
		//Kalman filter to smooth position of markers
		if(!kalman_setup) setupKalmanFilter();
		KalmanSmooth();

		//look for extra marker
		other_extra_marker_found = FindExtraMarker(otherCharacter);

		if(_debug) {
			DRAW_CROSS(img,this->otherCharacter[0])
			DRAW_CROSS(img,this->otherCharacter[1])
		}
		
		state[0] = 1;
	}
	
	if (// both characters visible 
		otherCharacter.size()>=2 && selfCharacter.size()>=2 && 
		// good vertical alignment 
		fabs(getSelfCenter().y - getOtherCenter().y) < 10.0f) {
		//increase alignment timer
		waveTimer = MIN(waveTimer + 1,30);

	} else {
		waveTimer = MAX(0,waveTimer - 1);
	}


	if(_debug) {
		if (shouldResize) {
			resize(img,_img,_img.size());	//so we'll have some feedback on screen
		} else {
			img.copyTo(_img);
		}
	}
	
	return state;
}
	
#define Point2Vec2f(p) Vec2f((p).x,(p).y)
#define Vec2f2Point(v) Point((v)[0],(v)[1])

bool Detector::FindExtraMarkerUsingBlobs(int i_am) {
	vector<Point> blobs;
	//get all the good colored good shaped blobs
	GetPointsUsingBlobs(blobs, img, hsv, true, (i_am==IAM_RED)?IAM_BLUE:IAM_RED, false);
	
	if (blobs.size() != 3) {
		return false; //we can only work if we find exactly 3 blobs..
	}
	
	//look for 90-degree angle between the three
	//there can be three configurations: 1-2-3, 2-3-1, 3-1-2
	for (int i=0; i<3; i++) {
		Vec2f a = Point2Vec2f(blobs[i] - blobs[(i+1)%3]);
		float na = norm(a);
		Vec2f an = a * (1.0f / na);
		Vec2f b = Point2Vec2f(blobs[(i+1)%3] - blobs[(i+2)%3]);
		float nb = norm(b);
		Vec2f bn = b * (1.0f / nb);

#ifdef _PC_COMPILE
		Mat tmp; img.copyTo(tmp);
		line(tmp, blobs[i], blobs[(i+1)%3], Scalar(255), 2);
		line(tmp,blobs[(i+1)%3],blobs[(i+2)%3],Scalar(0,255),2);

		stringstream ss; ss<<"abs(dotp): "<<abs(an.dot(bn));
		putText(tmp, ss.str(), Point(10,10), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255,255), 1);
		ss.str(""); ss<<"na/nb: "<<na/nb;
		putText(tmp, ss.str(), Point(10,25), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255,255), 1);
		ss.str(""); ss<<"nb/na: "<<nb/na;
		putText(tmp, ss.str(), Point(10,50), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255,255), 1);
		imshow("tempp", tmp);
		waitKey(0);
#endif
		
		if(abs(an.dot(bn)) < DETECTOR_EPSILON && 
		   (fabsf((na / nb) - 0.56) < DETECTOR_TIGHT_EPSILON || fabsf((nb / na) - 0.56) < DETECTOR_TIGHT_EPSILON)) {
			//(pretty much) 0 dot product says they are perpendicular
			//and look for a ratio of (pretty much) 0.75 between the lengths
			//TODO: why is it 0.56??
			return true;
		}
	}
	return false;
}
	
bool Detector::FindExtraMarker(vector<Point>& pts) {
	Vec2f pa = Point2Vec2f(pts[0]) - Point2Vec2f(pts[1]); //principle_axis
	float angle = atan2(3.0, 4.0); //the angle between the diagonal and length
	//get the vector from the upper marker to the place of the extra-marker
	Vec2f rotated_upper(pa[0]*cos(angle)+pa[1]*(-sin(angle)), pa[0]*sin(angle)+pa[1]*cos(angle));
	rotated_upper = rotated_upper * 0.8; // 4/5 is the ratio between the diagonal and the length of the rectangle

	Point extraMarkerPoint = pts[1]+Vec2f2Point(rotated_upper);
	if(!extraMarkerPoint.inside(Rect(0,0,img.cols,img.rows))) return false;

#ifdef _PC_COMPILE	
//	line(img, otherCharacter[0], otherCharacter[0]+Vec2f2Point(rotated_lower), Scalar(0,0,255), 2);
//	line(img, otherCharacter[1], extraMarkerPoint, Scalar(0,0,255), 2);
//	line(img, otherCharacter[0], extraMarkerPoint, Scalar(0,0,255), 2);
	circle(img, extraMarkerPoint, 10, Scalar(255), 1);
#endif	
	
	//compare histogram of colors within this area to histogram of known marker color
	Mat _hist;
	if(!hue.data) return false; //images not setup properly
	
	Mat roi = hsv(Rect(extraMarkerPoint.x-10,extraMarkerPoint.y-10,20,20)&Rect(0,0,hsv.cols,hsv.rows));
//	Mat maskRoi = Mat::ones(roi.size(),CV_8UC1) * 255;
	int channels[3] = {1,1,1};
	calcHist(&roi, 1, channels, Mat(), _hist, 3, hsize, phranges);

	roi = hsv(Rect(pts[0].x-10,pts[0].y-10,20,20)&Rect(0,0,hsv.cols,hsv.rows));
	calcHist(&roi, 1, channels, Mat(), hist, 3, hsize, phranges);
	
	double chisqr_test = compareHist(_hist, hist, CV_COMP_CHISQR);
	bool extra_marker_found = (chisqr_test < 50.0);
	
#ifdef _PC_COMPILE
	if (extra_marker_found) {
		putText(img, "EXTRA MARKER", Point(20,20), CV_FONT_HERSHEY_PLAIN, 2.0, Scalar(255), 2);
	}
#endif
	
	return extra_marker_found;
}
	
	/*
void _GetCandidatePoints(vector<Point2f>& points, Mat& img, Mat& gray, Mat& hsv) {
	vector<Point2f> corners;
	points.clear();
	
	goodFeaturesToTrack(gray, corners, 150, 0.01, 50.0);
	for (int i=0; i<corners.size(); i++) {
		Vec3b hsvv = hsv.at<Vec3b>(corners[i]);
		if(hsvv[2] > 200 && hsvv[1] < 50) {	//lightly saturated and high value
			circle(img, corners[i], 3, Scalar(0,255,0), 2);
			stringstream ss; ss << "(" << (int)hsvv.val[0] << "," << (int)hsvv.val[1] << "," << (int)hsvv.val[2] << ")";
			putText(img, ss.str(), corners[i], CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255), 1);
			
			points.push_back(corners[i]);
		}
	}
}

Point2d Detector::_findCharacter(int idx, image_pool* pool, bool _flip, bool _debug) {
	Mat *_img = pool->getImage(idx),
		img,
		gray,
		hsv;
	
	if(!_img) return Point2d(-1,-1);
	
	resize(*_img,img,Size(),0.5,0.5);
	
	//rotate 90 degrees CCW
	double angle = -90.0;
	Point2f src_center(img.rows/2.0, img.rows/2.0);
    Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
    Mat dst;
    warpAffine(img, dst, rot_mat, Size(img.rows,img.cols));
	if(_flip) flip(dst,dst,0);
	dst.copyTo(img);
	
	cvtColor(img, gray, CV_RGB2GRAY);
	cvtColor(img, hsv, CV_BGR2HSV);
	
	GaussianBlur(gray, gray, Size(7,7), 3.0);
	
	vector<Point2f> points;
	GetCandidatePoints(points, img, gray, hsv);
	
	resize(img,dst,_img->size());	//so we'll have some feedback on screen
	dst.copyTo(*_img);
	
	if (points.size() >= 3) {
		//TODO: see that these are the points we are looking for
		
		//get center point
		Scalar s = mean(Mat(points));
		return Point2d(s[0]/img.cols,s[1]/img.rows);
	} else {
		return Point2d(-1,-1);
	}
}
*/