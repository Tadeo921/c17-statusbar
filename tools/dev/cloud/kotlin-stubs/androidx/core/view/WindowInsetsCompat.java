package androidx.core.view;
import androidx.core.graphics.Insets;
public class WindowInsetsCompat {
    public static WindowInsetsCompat toWindowInsetsCompat(android.view.WindowInsets insets, android.view.View view) { return new WindowInsetsCompat(); }
    public boolean isVisible(int types) { return false; }
    public Insets getInsets(int types) { return new Insets(); }
    public static final class Type { public static int ime() { return 8; } public static int navigationBars() { return 2; } public static int displayCutout() { return 128; } public static int systemBars() { return 7; } public static int statusBars() { return 1; } }
}
