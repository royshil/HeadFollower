package edu.mit.media.fluid.royshil.headfollower;

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
		if(this.bmp != null) {
			this.bmp.recycle();
			this.bmp = null;
		}

		this.bmp = bmp;
		this.postInvalidate();
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
		synchronized (bmp) {
			canvas.drawBitmap(bmp, canvas.getWidth()/2.0f, canvas.getHeight()/2.0f, paint);
		}
	}
}
