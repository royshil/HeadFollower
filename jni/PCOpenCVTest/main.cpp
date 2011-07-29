#include <stdio.h>


#include "Detector.h"


int main (int argc, const char * argv[]) {
	
	VideoCapture vc("/Users/royshilkrot/Documents/Handheld Projector-Cameras/video-2011-07-27-22-53-48.3gp");
	Mat frame,img;
	
	Detector d;
	
	while (vc.isOpened()) {
		vc >> frame;
		
		if(!frame.data) break;
		
		frame.copyTo(img);
		
		d.findCharacter(img, IAM_BLUE, true, true);
		
		if (d.otherCharacter.size()>=2) {
			line(img, d.otherCharacter[0], d.otherCharacter[1], Scalar(255,0,0), 2);
		}
		if (d.selfCharacter.size()>=2) {
			line(img, d.selfCharacter[0], d.selfCharacter[1], Scalar(0,255,0), 2);
		}

		{
			double sz_of_self = d.getSizeOfSelf();
			stringstream ss; ss << "sz " << sz_of_self;
			putText(img, ss.str(), d.getSelfCenter(), CV_FONT_HERSHEY_PLAIN, 1.0, Scalar(0,0,255), 1);
		}
		
		imshow("temp",img);
		
		int c = waitKey(30);
		if(c==' ') waitKey(0);
		if(c==27) break;
	}
	
    return 0;
}
