package edu.mit.media.fluid.royshil.headfollower;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.method.DateTimeKeyListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.opencv.camera.NativePreviewer;
import com.opencv.camera.NativeProcessor;
import com.opencv.camera.NativeProcessor.PoolCallback;
import com.opencv.jni.image_pool;
import com.opencv.opengl.GL2CameraViewer;

import edu.mit.media.fluid.charactertracker.jni.Detector;

public class HeadFollower extends Activity implements android.view.View.OnClickListener, OnSeekBarChangeListener {
	private static final String LOG_TAG = "HeadFollower";
	private static final int SETTINGS_DIALOG = 99;
	private NativePreviewer mPreview;
	private GL2CameraViewer glview;
	final Detector processor = new Detector();
	public boolean mDebug;
	public boolean mFlip;
	public boolean mOpenCV = false;
	public boolean mRedOrBlue = true;
	public boolean mTouchToColor = true;
	
	private boolean mLooking = true; //true == looking right, false == looking left
	
	private SurfaceView mCharPreview;
	private SurfaceHolder holder;
	private Bundle extras;
	private MediaPlayer mMediaPlayer;
	private boolean mIsVideoReadyToBePlayed;
	
	enum Animations { 
		TURN,
		THREE_QUARTERS_TO_PROFILE,
		START_WALK,
		END_WALK,
		WALK,
		SHAKE_HAND,
		WAVE, NATURAL
	}
	
//	private static HashMap<Animations, Integer> blue_animation_index = new HashMap<Animations, Integer>();
//	private static HashMap<Animations, Integer> red_animation_index = new HashMap<Animations, Integer>();
//	{
//		blue_animation_index.put(Animations.TURN,							R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.START_WALK, 					R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.END_WALK, 						R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.WALK, 							R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.WAVE, 							R.drawable.anim_look_l_to_r);
//		blue_animation_index.put(Animations.SHAKE_HAND, 					R.drawable.anim_look_l_to_r);
//
//		red_animation_index.put(Animations.TURN,							R.drawable.girlturn);
////		red_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
//		red_animation_index.put(Animations.START_WALK, 						R.drawable.girlstartwalk);
//		red_animation_index.put(Animations.END_WALK, 						R.drawable.girlendwalk);
//		red_animation_index.put(Animations.WALK, 							R.drawable.girlwalk);
//		red_animation_index.put(Animations.WAVE, 							R.drawable.girlwave);
//		red_animation_index.put(Animations.SHAKE_HAND, 						R.drawable.girlshake);
//	}
	private class MyAnim {
		String filename; 
		int start;
		int end;
		boolean loop;
		public MyAnim(String filename, int start, int end) {
			super();
			this.filename = filename;
			this.start = start;
			this.end = end;
			this.loop = false;
		}
		public MyAnim(String filename, int start, int end, boolean loop) {
			super();
			this.filename = filename;
			this.start = start;
			this.end = end;
			this.loop = loop;
		}
	}
	
