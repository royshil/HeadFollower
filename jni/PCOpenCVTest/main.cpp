#include <stdio.h>

#include <iostream>
#include "Detector.h"

Mat img;

void onMouse( int event, int x, int y, int, void* ) {
	cout << "mouse vent" << endl;
	if( event != CV_EVENT_LBUTTONUP )
        return;

	Point p(x,y);
//	
//	
//	Mat img_roi = img(Rect(p.x-5,p.y-5,10,10));
//	Mat hsv_; cvtColor(img_roi, hsv_, CV_BGR2HSV);
//	Scalar mean_ = mean(hsv_);
//	stringstream ss; ss << mean_.val[0] << "," << mean_.val[1] << "," << mean_.val[2];
	stringstream ss; ss<<p.x<<","<<p.y;
	putText(img, ss.str(), p-Point(20,20), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(255), 1);
//	circle(img, p, 10, Scalar(255), 2);
	
	imshow("temp",img);
	waitKey(0);
}

int main (int argc, const char * argv[]) {
	
	VideoCapture vc;
	Mat frame;
	
	Detector d;
	
	VideoWriter vw;
	
	namedWindow("temp");
	setMouseCallback("temp", onMouse, 0);
	
	vector<int> state(1);
	state[0] = CALIBRATE_NO_MARKERS_FOUND;
	
	//vc.open("video-2011-09-11-20-35-26.avi");
	int frame_index = 1;
	while (/*vc.isOpened()*/ frame_index < 100) {
//		vc >> frame;
		stringstream ss; ss<<"/tmp/from_device/frame"<<frame_index++<<".png";
		frame = imread(ss.str());
		cout << ss.str() << endl;
		
		if(!frame.data) {
			cerr << "can't read." <<endl;
			continue;
		}
//		if (!vw.isOpened()) {
//			vw.open("/Users/royshilkrot/Documents/Handheld Projector-Cameras/tracking.AVI", CV_FOURCC('X','V','I','D'), 24.0, frame.size(),true);
//		}
		
		frame.copyTo(img);
		
		img = img(Rect(159,59,474-159,429-59));
		
//		if(state[0] != CALIBRATE_FOUND) {
			state = d.calibrateSelfCharacter(img, IAM_RED, false, true);
//		} if(state[0] == CALIBRATE_FOUND) {
//			d.findCharacter(img, IAM_RED, true, true);
//		}
		
//		if (d.otherCharacter.size()>=2) {
//			line(img, d.otherCharacter[0], d.otherCharacter[1], Scalar(255,0,0), 2);
//		}
//		if (d.selfCharacter.size()>=2) {
//			line(img, d.selfCharacter[0], d.selfCharacter[1], Scalar(0,255,0), 2);
//		}

		{
			double sz_of_self = d.getSizeOfSelf();
			stringstream ss; ss << "sz " << sz_of_self;
			putText(img, ss.str(), d.getSelfCenter(), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(0,0,255), 1);
		}
		
		imshow("temp",img);
//		vw.write(img);
		
		
		int c = waitKey(300);
		if(c==' ') waitKey(0);
		if(c==27) break;
	}
	
    return 0;
}
