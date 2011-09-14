package edu.mit.media.fluid.royshil.headfollower;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import edu.mit.media.fluid.royshil.graphics.InteractionBar;
import edu.mit.media.fluid.royshil.graphics.MyAnimations;
import edu.mit.media.fluid.royshil.graphics.MyCanvasView;

public class HeadFollower extends Activity implements android.view.View.OnClickListener, OnSeekBarChangeListener, ICharacterStateHandler, IMarkerShower {
	private static final String TAG = "HeadFollower";  
	private static final int SETTINGS_DIALOG = 99;

	public boolean mDebug;
	public boolean mFlip;
	public boolean mOpenCV = false;
	public boolean mRedOrBlue = true;
	public boolean mTouchToColor = true;
	
	private boolean mLooking = true; //true == looking right, false == looking left
	
//	private SurfaceView mCharPreview; 
//	private SurfaceHolder holder;
//	private Bundle extras;
//	private MediaPlayer mMediaPlayer;
//	private boolean mIsVideoReadyToBePlayed;    

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
			toggleOpenCV();
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

	private void toggleOpenCV() {
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.charactercenterview);
		if(mOpenCV) { //remove OpenCV view
			View calibration_or_character = findViewById(
					cview.getCurrentState() == CharacterTrackerView.State.CALIBRATING_NO_MARKERS_FOUND ? R.id.calibration_text_background : R.id.mycanvas
					); //if calibrating, bring the calibration text forward, else bring character
			rl.bringChildToFront(calibration_or_character);
			mOpenCV = false;
		} else {	//install OpenCV view
			rl.bringChildToFront(findViewById(R.id.drawtracker)); 
			mOpenCV = true;				
		}
		rl.invalidate();
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
		cview.setI_am(mRedOrBlue ? 2 : 1); //RED = 2, BLUE = 1
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
		 
		mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.NATURAL, MyAnimations.Character.BLUE), false);
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
		
		
//		initOpenCVViews(); 
		
//		((SeekBar)findViewById(R.id.hueSeek)).setOnSeekBarChangeListener(this);
//		
//		float hsv[] = new float[3]; 
//		android.graphics.Color.RGBToHSV(255, 0, 0, hsv);
//		((SeekBar)findViewById(R.id.hueSeek)).setProgress((int)hsv[0]);
		
		findViewById(R.id.tr_circle).setOnClickListener(this);
		findViewById(R.id.bl_circle).setOnClickListener(this);
		
//		WebView wb = (WebView) findViewById(R.id.webview);
////		wb.setOnClickListener(this);
//		wb.setBackgroundColor(0);
////		wb.loadDataWithBaseURL("fake://dagnabbit",
////				"<div style=\"text-align: center;\"><IMG id=\"myanim\" SRC=\"file:///android_asset/guynatural3q.png\" style=\"height: 100%\" /></div>", 
////				"text/html",  
////				"UTF-8", 
////				"fake://lala");
////		fireAnimation(getAnimationsIndex().get(Animations.NATURAL),false);
////		wb.loadUrl("file:///android_asset/animate.html?anim_file=girlshake0&first=160&last=235");
//
//		WebSettings webSettings = wb.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setSupportZoom(false);
        
        mcv = (MyCanvasView) findViewById(R.id.mycanvas);
        mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.NATURAL, MyAnimations.Character.BLUE), false);
        
        cview = (CharacterTrackerView) findViewById(R.id.charactertracker);
        BitmapDrawerSurfaceView dtv = (BitmapDrawerSurfaceView)findViewById(R.id.drawtracker);
		cview.setBitmapHolder(dtv);
		cview.setmStateHandler(this);
		cview.setmMarkerShower(this);

        RelativeLayout rl = (RelativeLayout)findViewById(R.id.mainFrameLayout);
        rl.bringChildToFront(findViewById(R.id.charcterView));
        
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
          