	private static HashMap<Animations, MyAnim> blue_animation_index = new HashMap<Animations, MyAnim>();
	private static HashMap<Animations, MyAnim> red_animation_index = new HashMap<Animations, MyAnim>();
	{
		blue_animation_index.put(Animations.TURN,							new MyAnim("guy/guy_",329,340));
//		blue_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
		blue_animation_index.put(Animations.START_WALK, 					new MyAnim("guy/guy_",37,57));
		blue_animation_index.put(Animations.END_WALK, 						new MyAnim("guy/guy_",90,111));
		blue_animation_index.put(Animations.WALK, 							new MyAnim("guy/guy_",58,89,true));
		blue_animation_index.put(Animations.WAVE, 							new MyAnim("guy/guy_",113,190));
		blue_animation_index.put(Animations.SHAKE_HAND, 					new MyAnim("guy/guy_",266,325));
		blue_animation_index.put(Animations.NATURAL, 						new MyAnim("guynatural3q.png",-1,-1));

		red_animation_index.put(Animations.TURN,							new MyAnim("girl/girl_",301,322));
//		red_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
		red_animation_index.put(Animations.START_WALK, 						new MyAnim("girl/girl_",45,63));
		red_animation_index.put(Animations.END_WALK, 						new MyAnim("girl/girl_",97,112));
		red_animation_index.put(Animations.WALK, 							new MyAnim("girl/girl_",64,96,true));
		red_animation_index.put(Animations.WAVE, 							new MyAnim("girl/girl_",242,300));
		red_animation_index.put(Animations.SHAKE_HAND, 						new MyAnim("girl/girl_",160,235));
		red_animation_index.put(Animations.NATURAL, 						new MyAnim("girlnatural3q.png",-1,-1));
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true; 
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.showOpenCV:
			RelativeLayout rl = (RelativeLayout)findViewById(R.id.mainFrameLayout);
			if(mOpenCV) {
				rl.bringChildToFront(findViewById(R.id.charcterView));
				mOpenCV = false;
			} else {
				rl.bringChildToFront(glview); 
				mOpenCV = true;				
			}
			rl.invalidate();
			return true;
//		case R.id.showsensors_item:
//			Intent myIntent = new Intent();
//			myIntent.setClassName("edu.mit.media.fluid.royshil.headfollower", "edu.mit.media.fluid.royshil.headfollower.Sensors");
//			startActivity(myIntent);
//			return true;
		case R.id.quit_item:
			finish();
			return true;
		case R.id.toggleFlip_item:
			mFlip = !mFlip;
			return true;
		case R.id.showDebugParts:
			mDebug = !mDebug;
			return true;
		case R.id.openSettings_itm:
			showDialog(SETTINGS_DIALOG);
			return true;
		case R.id.lookLtoR_item:
			showChooseAnimDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id==SETTINGS_DIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("ON/OFF Settings");
			
			final String touch_to_color = "Touch to color";
			final String red_markers = "Red markers";
			final String[] items = new String[] {touch_to_color, red_markers};
			boolean[] checked = new boolean[] {mTouchToColor,mRedOrBlue};
			
			builder.setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					if(items[which].equals(touch_to_color)) mTouchToColor = isChecked;
					if(items[which].equals(red_markers)) { mRedOrBlue = isChecked; setRedBlue(); }
				}
			});
			
			return builder.create();
		}
		return null;
	}
	
	private void flipRedBlue() {
		mRedOrBlue = !mRedOrBlue;
		setRedBlue();
	}

	private void setRedBlue() {
		ImageView imageView = (ImageView)findViewById(R.id.bl_circle);
		ImageView imageView3 = (ImageView)findViewById(R.id.tr_circle);
		if(!mRedOrBlue) {
			imageView.setBackgroundResource(R.drawable.blue_markercircle);
			imageView3.setBackgroundResource(R.drawable.blue_markercircle);
		} else {
			imageView.setBackgroundResource(R.drawable.red_markercircle);
			imageView3.setBackgroundResource(R.drawable.red_markercircle);
		}
		imageView.postInvalidate();
		imageView3.postInvalidate();
		
		mLooking = true;//looking right.
		
		fireAnimation(getAnimationsIndex().get(Animations.NATURAL),false);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		
//		TransformableImageView headImgView = (TransformableImageView)findViewById(R.id.head_img);
//		headImgView.setOnClickListener(this);
		
		
		initOpenCVViews(); 
		
		((SeekBar)findViewById(R.id.hueSeek)).setOnSeekBarChangeListener(this);
		
		float hsv[] = new float[3]; 
		android.graphics.Color.RGBToHSV(255, 0, 0, hsv);
		((SeekBar)findViewById(R.id.hueSeek)).setProgress((int)hsv[0]);
		
		findViewById(R.id.tr_circle).setOnClickListener(this);
		findViewById(R.id.bl_circle).setOnClickListener(this);
		
		WebView wb = (WebView) findViewById(R.id.webview);
//		wb.setOnClickListener(this);
		wb.setBackgroundColor(0);
//		wb.loadDataWithBaseURL("fake://dagnabbit",
//				"<div style=\"text-align: center;\"><IMG id=\"myanim\" SRC=\"file:///android_asset/guynatural3q.png\" style=\"height: 100%\" /></div>", 
//				"text/html",  
//				"UTF-8", 
//				"fake://lala");
//		fireAnimation(getAnimationsIndex().get(Animations.NATURAL),false);
//		wb.loadUrl("file:///android_asset/animate.html?anim_file=girlshake0&first=160&last=235");

		WebSettings webSettings = wb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        
        fireAnimation(getAnimationsIndex().get(Animations.NATURAL), false);
        
        
//        mCharPreview = (SurfaceView) findViewById(R.id.mysurfaceview);
//        holder = mCharPreview.getHolder();
//        holder.addCallback(this);
////        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        extras = getIntent().getExtras();
//
//        mMediaPlayer = new MediaPlayer();
//        mMediaPlayer.setDisplay(holder);
//        mMediaPlayer.setOnCompletionListener(this);
//        mMediaPlayer.setOnPreparedListener(this);
//                
//        mMediaPlayer.setOnBufferingUpdateListener(this);
//        mMediaPlayer.setOnVideoSizeChangedListener(this);
//        mMediaPlayer.setAudioStreamType(AudioManager.);

	}     
          
	private void initOpenCVViews() {
		// Create our Preview view and set it as the content of our activity.
		mPreview = new NativePreviewer(getApplication(), 640, 480);

		// RotateAnimation rotateAnimation = new RotateAnimation(0.0f, 90.0f,
		// getWindowManager().getDefaultDisplay().getHeight() /2.0f,
		// getWindowManager().getDefaultDisplay().getWidth() / 2.0f);
		// rotateAnimation.setFillAfter(true);
		// rotateAnimation.setDuration(1000);
		// mPreview.setAnimation(rotateAnimation);

		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		// params.height = getWindowManager().getDefaultDisplay().getHeight();
		// params.width = (int) (params.height * 4.0 / 2.88);

		LinearLayout vidlay = new LinearLayout(getApplication());

		vidlay.setGravity(Gravity.CENTER);
		vidlay.addView(mPreview, params);

		// make the glview overlay ontop of video preview
		mPreview.setZOrderMediaOverlay(false);

		// RelativeLayout relativeLayout = (RelativeLayout)
		// findViewById(R.id.mainFrameLayout);
		// relativeLayout.addView(vidlay);

		glview = new GL2CameraViewer(getApplication(), false, 0, 0);
		glview.setZOrderMediaOverlay(true);
		glview.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));

		// relativeLayout.bringChildToFront(findViewById(R.id.crossandtext));

		LinkedList<PoolCallback> defaultcallbackstack = new LinkedList<PoolCallback>();
		defaultcallbackstack.addFirst(glview.getDrawCallback());
		defaultcallbackstack.addFirst(new CharacterProcessor());
		mPreview.addCallbackStack(defaultcallbackstack);
		
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.mainFrameLayout);
		rl.addView(vidlay);
		rl.addView(glview);
		rl.bringChildToFront(findViewById(R.id.charcterView));
	}   

	@Override
	protected void onPause() {
		super.onPause();
		mPreview.onPause();
		glview.onPause();
	} 
  
	@Override
	protected void onResume() {
		super.onResume();
		glview.onResume();
		mPreview.onResume();
	}	      
  
	class CharacterProcessor implements NativeProcessor.PoolCallback {
		@Override
		public void process(int idx, image_pool pool, long timestamp, NativeProcessor nativeProcessor) {
			if(processor.findCharacter(idx, pool, (mRedOrBlue)?1:2, mFlip, mDebug)) {
				//found friend
				
				//adjust size
				WebView wb = (WebView) findViewById(R.id.webview);
				wb.loadUrl("javascript:document.getElementById('im').style.webkitTransform='scaleX(" + (float) processor.getSizeOfSelf() + ") scaleY(" + (float) processor.getSizeOfSelf() + ")';");
				
//				final TransformableImageView transformableImageView = (TransformableImageView)findViewById(R.id.head_img);
//				transformableImageView.post(new Runnable() {
//					@Override
//					public void run() {
//						transformableImageView.scale = (float) processor.getSizeOfSelf();
//						transformableImageView.invalidate();
//					}   
//				});
				
				//look in the right direction
				if ( processor.getPtX(processor.getOtherCenter()) > processor.getPtX(processor.getSelfCenter())) {
					if(!mLooking) toggleLooking();
				} else {
					if(mLooking) toggleLooking();
				}
				
//				TransformableImageView headImg = (TransformableImageView) findViewById(R.id.head_img);
//				AnimationDrawable anim = (AnimationDrawable) headImg.getDrawable();
//				anim.start();
//				boolean doneTurning = !anim.isRunning();
				boolean doneTurning = true;
				
				boolean waveTimerDue = processor.getWaveTimer() > 15;
				
				if(doneTurning && waveTimerDue) {
//					fireAnimation(getAnimationsIndex().get(Animations.WAVE),false);
				}
			}
		}		
	} 
	
	private void toggleLooking() {
		fireAnimation(getAnimationsIndex().get(Animations.TURN),true);
	}

	long anim_start_ts = -1;
	
	private void fireAnimation(final MyAnim myAnim, final boolean shouldTurn) {
		findViewById(R.id.webview).post(new Runnable() {
			@Override
			public void run() {
				long now = (new Date()).getTime();
				if((now - anim_start_ts) < 2000) return;
				
				WebView wb = (WebView) findViewById(R.id.webview);
				wb.loadUrl("file:///android_asset/animate.html?anim_file="+myAnim.filename+"&first="+myAnim.start+"&last="+myAnim.end+"&flip="+(HeadFollower.this.mLooking ? "1" : "0") + "&loop="+myAnim.loop);
				if(shouldTurn) HeadFollower.this.mLooking = !HeadFollower.this.mLooking;
				wb.invalidate();
				
				anim_start_ts = now;
			}
		});
	}

     
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser && seekBar.getTag().equals("hueSeek")) { 
			int rgbi = Color.HSVToColor(new float[] {(float)progress,1.0f,1.0f});
			((TextView)findViewById(R.id.debugTxt)).setText("rgb: " + (rgbi & 0x000000ff) + "," + (rgbi >> 8 & 0x000000ff) + "," + (rgbi >> 16 & 0x000000ff));
		}
	}
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.tr_circle)
			showChooseAnimDialog();
		if(v.getId() == R.id.bl_circle) 
			fireAnimation(getAnimationsIndex().get(Animations.END_WALK), false);
	}

	private void showChooseAnimDialog() {
		final CharSequence[] items = {"Flip Red-Blue", "Shake Hands", "Turn Right-Left", "Wave hand", "Start walk", "Walk", "End walk", "Natural pose"};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a color");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
		        switch (item) {
				case 0:
		    		flipRedBlue();
					break;
				case 1:
					fireAnimation(getAnimationsIndex().get(Animations.SHAKE_HAND),false);
					break;
				case 2:
					toggleLooking();
					break;   
				case 3:  
					fireAnimation(getAnimationsIndex().get(Animations.WAVE),false);
					break;
				case 4:
					fireAnimation(getAnimationsIndex().get(Animations.START_WALK),false);
					break;
				case 5:
					fireAnimation(getAnimationsIndex().get(Animations.WALK),false);
					break;
				case 6:
					fireAnimation(getAnimationsIndex().get(Animations.END_WALK),false);
					break;
				case 7:
					fireAnimation(getAnimationsIndex().get(Animations.NATURAL),false);
					break;
				default:
					break;
				}
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if(seekBar.getTag().equals("hueSeek")) {
			GradientDrawable sd = (GradientDrawable)getResources().getDrawable((mRedOrBlue) ? R.drawable.red_markercircle : R.drawable.blue_markercircle);
	        sd.setColor(Color.HSVToColor(new float[] {(float)seekBar.getProgress(),1.0f,1.0f}));
	         
			findViewById(R.id.bl_circle).postInvalidate();
			findViewById(R.id.tr_circle).postInvalidate();
		}
	}

	private HashMap<Animations, MyAnim> getAnimationsIndex() {
		HashMap<Animations, MyAnim> index = (!mRedOrBlue) ? red_animation_index : blue_animation_index;
		return index;
	}

