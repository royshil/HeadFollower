package edu.mit.media.fluid.royshil.headfollower;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public abstract class CameraFrameProcessor extends FrameProcessorBase {
    static final String TAG = "CameraFrameProcessor";

    protected Camera              mCamera;
    private SurfaceHolder       mHolder;
    
    public CameraFrameProcessor(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CameraFrameProcessor(Context context) {
        super(context);
        init(); 
    }

	@Override
	protected void init() {
		mHolder = getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
	}

    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        if(mCamera == null) initCamera(mHolder);
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            mFrameWidth = width;
            mFrameHeight = height;

            // selecting optimal camera preview size
            {
                double minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - height) < minDiff) {
                        mFrameWidth = size.width;
                        mFrameHeight = size.height;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }

            params.setPreviewSize(getFrameWidth(), getFrameHeight());
            mCamera.setParameters(params);
            mCamera.startPreview();
        }
    }
    
    public void initCamera(SurfaceHolder holder) {
        mCamera = Camera.open();
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
			public void onError(int error, Camera camera) {
				Log.i(TAG,"ErrorCallback,"+error);
			}
		});
        mCamera.setOneShotPreviewCallback(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				Log.i(TAG,"OneShotPreviewCallback");
			}
		});
        mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
            	Log.i(TAG, "got frame");
                synchronized (CameraFrameProcessor.this) {
                    mFrame = data;
                    CameraFrameProcessor.this.notify();
                }
            }
        });
        try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e);
		}
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    	super.surfaceDestroyed(holder);
        Log.i(TAG, "surfaceDestroyed");
        mThreadRun = false;
        if (mCamera != null) {
            synchronized (this) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
    }
    
}