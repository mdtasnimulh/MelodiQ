package android.hardware.radio;

/** Compile-time stub of the @SystemApi class; see build.gradle.kts. Never packaged. */
public abstract class RadioTuner {
    public abstract void close();
    public abstract int setMute(boolean mute);
    public abstract int tune(ProgramSelector selector);
    public abstract int cancel();

    public abstract static class Callback {
        public void onError(int status) { }
        public void onTuneFailed(int status, ProgramSelector selector) { }
        public void onProgramInfoChanged(RadioManager.ProgramInfo info) { }
        public void onAntennaState(boolean connected) { }
    }
}
