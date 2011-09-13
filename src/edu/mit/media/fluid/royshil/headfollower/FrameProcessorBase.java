package edu.mit.media.fluid.royshil.headfollower;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class FrameProcessorBase extends SurfaceView implements Runnable, SurfaceHolder.Callback {
	private static final String TAG = "FrameProcessorBase";
	
	protected boolean 					mThreadRun;
	private IBitmapHolder 				mBitmapHolder;
	byte[]              				mFrame;
    protected int                 		mFrameWidth;
    protected int                 		mFrameHeight;
    protected ICharacterStateHandler	mStateHandler;

	public ICharacterStateHandler getmStateHandler() {
		return mStateHandler;
	}

	public void setmStateHandler(ICharacterStateHandler mStateHandler) {
		this.mStateHandler = mStateHandler;
	}

	public FrameProcessorBase(Context context) {
		super(context);
	}

	public FrameProcessorBase(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FrameProcessorBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	protected abstract Bitmap processFrame(byte[] data);
	protected abstract void init();

	public void run() {
		mThreadRun = true;
		Log.i(TAG, "Starting processing thread");
		while (mThreadRun) {
			Bitmap bmp = null;

			synchronized (this) {
				try {
					this.wait();
					bmp = processFrame(mFrame);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (bmp != null && mBitmapHolder != null) {
				// Canvas canvas = mDrawHolder.lockCanvas();
				// if (canvas != null) {
				// canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth())
				// / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
				// mDrawHolder.unlockCanvasAndPost(canvas);
				// }
				// bmp.recycle();
				synchronized (bmp) {
					mBitmapHolder.setBmp(bmp);
				}
			}
		}
	}

	public IBitmapHolder getBitmapHolder() {  
		return mBitmapHolder;
	}

	public void setBitmapHolder(IBitmapHolder mDrawHolder) {
		this.mBitmapHolder = mDrawHolder;
	}

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
    	Log.i(TAG, "surfaceChanged");
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    	Log.i(TAG,"surfaceDestroyed");
    	mThreadRun = false;
    }
}