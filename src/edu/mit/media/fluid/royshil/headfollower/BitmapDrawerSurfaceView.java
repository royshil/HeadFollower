package edu.mit.media.fluid.royshil.headfollower;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BitmapDrawerSurfaceView extends SurfaceView implements IBitmapHolder, SurfaceHolder.Callback {
	private static final String TAG = "BitmapDrawerSurfaceView";
	Bitmap bmp;
	private SurfaceHolder mHolder;
	private Paint paint;
	Semaphore bmp_mutex = new Semaphore(1);
	
	public BitmapDrawerSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mHolder = getHolder();
		mHolder.addCallback(this);
		paint = new Paint();
	}

	/* (non-Javadoc)
	 * @see edu.mit.media.fluid.royshil.headfollower.BitmapHolder#getBmp()
	 */
	@Override
	public Bitmap getBmp() {
		return bmp;    
	}

	/* (non-Javadoc)
	 * @see edu.mit.media.fluid.royshil.headfollower.BitmapHolder#setBmp(android.graphics.Bitmap)
	 */
	@Override
	public void setBmp(Bitmap bmp) {
		try {
			bmp_mutex.acquire();
			if(this.bmp != null) {
				this.bmp.recycle();
				this.bmp = null;
			}

			this.bmp = bmp;
			this.postInvalidate();
			bmp_mutex.release();
		} catch (InterruptedException e) {
			Log.e(TAG,"cdn't acquire bmp lock");
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG,"SurfaceChanged");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG,"SurfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG,"SurfaceDestroyed");
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Log.v(TAG, "OnDraw");
		if(bmp == null) return;
		super.onDraw(canvas);
		try {
			bmp_mutex.acquire();
			if(bmp.isRecycled()) return;
			canvas.drawBitmap(bmp, 0,0, paint);
			bmp_mutex.release();
		} catch (InterruptedException e) {
			Log.e(TAG,"cldn't acquire bmp lock");
		}
	}
}