//	private void initOpenCVViews() {
//		// Create our Preview view and set it as the content of our activity.
//		mPreview = new NativePreviewer(getApplication(), 640, 480);
//
//		// RotateAnimation rotateAnimation = new RotateAnimation(0.0f, 90.0f,
//		// getWindowManager().getDefaultDisplay().getHeight() /2.0f,
//		// getWindowManager().getDefaultDisplay().getWidth() / 2.0f);
//		// rotateAnimation.setFillAfter(true);
//		// rotateAnimation.setDuration(1000);
//		// mPreview.setAnimation(rotateAnimation);
//
//		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
//		// params.height = getWindowManager().getDefaultDisplay().getHeight();
//		// params.width = (int) (params.height * 4.0 / 2.88);
//
//		LinearLayout vidlay = new LinearLayout(getApplication());
//
//		vidlay.setGravity(Gravity.CENTER);
//		vidlay.addView(mPreview, params);
//
//		// make the glview overlay ontop of video preview
//		mPreview.setZOrderMediaOverlay(false);
//
//		// RelativeLayout relativeLayout = (RelativeLayout)
//		// findViewById(R.id.mainFrameLayout);
//		// relativeLayout.addView(vidlay);
//
//		glview = new GL2CameraViewer(getApplication(), false, 0, 0);
//		glview.setZOrderMediaOverlay(true);
//		glview.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
//
//		// relativeLayout.bringChildToFront(findViewById(R.id.crossandtext));
//
//		LinkedList<PoolCallback> defaultcallbackstack = new LinkedList<PoolCallback>();
//		defaultcallbackstack.addFirst(glview.getDrawCallback());
////		defaultcallbackstack.addFirst(new CharacterProcessor());
//		mPreview.addCallbackStack(defaultcallbackstack);
//		
//		RelativeLayout rl = (RelativeLayout)findViewById(R.id.mainFrameLayout);
////		rl.addView(vidlay);
////		rl.addView(glview);
//		rl.bringChildToFront(findViewById(R.id.charcterView));
//	}   

//	@Override
//	protected void onPause() {
//		super.onPause();
//		mPreview.onPause();
//		glview.onPause();
//	} 
//  
//	@Override
//	protected void onResume() {
//		super.onResume();
//		glview.onResume();
//		mPreview.onResume();
//	}	      
//  
////	class CharacterProcessor implements NativeProcessor.PoolCallback {
//		@Override
//		public void process(int idx, image_pool pool, long timestamp, NativeProcessor nativeProcessor) {
//			if(processor.findCharacter(idx, pool, (mRedOrBlue)?1:2, mFlip, mDebug)) {
//				//found friend
//				
//				//adjust size
////				WebView wb = (WebView) findViewById(R.id.webview);
////				wb.loadUrl("javascript:document.getElementById('im').style.webkitTransform='scaleX(" + (float) processor.getSizeOfSelf() + ") scaleY(" + (float) processor.getSizeOfSelf() + ")';");
//				
////				final TransformableImageView transformableImageView = (TransformableImageView)findViewById(R.id.head_img);
////				transformableImageView.post(new Runnable() {
////					@Override
////					public void run() {
////						transformableImageView.scale = (float) processor.getSizeOfSelf();
////						transformableImageView.invalidate();
////					}   
////				});
//				
//				//look in the right direction
//				if ( processor.getPtX(processor.getOtherCenter()) > processor.getPtX(processor.getSelfCenter())) {
//					if(!mLooking) toggleLooking();
//				} else {
//					if(mLooking) toggleLooking();
//				}  
//				
////				TransformableImageView headImg = (TransformableImageView) findViewById(R.id.head_img);
////				AnimationDrawable anim = (AnimationDrawable) headImg.getDrawable();
////				anim.start();
////				boolean doneTurning = !anim.isRunning();
//				boolean doneTurning = true;
//				
//				boolean waveTimerDue = processor.getWaveTimer() > 15;
//				
//				if(doneTurning && waveTimerDue) {
////					fireAnimation(getAnimationsIndex().get(Animations.WAVE),false);
//					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.WAVE, MyAnimations.Character.BLUE), false);
//				}
//			}
//		}		
//	} 
	
	private void toggleLooking() {
		mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.TURN, MyAnimations.Character.BLUE), false);
	}

	private MyCanvasView mcv;
	private CharacterTrackerView cview;
	protected int farInteractionCounter = 0;
	protected int closeInteractionCounter = 0;
     
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//		if(fromUser && seekBar.getTag().equals("hueSeek")) { 
//			int rgbi = Color.HSVToColor(new float[] {(float)progress,1.0f,1.0f});
////			((TextView)findViewById(R.id.debugTxt)).setText("rgb: " + (rgbi & 0x000000ff) + "," + (rgbi >> 8 & 0x000000ff) + "," + (rgbi >> 16 & 0x000000ff));
//		}
	}
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.tr_circle)
			showChooseAnimDialog();
		if(v.getId() == R.id.bl_circle) 
			mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.END_WALK, MyAnimations.Character.BLUE), false);
