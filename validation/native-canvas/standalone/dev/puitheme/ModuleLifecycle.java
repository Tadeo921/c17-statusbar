package dev.puitheme;
/** The standalone raster probe has no installed module or package-removal lifecycle.
 * Never compile this fixture into the app or the desktop lifecycle checks. */
public final class ModuleLifecycle {
    private ModuleLifecycle() { }
    public static boolean removed() { return false; }
}
