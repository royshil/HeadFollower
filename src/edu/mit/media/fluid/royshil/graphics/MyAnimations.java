package edu.mit.media.fluid.royshil.graphics;

import java.util.HashMap;

public class MyAnimations {
	public class MyAnim {
		public String filename; 
		public int start;
		public int end;
		public boolean loop;
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
	
	public enum Animations { 
		TURN,
		THREE_QUARTERS_TO_PROFILE,
		START_WALK,
		END_WALK,
		WALK,
		SHAKE_HAND,
		WAVE, NATURAL
	}
	
	public enum Character {
		BLUE,
		RED
	}
	
	private static HashMap<Animations, MyAnim> blue_animation_index = new HashMap<Animations, MyAnim>();
	private static HashMap<Animations, MyAnim> red_animation_index = new HashMap<Animations, MyAnim>();
	private static MyAnimations myAnimations = new MyAnimations();
	
	static {
		blue_animation_index.put(Animations.TURN,							myAnimations.new MyAnim("guy/guy_",329,341));
//		blue_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
		blue_animation_index.put(Animations.START_WALK, 					myAnimations.new MyAnim("guy/guy_",37,57));
		blue_animation_index.put(Animations.END_WALK, 						myAnimations.new MyAnim("guy/guy_",90,111));
		blue_animation_index.put(Animations.WALK, 							myAnimations.new MyAnim("guy/guy_",58,89,true));
		blue_animation_index.put(Animations.WAVE, 							myAnimations.new MyAnim("guy/guy_",113,190));
		blue_animation_index.put(Animations.SHAKE_HAND, 					myAnimations.new MyAnim("guy/guy_",266,325));
		blue_animation_index.put(Animations.NATURAL, 						myAnimations.new MyAnim("guynatural3q.png",-1,-1));

		red_animation_index.put(Animations.TURN,							myAnimations.new MyAnim("girl/girl_",301,323));
//		red_animation_index.put(Animations.THREE_QUARTERS_TO_PROFILE, 		R.drawable.anim_look_l_to_r);
		red_animation_index.put(Animations.START_WALK, 						myAnimations.new MyAnim("girl/girl_",45,63));
		red_animation_index.put(Animations.END_WALK, 						myAnimations.new MyAnim("girl/girl_",97,112));
		red_animation_index.put(Animations.WALK, 							myAnimations.new MyAnim("girl/girl_",64,96,true));
		red_animation_index.put(Animations.WAVE, 							myAnimations.new MyAnim("girl/girl_",242,300));
		red_animation_index.put(Animations.SHAKE_HAND, 						myAnimations.new MyAnim("girl/girl_",160,235));
		red_animation_index.put(Animations.NATURAL, 						myAnimations.new MyAnim("girl/girl_0035.png",-1,-1));
	}
	
	public static MyAnim getAnimation(Animations a, Character c) {
		MyAnim myAnim = (c == Character.BLUE) ? blue_animation_index.get(a) : red_animation_index.get(a);
		return myAnim;
	}
}
