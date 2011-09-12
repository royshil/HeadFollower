#include <stdio.h>


#include "Detector.h"


int main (int argc, const char * argv[]) {
	
	VideoCapture vc;
	Mat frame,img;
	
	Detector d;
	
	VideoWriter vw;
	
	vc.open("video-2011-09-11-20-35-26.avi");
	while (vc.isOpened()) {
		vc >> frame;
		
		if(!frame.data) break;
//		if (!vw.isOpened()) {
//			vw.open("/Users/royshilkrot/Documents/Handheld Projector-Cameras/tracking.AVI", CV_FOURCC('X','V','I','D'), 24.0, frame.size(),true);
//		}
		
		frame.copyTo(img);
		
		d.findCharacter(img, IAM_RED, true, true);
		
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
		
		
		int c = waitKey(30);
		if(c==' ') waitKey(0);
		if(c==27) break;
	}
	
    return 0;
}