//	@Override
//	public void surfaceChanged(SurfaceHolder holder, int format, int width,
//			int height) {
//		
//	}
//
//	@Override
//	public void surfaceCreated(SurfaceHolder holder) {
//        try {
//            AssetFileDescriptor openFd = getAssets().openFd("girlturn.gif");
//			mMediaPlayer.setDataSource(openFd.getFileDescriptor());
//	        mMediaPlayer.prepare();
//		} catch (Exception e) {
//			e.printStackTrace();
//			(new AlertDialog.Builder(this)).setTitle("Exception").setMessage(e.getClass().getName() + ":" + e.getLocalizedMessage()).create().show();
//		}
//	}
//
//	@Override
//	public void surfaceDestroyed(SurfaceHolder holder) {
//		
//	}
//
//	@Override
//	public void onCompletion(MediaPlayer mp) {
//		
//	}
//
//    private void startVideoPlayback() {
//        Log.v(LOG_TAG, "startVideoPlayback");
//        holder.setFixedSize(200, 300);
//        mMediaPlayer.start();
//    }
//    
//	public void onPrepared(MediaPlayer mediaplayer) {
//        Log.d(LOG_TAG, "onPrepared called");
//        mIsVideoReadyToBePlayed = true;
//        if (mIsVideoReadyToBePlayed/* && mIsVideoSizeKnown*/) {
//            startVideoPlayback();
//        }
//    }
}