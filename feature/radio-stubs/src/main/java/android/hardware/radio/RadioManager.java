package android.hardware.radio;

import android.os.Handler;
import java.util.List;

/** Compile-time stub of the @SystemApi class; see build.gradle.kts. Never packaged. */
public class RadioManager {
    public static final int STATUS_OK = 0;
    public static final int BAND_INVALID = -1;
    public static final int BAND_AM = 0;
    public static final int BAND_FM = 1;
    public static final int BAND_AM_HD = 2;
    public static final int BAND_FM_HD = 3;

    public int listModules(List<ModuleProperties> modules) { throw new RuntimeException("Stub!"); }

    public RadioTuner openTuner(int moduleId, BandConfig config, boolean withAudio,
                                RadioTuner.Callback callback, Handler handler) {
        throw new RuntimeException("Stub!");
    }

    public static class ModuleProperties {
        public int getId() { throw new RuntimeException("Stub!"); }
        public BandDescriptor[] getBands() { throw new RuntimeException("Stub!"); }
    }

    public static class BandDescriptor {
        public int getType() { throw new RuntimeException("Stub!"); }
    }

    public static abstract class BandConfig { }

    public static class ProgramInfo {
        public ProgramSelector getSelector() { throw new RuntimeException("Stub!"); }
    }
}
