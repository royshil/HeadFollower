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

void GetPointsUsingBlobs(vector<Point>& _points, Mat& img, Mat& hsv, int i_am, bool _debug) {
	_points.clear();
	
	Mat blobmask;
	
	{		
		if(i_am == IAM_BLUE) {
			inRange(hsv, Scalar(0,80,210), Scalar(37,256,256), blobmask);
		} else if (i_am == IAM_RED) {
			inRange(hsv, Scalar(85,45,100), Scalar(120,256,256), blobmask);
		}
	}
	
//	cvtColor(blobmask,img,CV_GRAY2RGB);
	
	imshow("blobmask",blobmask);
	
	vector<vector<Point> > contours;
	{
		Mat __tmp; blobmask.copyTo(__tmp);
		findContours( __tmp, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE );
	}
		
    int idx = 0, largestComp = -1, secondlargest = -1;
	float maxarea = -1.0f, secondmaxarea = -1.0f;
	
	for (; idx<contours.size(); idx++)
    {
        const vector<Point>& c = contours[idx];
		float area = (float)(contourArea(Mat(c)));	//TODO: add previous detected marker distance
		if(area < 10 || area > 1000) continue;
		
		int num = contours[idx].size();
		Point* pts = &(contours[idx][0]);
		if(_debug)
			fillPoly(img, (const Point**)(&pts), &num, 1, Scalar(255,255,0));
		
		//make a "similar" circle to match to
		Scalar _mean = mean(Mat(contours[idx]));
		
		if(_debug) {
			Vec3b hsvv = hsv.at<Vec3b>(_mean[1],_mean[0]);
			stringstream ss; ss << "h " << (int)hsvv[0] << " s " << (int)hsvv[1] << " v " << (int)hsvv[2];
			putText(img,ss.str(),Point(_mean[0],_mean[1]),CV_FONT_HERSHEY_PLAIN,1.0,Scalar(255,255),1);
		}
		
//		circle(img, Point(_mean[0],_mean[1]),2,Scalar(0,0,255),1);
		vector<Point> circlepts;
		ellipse2Poly(Point(_mean[0],_mean[1]), Size(10,10),0,0,360,6,circlepts);
//		vector<vector<Point> > _circlepts; _circlepts.push_back(circlepts);
//		drawContours(img,_circlepts,0,Scalar(0,255,0));
		
		double ellipsematch = matchShapes(Mat(contours[idx]), Mat(circlepts),CV_CONTOURS_MATCH_I2,0.0);
		if (ellipsematch < 0.5) { //this is just not a circle..
			continue;
		}
//		if(_debug) {
//			stringstream ss; ss << setprecision(3) << "a = " << area << ", e = " << ellipsematch;
//			putText(img,ss.str(),Point(_mean[0],_mean[1]),CV_FONT_HERSHEY_PLAIN,1.0,Scalar(255,255),1);
//		}
		
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
	for (int i=0; i<contours.size(); i++) { 
		if(i==secondlargest || i==largestComp) {
			int num = contours[i].size();
			Point* pts = &(contours[i][0]);
			
			if(_debug) fillPoly(img, (const Point**)(&pts), &num, 1, Scalar((i_am == IAM_BLUE)?255:0,(i_am == IAM_RED)?255:0,255));
			
			Scalar _mean = mean(Mat(contours[i]));
			_points.push_back(Point(_mean[0],_mean[1]));
		}
	}
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
		Mat roi(hue, trackWindow1), maskroi(trackMask, trackWindow1);
		calcHist(&roi, 1, 0, maskroi, hist, 1, &hsize, &phranges);

		//Histogram of other candidate marker
		Mat hist1;
		Mat roi1(hue, trackWindow2), maskroi1(trackMask, trackWindow2);
		
		calcHist(&roi1, 1, 0, maskroi1, hist1, 1, &hsize, &phranges);
		
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
		{
			//Draw histogram image
			histimg = Scalar::all(0);
			int binW = histimg.cols / hsize;
			Mat buf(1, hsize, CV_8UC3);
			for( int i = 0; i < hsize; i++ )
				buf.at<Vec3b>(i) = Vec3b(saturate_cast<uchar>(i*180./hsize), 255, 255);
			cvtColor(buf, buf, CV_HSV2BGR);
			
			for( int i = 0; i < hsize; i++ )
			{
				int val = saturate_cast<int>(hist.at<float>(i)*histimg.rows);
				rectangle( histimg, Point(i*binW,histimg.rows),
						  Point((i+1)*binW,histimg.rows - val),
						  Scalar(buf.at<Vec3b>(i)), -1, 8 );
			}
		}
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
	calcBackProject(&hue, 1, 0, hist, backproj, &phranges);
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

//#define CALIBRATE_NOT_FOUND 0
//#define CALIBRATE_SEND_EXTRA_MARKER 1
//#define CALIBRATE_FOUND 2

int Detector::calibrateSelfCharacter(Mat& _img, int i_am, bool _flip, bool _debug) {
	if(!_img.data) return false;
	
	setupImages(_img,_flip);
	
	//self localization
	if(calibration_state == CALIBRATE_NOT_FOUND) {
		GetPointsUsingBlobs(selfCharacter, img, hsv, (i_am==IAM_RED)?IAM_BLUE:IAM_RED, _debug);
	}
	
	if (selfCharacter.size() == 2) {
		if(calibration_state == CALIBRATE_NOT_FOUND) {
			return (calibration_state = CALIBRATE_SEND_EXTRA_MARKER);
		} else if (calibration_state == CALIBRATE_SEND_EXTRA_MARKER) {
			if(!hue.data)
				hue.create(hsv.size(), hsv.depth());
			int ch[] = {0, 0};
			mixChannels(&hsv, 1, &hue, 1, ch, 1); //prepare hue data for extra marker
			if (FindExtraMarker(selfCharacter)) {
				//extra marker found -> position of self markers found
				return (calibration_state = CALIBRATE_FOUND);
			} else {
				return (calibration_state = CALIBRATE_NOT_FOUND); //back to looking for self markers
			}
		}
	} else //not enough points to start calibration
		return (calibration_state = CALIBRATE_NOT_FOUND);
	
	return (calibration_state = CALIBRATE_NOT_FOUND);
}

/**
 * Returns 4 points: first 2 is other character, second 2 is self character
 */
//#ifndef _PC_COMPILE
//bool Detector::findCharacter(int idx, image_pool* pool, int i_am, bool _flip, bool _debug) {
//	Mat _img = pool->getImage(idx),
//#else
bool Detector::findCharacter(Mat& _img, int i_am, bool _flip, bool _debug) {
	if(!_img.data) return false;
	
	setupImages(_img,_flip);
	
	if(!tracking) {
		//Initialize position of markers
		cout << "BLOB DETECT" << endl;
		//GetPointsUsingBlobs(otherCharacter, img, hsv, i_am, _debug);
		tracking = otherCharacter.size() >= 2;
		if(tracking)
			cout << "BEGIN TRACKING" << endl;
//		KF[0].statePre.at<float>(0) = this->otherCharacter[0].x;
//		KF[0].statePre.at<float>(1) = this->otherCharacter[0].y;
//		KF[1].statePre.at<float>(0) = this->otherCharacter[1].x;
//		KF[1].statePre.at<float>(1) = this->otherCharacter[1].y;
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
	
	return true;
}
	
#define Point2Vec2f(p) Vec2f((p).x,(p).y)
#define Vec2f2Point(v) Point((v)[0],(v)[1])
	
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
	
	Mat roi = hue(Rect(extraMarkerPoint.x-10,extraMarkerPoint.y-10,20,20)&Rect(0,0,hue.cols,hue.rows));
	Mat maskRoi = Mat::ones(roi.size(),CV_8UC1) * 255;
	calcHist(&roi, 1, 0, maskRoi, _hist, 1, &hsize, &phranges);

	roi = hue(Rect(pts[0].x-10,pts[0].y-10,20,20)&Rect(0,0,hue.cols,hue.rows));
	maskRoi = Mat::ones(roi.size(),CV_8UC1) * 255;
	calcHist(&roi, 1, 0, maskRoi, hist, 1, &hsize, &phranges);
	
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