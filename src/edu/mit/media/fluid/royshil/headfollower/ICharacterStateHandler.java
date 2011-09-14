package edu.mit.media.fluid.royshil.headfollower;

public interface ICharacterStateHandler {

	public abstract void onCharacterStateChanged(float[] state);

	public abstract void onCalibrationStateChanged(int[] currentState);

}