//			fireAnimation(getAnimationsIndex().get(Animations.END_WALK), false);
	}

	private void showChooseAnimDialog() {
		final CharSequence[] items = {"Flip Red-Blue","Toggle OpenCV","Disable Tracking", "Recalibrate", "Shake Hands", "Turn Right-Left", "Wave hand", "Start walk", "Walk", "End walk", "Natural pose"};

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
					toggleOpenCV();
					break;
				case 2:
					cview.disableTracking();
					break;
				case 3:
					cview.recalibrate();
					break;
				case 4:
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.SHAKE_HAND, MyAnimations.Character.BLUE), false);
					break;
				case 5:
					toggleLooking();
					break;   
				case 6:  
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.WAVE, MyAnimations.Character.BLUE), false);
					break;
				case 7:
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.START_WALK, MyAnimations.Character.BLUE), false);
					break;
				case 8:
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.WALK, MyAnimations.Character.BLUE), false);					
					break;
				case 9:
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.END_WALK, MyAnimations.Character.BLUE), false);
					break;
				case 10:
					mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.NATURAL, MyAnimations.Character.BLUE), false);
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

	/* (non-Javadoc)
	 * @see edu.mit.media.fluid.royshil.headfollower.ICharacterStateHandler#onCharacterStateChanged(float[])
	 */
	@Override
	public void onCharacterStateChanged(final float[] state) {
		//11-floats:
		//state[0] = self point1 X
		//state[1] = self point1 Y
		//state[2] = self point2 X
		//state[3] = self point2 Y
		//state[4] = other point1 X
		//state[5] = other point1 Y
		//state[6] = other point2 X
		//state[7] = other point2 Y
		//state[8] = wave timer
		//state[9] = is tracking
		//state[10] = self size
		
		mcv.post(new Runnable() {
			@Override
			public void run() {
				if(state[4] > 0.0f && state[5] > 0.0f && state[6] > 0.0f  && state[6] > 0.0f) {
					//other character recognized
					Log.i(TAG,"other character in sight");
					
					mcv.setRotationAndScale(0.0f, state[10]);
					
					float midx_self = (state[0]+state[2])/2;
					float midx_other = (state[4]+state[6])/2;
					if(midx_self > midx_other) {
						if(!mLooking) toggleLooking();
					} else {
						if(mLooking) toggleLooking();
					}  
					
					float distance = Math.abs(midx_other-midx_self);
					Log.i(TAG,"distance: "+distance);
					if(distance < 100) {
						farInteractionCounter = Math.min(farInteractionCounter + 1,15);
						if(distance < 25) {
							closeInteractionCounter = Math.min(closeInteractionCounter + 1,15);  
						} else {
							closeInteractionCounter = Math.max(closeInteractionCounter - 1, 0);
						}
					} else {
						farInteractionCounter = Math.max(farInteractionCounter - 1, 0);
						closeInteractionCounter = Math.max(closeInteractionCounter - 1, 0);
					}
					
					if(farInteractionCounter == 15 && closeInteractionCounter < 5) {
						//fire long-distance animation: hand wave
						mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.WAVE, MyAnimations.Character.BLUE), false);
					} 
					if(farInteractionCounter == 15 && closeInteractionCounter == 15) {
						mcv.fireAnimation(MyAnimations.getAnimation(MyAnimations.Animations.SHAKE_HAND, MyAnimations.Character.BLUE), false);
					}
				} else {
					farInteractionCounter = Math.max(farInteractionCounter - 1, 0);
					closeInteractionCounter = Math.max(closeInteractionCounter - 1, 0);					
				}
				InteractionBar far = (InteractionBar) findViewById(R.id.interactionbar1);
				far.setValue(farInteractionCounter);
				InteractionBar close = (InteractionBar) findViewById(R.id.interactionbar2);
				close.setValue(closeInteractionCounter);
			}
		});
	}

	@Override
	public void showMarker() {
		final View extra = findViewById(R.id.extra_marker);
		extra.post(new Runnable() {			
			@Override
			public void run() {
				extra.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void removeMarker() {
		final View extra = findViewById(R.id.extra_marker);
		extra.post(new Runnable() {			
			@Override
			public void run() {
				extra.setVisibility(View.INVISIBLE);
			}
		});
	}

	@Override
	public void showCharacter() {
		final View mycanvas = findViewById(R.id.mycanvas);
		mycanvas.post(new Runnable() {
			@Override
			public void run() {
				mycanvas.setVisibility(View.VISIBLE);
				findViewById(R.id.calibration_text_background).setVisibility(View.INVISIBLE);
				RelativeLayout rl = (RelativeLayout)findViewById(R.id.charactercenterview);
				rl.bringChildToFront(mycanvas);
			}
		});
	}

	@Override
	public void showCalibrationMessage() {
		final RelativeLayout rl = (RelativeLayout)findViewById(R.id.charactercenterview);
		rl.post(new Runnable() {
			@Override
			public void run() {
				View calib_text_view = findViewById(R.id.calibration_text_background);
				calib_text_view.setVisibility(View.VISIBLE);
				rl.bringChildToFront(calib_text_view);
			}
		});
	}

	@Override
	public void onCalibrationStateChanged(int[] state) {
		
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