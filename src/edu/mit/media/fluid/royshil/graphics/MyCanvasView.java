package edu.mit.media.fluid.royshil.graphics;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MyCanvasView extends View {

	private float rotation;
	private float scale;
	private Matrix matrix;
	private Bitmap bmp;
	private Paint paint;
	private Animator a;
	private RotatorScaler r;
	public Lock matrixLock;	//may use Object and notify()-wait()..
	private Lock bmpLock;
	private Rect clip;
	private boolean mLookingRight;

	public MyCanvasView(Context context, AttributeSet attrs) {
		super(context, attrs);
		matrix = new Matrix(); matrix.setScale(1.0f, 1.0f);
		paint = new Paint(); 
		paint.setColor(Color.RED); 
		paint.setStyle(Style.STROKE);
		matrixLock = new ReentrantLock();
		bmpLock = new ReentrantLock();
		rotation = 0.0f;  
		scale = 1.0f; 
		mLookingRight = false;
	} 

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(bmp != null) {
//			paint.setColor(Color.BLUE);
			clip = canvas.getClipBounds();
			float w = clip.width(), w2 = w/2.0f, h = clip.height(), h2 = h/2.0f;
//			canvas.drawRect(0,0,w-1,h-1, paint);
//			canvas.drawCircle(w2, h2, 10, paint);
			
			matrixLock.lock();
			Matrix m = new Matrix();
			m.reset();
			m.postTranslate(0, 80);
			m.postConcat(matrix);			
			canvas.setMatrix(m);
			matrixLock.unlock();
			
			clip = canvas.getClipBounds();
			w = clip.width();
			w2 = w/2.0f;
			h = clip.height();
			h2 = h/2.0f;
//			paint.setColor(Color.GREEN);
//			canvas.drawRect(0,0,w-1,h-1, paint);
			
			bmpLock.lock();
			float left = scale*(w2 - (float)bmp.getWidth()/2.0f);
			float top = scale*(h2 - (float)bmp.getHeight()/2.0f);

			Bitmap _bmp = bmp;
			//check if rotated, and set the matrix accordingly
			if(!mLookingRight) {
				Matrix _m = new Matrix();
				_m.preScale(-1.0f, 1.0f);
				_bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), _m, false);
			}

			canvas.drawBitmap(_bmp, left, top, paint);
//			paint.setColor(Color.RED);
//			canvas.drawRect(left, top, left + bmp.getWidth(), top + bmp.getHeight(), paint);
			bmpLock.unlock(); 
		}
	}	
	
	private class Animator extends Thread {

		private final boolean shouldTurn;
		private final MyCanvasView myCanvasView;
		private final MyAnimations.MyAnim myAnim;
		private AssetManager assets;

		public Animator(
				MyAnimations.MyAnim myAnim,
				boolean shouldTurn, 
				MyCanvasView myCanvasView) 
		{
			this.myAnim = myAnim;
			this.shouldTurn = shouldTurn;
			this.myCanvasView = myCanvasView;
			assets = myCanvasView.getContext().getAssets();
		}
		
		@Override
		public void run() {
			super.run();
			
			try {
				do {
					if(myAnim.start == -1 || myAnim.end == -1) {
						//this is a single image, not an animation
						tryLoadBitmap(myAnim.filename);
					} else {
						for (int i = myAnim.start; i <= myAnim.end; i++) {
							boolean bitmapLoaded;
								bitmapLoaded = tryLoadBitmap(myAnim.filename + new DecimalFormat("0000").format(i) + ".png");
							if(!bitmapLoaded) { break; }
						}
					}
				} while(myAnim.loop);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if(shouldTurn) {
					myCanvasView.setmLookingRight(!myCanvasView.ismLookingRight());
				}
			}
		}

		private boolean tryLoadBitmap(String bmpFilename) throws InterruptedException {
			try {
				bmpLock.lock();
				bmp = BitmapFactory.decodeStream(assets.open(bmpFilename));
				bmpLock.unlock();
				if(bmp == null) {
					AlertDialog a = new AlertDialog.Builder(getContext()).create();
					a.setMessage("Cannot load image");
					a.show();
					return false;
				}
				myCanvasView.postInvalidate();
				
				sleep(25);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
	}
	
	private class RotatorScaler extends Thread {
		private static final String TAG = "RotatorScaler";
		private float deg;
		private float scl;
		private MyCanvasView myCanvasView;

		public RotatorScaler(float deg, float scl, MyCanvasView myCanvasView) {
			this.deg = deg;
			this.scl = scl;
			this.myCanvasView = myCanvasView;
		}
		
		@Override
		public void run() {
			super.run();

//			float h2 = (float)myCanvasView.getHeight()/2.0f;
//			float w2 = (float)myCanvasView.getWidth()/2.0f;
//			float h2 = clip.height()/2.0f;
//			float w2 = clip.width()/2.0f;

			float rotstep = (deg-rotation)/20.0f;
			float scalestep = (scl-scale)/20.0f;
			for (int i = 0; i < 20; i++) {
				matrixLock.lock();
				matrix.reset();
				
				//linear interpolation
//				rotation += rotstep;
//				matrix.setRotate(rotation,w2,h2);
				
				scale += scalestep;
				matrix.postScale(scale, scale);
				
				matrixLock.unlock();
				
				myCanvasView.postInvalidate();
				
				try {
					sleep(30);
				} catch (InterruptedException e) {
					Log.i(TAG,"interrupted",e);  
					break;
				}
			}
		}

		public void setDeg(float deg) {this.deg = deg;}
		public void setScl(float scl) {this.scl = scl;}

		public float getDeg() {
			return deg;
		}

		public float getScl() {
			return scl;
		}
	}
	
	public void fireAnimation(final MyAnimations.MyAnim myAnim, final boolean shouldTurn) {
		if(a != null && a.isAlive()) { 
			a.interrupt(); 
			a = null;
//			try {
//				a.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} 
		}
		a = new Animator(myAnim,shouldTurn,this);
		a.start(); //TODO: reuse the object
	}
	
	public void setRotationAndScale(float deg, float scl) {
		if(r != null) { 
			if(r.getDeg() != deg || r.getScl() != scl) {
				if(r.isAlive()) {
					r.interrupt(); 
//					try {
//						r.join();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					} 
				}
				r.setDeg(deg); 
				r.setScl(Math.min(scl,2.0f)); 
				r.run();
			}
		} else {
			r = new RotatorScaler(deg,scl,this);
			r.start();
		}
	}
	
	public boolean ismLookingRight() {
		return mLookingRight;
	}

	public void setmLookingRight(boolean mLookingRight) {
		this.mLookingRight = mLookingRight;
	}

}
