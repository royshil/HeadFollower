/*
 * include the headers required by the generated cpp code
 */
%{
#include "Detector.h"
#include "image_pool.h"
using namespace cv;
%}

//import the android-cv.i file so that swig is aware of all that has been previous defined
//notice that it is not an include....
%import "android-cv.i"
%import "android-cv-typemaps.i"

//make sure to import the image_pool as it is 
//referenced by the Processor java generated
//class
%typemap(javaimports) Detector "
import com.opencv.jni.image_pool;	// import the image_pool interface for playing nice with
									// android-opencv

/** Characters Detector */
"

class Detector {
public:
	vector<Point> otherCharacter;
	vector<Point> selfCharacter;
	
	Detector() {};
	virtual ~Detector();
	
	bool findCharacter(int idx, image_pool* pool, int i_am, bool _flip, bool _debug);
	
	int getPtX(Point* p) { return p->x;}
	int getPtY(Point* p) { return p->y;}
	
	Point getPointFromVector(vector<Point>* v, int idx) { return ((v->size() > idx) ? (*v)[idx] : Point(-1,-1)); }
	
	double getSizeOfSelf();
	
	Point getSelfCenter();
	Point getOtherCenter();
	float getSelfAngle();
	int getWaveTimer();
};
