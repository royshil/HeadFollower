package edu.mit.media.fluid.royshil.headfollower;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public abstract class FileFrameProcessor extends FrameProcessorBase implements Runnable {
	private static final String TAG = "FileFrameProcessor";
	protected String filename = "NO_FILENAME";
	
	private static int ERROR_FILE_DOESNT_EXIST = -1;
	private static int ERROR_FRAME_DATA_NULL = -2;
	private static int ERROR_VIDEOCAPTURE_NOT_OPEN = -3;

	public FileFrameProcessor(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public FileFrameProcessor(Context context) {
		super(context);
	}
	
	@Override
	protected void init() {
		Log.i(TAG,"init()");
		int[] ret = OpenFromFile(filename);
		if(ret[0] < 0) {
			if(ret[0] == ERROR_FILE_DOESNT_EXIST)
				throw new RuntimeException("Shit, file doesn't exist");
			if(ret[0] == ERROR_FRAME_DATA_NULL) 
				throw new RuntimeException("Shit, file was loaded incorrectly and the frame data is null!");
			if(ret[0] == ERROR_VIDEOCAPTURE_NOT_OPEN)
				throw new RuntimeException("Shit, file was loaded incorrectly and now it can't be opened");
		} 
		mFrameWidth = ret[0];
		mFrameHeight = ret[1];

		(new Thread(this)).start(); //start the FrameProcessorBase thread..

		//start the thread to peridocally load a frame from the file
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while(FileFrameProcessor.this.mThreadRun) { //TODO: when to stop??
					synchronized (FileFrameProcessor.this) {
						FileFrameProcessor.this.notify(); //this will let FrameProcessorBase fire "processFrame"
					}
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						Log.e(TAG,"interrupted",e);
					}
				}
			}
		});
		t.start(); 
	}
	
	public native int[] OpenFromFile(String filename);
}
