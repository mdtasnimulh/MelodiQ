package android.hardware.radio;

/** Compile-time stub of the @SystemApi class; see build.gradle.kts. Never packaged. */
public final class ProgramSelector {
    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz) {
        throw new RuntimeException("Stub!");
    }

    public Identifier getPrimaryId() { throw new RuntimeException("Stub!"); }

    public static final class Identifier {
        public long getValue() { throw new RuntimeException("Stub!"); }
    }
}
