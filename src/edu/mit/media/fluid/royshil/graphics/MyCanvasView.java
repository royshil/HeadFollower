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
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(bmp != null) {
			paint.setColor(Color.BLUE);
			clip = canvas.getClipBounds();
			float w = clip.width(), w2 = w/2.0f, h = clip.height(), h2 = h/2.0f;
			canvas.drawRect(0,0,w-1,h-1, paint);
			canvas.drawCircle(w2, h2, 10, paint);
			
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
			paint.setColor(Color.GREEN);
			canvas.drawRect(0,0,w-1,h-1, paint);
			
			bmpLock.lock();
			float left = scale*(w2 - (float)bmp.getWidth()/2.0f);
			float top = scale*(h2 - (float)bmp.getHeight()/2.0f);
			canvas.drawBitmap(bmp, left, top, paint);
			paint.setColor(Color.RED);
			canvas.drawRect(left, top, left + bmp.getWidth(), top + bmp.getHeight(), paint);
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
				boolean shouldTurn, MyCanvasView myCanvasView) {
					this.myAnim = myAnim;
					this.shouldTurn = shouldTurn;
					this.myCanvasView = myCanvasView;
					assets = myCanvasView.getContext().getAssets();
		}
		
		@Override
		public void run() {
			super.run();
			
			do {
				for (int i = myAnim.start; i <= myAnim.end; i++) {
					try {
						bmpLock.lock();
						bmp = BitmapFactory.decodeStream(
								assets.open(myAnim.filename + new DecimalFormat("0000").format(i) + ".png"));
						bmpLock.unlock();
						if(bmp == null) {
							AlertDialog a = new AlertDialog.Builder(getContext()).create();
							a.setMessage("Cannot load image");
							a.show();
							break;
						}
						myCanvasView.postInvalidate();
						
						sleep(100);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
			} while(myAnim.loop);
		}
	}
	
	private class RotatorScaler extends Thread {
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
					e.printStackTrace();
					break;
				}
			}
		}

		public void setDeg(float deg) {this.deg = deg;}
		public void setScl(float scl) {this.scl = scl;}
	}
	
	public void fireAnimation(final MyAnimations.MyAnim myAnim, final boolean shouldTurn) {
		if(a != null && a.isAlive()) { 
			a.interrupt(); 
			try {
				a.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
		a = new Animator(myAnim,shouldTurn,this);
		a.start(); //TODO: reuse the object
	}
	
	public void setRotationAndScale(float deg, float scl) {
		if(r != null && r.isAlive()) { 
			r.interrupt(); 
			try {
				r.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
		if(r == null) {
			r = new RotatorScaler(deg,scl,this);
			r.start();
		} else { 
			r.setDeg(deg); 
			r.setScl(scl); 
			r.run(); 
		}
		
	}
}
