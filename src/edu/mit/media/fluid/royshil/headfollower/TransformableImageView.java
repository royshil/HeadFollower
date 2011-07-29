package edu.mit.media.fluid.royshil.headfollower;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TransformableImageView extends ImageView {
    public TransformableImageView(Context context)
    {
        super(context);
    }

    public TransformableImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TransformableImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }
	
	public float scale = 1.0f;
	public float angle = 0.0f;
	public boolean flip = false;
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save();
		canvas.scale(this.scale, this.scale);
		canvas.translate(canvas.getWidth()*(1-scale)/2.0f, canvas.getHeight()*(1-scale)/2.0f);
		canvas.rotate(angle);
		if(this.flip) {
			canvas.scale(-1.0f,1.0f);
			canvas.translate(canvas.getWidth()*-.5f,0);
		}
		super.onDraw(canvas);
		canvas.restore();
	}
}
