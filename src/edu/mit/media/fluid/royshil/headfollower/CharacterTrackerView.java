package edu.mit.media.fluid.royshil.headfollower;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;

public class CharacterTrackerView extends CameraFrameProcessor {
	
    private static final String TAG = "CharacterTrackerView";
	private boolean debug = false; 
	private boolean flip = false;
	private int i_am = 2;
	
	public enum State 
	{
	     CALIBRATING_NO_MARKERS_FOUND(0),
	     CALIBRATING_SHOW_MARKER(1),
	     WORKING(2),
	     CALIBRTING_NO_EXTRA_MARKER_FOUND(3),
	     NO_TRACKING(99);
	     
	     private static final Map<Integer,State> lookup = new HashMap<Integer,State>();
	     static {
	          for(State s : EnumSet.allOf(State.class)) lookup.put(s.getCode(), s);
	     }
	     private int code;
	     private State(int code) { this.code = code; }
	     public int getCode() { return code; }
	     public static State get(int code) { return lookup.get(code); }
	}	
	
	private State currentState = State.CALIBRATING_NO_MARKERS_FOUND;
	private Semaphore currentStateLock = new Semaphore(1);
	private IMarkerShower mMarkerShower;
		

	public CharacterTrackerView(Context context, AttributeSet attrs) {
		super(context, attrs);
//		filename = "/sdcard/DCIM/Camera/video-2011-09-11-20-35-26.avi";
//		filename = "/sdcard/video/frame";
		init(); 
		Log.i(TAG,"new instance");
	}

	public CharacterTrackerView(Context context) {
        super(context);
        init();
        Log.i(TAG,"new instance");
    }
	
	@Override
	protected void init() {
		super.init();
		ResetFrameIndex();
	}

    @Override 
    protected Bitmap processFrame(byte[] data) {
        Bitmap bmp = Bitmap.createBitmap(getFrameWidth(), getFrameHeight(), Bitmap.Config.ARGB_8888);

        try {
			currentStateLock.acquire();
		} catch (InterruptedException e1) {
			Log.e(TAG,"interrupted on currentStateLock.acquire()");
			return bmp;
		}

        if(currentState == State.NO_TRACKING) {
        	return bmp; //skip everything
        }
        
        int frameSize = getFrameWidth() * getFrameHeight();
        int[] rgba = new int[frameSize];

        if(currentState == State.CALIBRATING_NO_MARKERS_FOUND || currentState == State.CALIBRTING_NO_EXTRA_MARKER_FOUND || currentState == State.CALIBRATING_SHOW_MARKER) {
        	if(currentState == State.CALIBRATING_SHOW_MARKER || currentState == State.CALIBRTING_NO_EXTRA_MARKER_FOUND) {
        		Log.i(TAG,"Show marker");
        		mMarkerShower.showMarker(); 
        	} else if (currentState == State.CALIBRATING_NO_MARKERS_FOUND) {
        		Log.i(TAG,"Calibrating...");
        		mMarkerShower.removeMarker();
        	}
    		try {
				Thread.sleep(100); //sleep a little to let the marker show up / clear out
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
 
//			WriteFrame(getFrameWidth(), getFrameHeight(), data); 
        	int returnState[] = CalibrateSelf(getFrameWidth(), getFrameHeight(), data, rgba, i_am, flip, debug);
        	currentState = State.get(returnState[0]);
        	Log.i(TAG,"state: "+Arrays.toString(returnState));
        	Log.i(TAG,"new state = " + currentState);  
        	if(currentState==State.WORKING) {
        		Log.i(TAG,"Marker found");
            	mMarkerShower.removeMarker();
            	mMarkerShower.showCharacter();
        	}
        } else if(currentState == State.WORKING) {
//        	WriteFrame(getFrameWidth(), getFrameHeight(), data); 
			float[] state = FindFeatures(getFrameWidth(), getFrameHeight(), data, rgba, i_am, flip, debug);  
			Log.i(TAG,"State: " + Arrays.toString(state));   
			if(mStateHandler!=null) mStateHandler.onCharacterStateChanged(state); //let the handler know.. 
        } 
        
        currentStateLock.release();

//        WriteFrame(getFrameWidth(), getFrameHeight(), data);     
//        float[] state = ProcessFileFrame(mFrameWidth, mFrameHeight, rgba, i_am, flip, debug); 
           

        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
        return bmp;    
    }
    
    public boolean isDebug() {  
		return debug;
	}   
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	public boolean isFlip() {
		return flip;
	}
	public void setFlip(boolean flip) {
		this.flip = flip;
	}
	public int getI_am() {
		return i_am;
	}
	public void setI_am(int i_am) {
		this.i_am = i_am;
	}
	public IMarkerShower getmMarkerShower() {
		return mMarkerShower;
	}

	public void setmMarkerShower(IMarkerShower mMarkerShower) {
		this.mMarkerShower = mMarkerShower;
	}
	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}

	public native float[] FindFeatures(int width, int height, byte yuv[], int[] rgba, int i_am, boolean _flip, boolean _debug);
	public native float[] ProcessFileFrame(int width, int height, int[] bgra, int i_am, boolean _flip, boolean _debug); 
	public native void WriteFrame(int width, int height, byte yuv[]);
	public native int[] CalibrateSelf(int width, int height, byte yuv[], int[] rgba, int i_am, boolean _flip, boolean _debug);
	public native void ResetFrameIndex();
	public native void SetCalibrationState(int state);
	
    static {
    	Log.i(TAG,"System.loadLibrary(...);");
        System.loadLibrary("headfollower_native"); 
        Log.i(TAG,"loaded.");
    }

	public void disableTracking() {
		try {
			currentStateLock.acquire();
		} catch (InterruptedException e) {
			Log.e(TAG,"thread interrupted while skipping calibration",e);
		}
		currentState = State.NO_TRACKING;
		mMarkerShower.showCharacter();
		
		mCamera.stopPreview();
		mCamera.setPreviewCallback(null);
        mCamera.release();
		
		currentStateLock.release();
	}

	public void recalibrate() {
		try {
			currentStateLock.acquire();
		} catch (InterruptedException e) {
			Log.e(TAG,"thread interrupted while recalibrate",e);
		}
		SetCalibrationState(State.CALIBRATING_NO_MARKERS_FOUND.code);
		currentState = State.CALIBRATING_NO_MARKERS_FOUND;
		mMarkerShower.showCalibrationMessage();		
		currentStateLock.release();
	}


}
