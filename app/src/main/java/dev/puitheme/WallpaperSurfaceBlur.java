// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** External wallpaper bridge using the OEM's actual wallpaper-window StateFlow.
 * The only borrowed surface is OplusWindowInfo.mSurfaceControl for TYPE_WALLPAPER.
 * Our windowless host is below native immersive surfaces (+1/+2), and never
 * captures/replaces a live wallpaper frame or draws in the shade's ViewRoot.
 * Blur belongs to an owned compositor effect layer, not a RenderNode in an empty
 * transparent host. Physical-pixel feather samples are merged at native alpha
 * precision; this is still a compositor approximation rather than a gradient
 * shader. Native submission requires on-device visual/GPU validation.
 */
final class WallpaperSurfaceBlur {
    static final String CONTROLLER = "com.oplus.systemui.keyguard.anim.OplusWallpaperAnimControllerImpl";
    static final String FLOW = "kotlinx.coroutines.flow.StateFlowImpl";
    static final String[] WINDOW_INFO_TASKS={CONTROLLER+"$onWallpaperSurfaceChanged$3",
            CONTROLLER+"$onPictorialWallpaperSurfaceChanged$2"};
    private static final String INFO = "com.oplus.app.OplusWindowInfo";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean posted = new AtomicBoolean();
    private final Runnable work = () -> { posted.set(false); sync(); };
    private WeakReference<Object> controller = new WeakReference<>(null), flow = new WeakReference<>(null);
    private WeakReference<View> shade = new WeakReference<>(null);
    private Field nativeFlow, nativeVisible;
    private Method flowValue;
    private NativeAccess api;
    private WindowAccess window;
    private Object borrowedParent, group, effectSurface, host, hostPackage, hostSurface, failedParent;
    private Backdrop backdrop;
    private boolean enabled, crossWindow = true;
    private float visibility, radius, range, transition, alpha = -1f;
    private int color, width, height;
    private float scaleX, scaleY;
    private int offsetX, offsetY;
    private long builds, commits, failures;
    private String bridgeState = "inactive";
    private long revision, plannedRevision = -1;
    private boolean apiUnavailable;
    private float plannedDensity;
    private int displayHeight;
    private LockscreenBlurGeometry.Plan currentPlan;
    private float[][] blurRegions;
    private int regionWidth, regionCore, regionEnd, regionRadius = -1;
    private boolean blurUnavailable;
    private String blurState = "inactive";
    private final Point displaySize = new Point();
    private final android.graphics.Rect crop = new android.graphics.Rect();
    private final Matrix inverse = new Matrix();
    private final float[] matrixValues = new float[9];

    synchronized void configure(boolean enabled, boolean crossWindow, float visibility, float radius,
                   float range, float transition, int color) {
        if (this.enabled == enabled && this.crossWindow == crossWindow && this.visibility == visibility
                && this.radius == radius && this.range == range && this.transition == transition && this.color == color) return;
        if (this.radius != radius || this.range != range || this.transition != transition) revision++;
        if (this.enabled != enabled) { failedParent = null; apiUnavailable = false; }
        this.enabled = enabled; this.crossWindow = crossWindow; this.visibility = visibility;
        this.radius = radius; this.range = range; this.transition = transition; this.color = color;
        request();
    }
    synchronized void onShade(View view) {
        if (view == null || !named(view.getClass(), LockscreenStatusBlur.SHADE_ROOT)) return;
        if (shade.get() != view) { shade = new WeakReference<>(view); failedParent = null; apiUnavailable = false; request(); }
    }
    synchronized void onShadeDetached(View view) {
        if (shade.get() == view) { shade.clear(); request(); }
    }
    synchronized void onController(Object owner) {
        if (owner == null || !named(owner.getClass(), CONTROLLER)) return;
        if (controller.get() != owner) {
            controller = new WeakReference<>(owner);
            nativeFlow = field(owner.getClass(), "_lockScreenWallpaperWindowInfoFlow");
            nativeVisible = field(owner.getClass(), "wallpaperVisiable");
            failedParent = null;
        }
        try {
            Object value = nativeFlow == null ? null : nativeFlow.get(owner);
            if (flow.get() != value) {
                flow = new WeakReference<>(value); flowValue = value == null ? null : method(value.getClass(), "getValue");
            }
        } catch (ReflectiveOperationException | RuntimeException unavailable) { flow.clear(); }
        request();
    }
    synchronized void onFlowChanged(Object owner) { if (flow.get() == owner) request(); }
    /** These two audited suspend callbacks publish WindowInfo before returning.
     * Observe their exact owners instead of intercepting every SystemUI StateFlow. */
    synchronized void onWindowInfoTask(Object task) {
        if(task==null)return;
        String name=task.getClass().getName();
        if(!WINDOW_INFO_TASKS[0].equals(name)&&!WINDOW_INFO_TASKS[1].equals(name))return;
        try{Field owner=field(task.getClass(),"this$0");if(owner!=null)onController(owner.get(task));}
        catch(ReflectiveOperationException|RuntimeException unavailable){ }
    }
    private void request() {
        // Even native drawing callbacks only enqueue. Host/tree/compositor changes
        // must not run inside another ViewRoot's draw or a coroutine worker.
        if (posted.compareAndSet(false, true) && !main.post(work)) posted.set(false);
    }
    private synchronized void sync() {
        Object t = null;
        try {
            View root = shade.get(); Object owner = controller.get(), state = flow.get();
            if (!enabled || root == null || !root.isAttachedToWindow() || owner == null
                    || state == null || nativeVisible == null || flowValue == null) { bridgeState = !enabled ? "disabled" : "awaiting-native-source"; release(); return; }
            if (!nativeVisible.getBoolean(owner) || visibility <= 0f) { bridgeState = "native-hidden"; hide(); return; }
            Object info = flowValue.invoke(state);
            if (info == null || !INFO.equals(info.getClass().getName())) { release(); return; }
            if (window == null) window = new WindowAccess(info.getClass());
            if (!window.ready || window.type.getInt(info) != 2013
                    || "com.android.systemui".equals(window.packageName.get(info))) { release(); return; }
            if (apiUnavailable) return;
            if (api == null) {
                try { api = new NativeAccess(root.getClass().getClassLoader()); }
                catch (ReflectiveOperationException | RuntimeException unavailable) { apiUnavailable = true; throw unavailable; }
            }
            Object parent = window.parent.get(info);
            if (!api.valid(parent)) { release(); return; }
            if (!api.same(parent, borrowedParent)) { release(); borrowedParent = parent; failedParent = null; }
            if (api.same(parent, failedParent)) return;
            Context context = root.getContext(); Object display = api.display.invoke(context);
            if (display == null || ((Number)api.displayId.invoke(display)).intValue() != 0) { release(); return; }
            Point size = displaySize; api.displaySize.invoke(display, size);
            Object extras = window.extension.get(info);
            if (!(extras instanceof Bundle) || size.x <= 0 || size.y <= 0) { hide(); return; }
            Bundle bundle = (Bundle)extras;
            // These are the same read-only extension keys used by the OEM's
            // attachHostSurfaceToWallpaper/buildWallpaperInverseMatrix contract.
            float sx = scalar(bundle.get("HScale")), sy = scalar(bundle.get("VScale"));
            Object point = bundle.get("offset");
            if (!Float.isFinite(sx) || !Float.isFinite(sy) || !(point instanceof Point)) { hide(); return; }
            Point offset = (Point)point;
            // Rotation's axes can be temporarily stale in WindowInfo. Admit the
            // native portrait mapping only; hide/recover rather than guessing.
            if (size.x >= size.y || Math.abs((long)offset.x) > size.x * 4L || Math.abs((long)offset.y) > size.y * 4L) { hide(); return; }
            float density = context.getResources().getDisplayMetrics().density;
            if (currentPlan == null || plannedRevision != revision || displayHeight != size.y || plannedDensity != density) {
                currentPlan = LockscreenBlurGeometry.plan(size.y, density, radius, range, transition);
                plannedRevision = revision; displayHeight = size.y; plannedDensity = density;
            }
            LockscreenBlurGeometry.Plan plan = currentPlan;
            if (plan.size() == 0) { hide(); return; }
            boolean geometry = backdrop == null || width != size.x || height != plan.end || backdrop.plan.core != plan.core
                    || backdrop.radius != (crossWindow ? plan.radius : 0) || backdrop.color != color;
            boolean transform = scaleX != sx || scaleY != sy || offsetX != offset.x || offsetY != offset.y;
            if (group == null || width != size.x || height != plan.end) {
                Object keep = borrowedParent; release(); borrowedParent = keep;
                group = api.createGroup(parent); builds++;
                Object viewRoot = api.viewRoot.invoke(root);
                Object token = viewRoot == null ? null : api.inputToken.invoke(viewRoot);
                if (token == null) throw new IllegalStateException("Native shade input-transfer token unavailable");
                host = api.host.newInstance(context, display, token, "C17 wallpaper status backdrop");
                backdrop = new Backdrop(context, api);
                backdrop.configure(size.x, plan, crossWindow ? plan.radius : 0, color);
                WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
                layout.width = size.x; layout.height = plan.end;
                layout.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                layout.format = android.graphics.PixelFormat.TRANSLUCENT;
                api.setView.invoke(host, backdrop, layout);
                backdrop.measure(View.MeasureSpec.makeMeasureSpec(size.x, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(plan.end, View.MeasureSpec.EXACTLY));
                backdrop.layout(0, 0, size.x, plan.end);
                hostPackage = api.surfacePackage.invoke(host);
                hostSurface = hostPackage == null ? null : api.packageSurface.invoke(hostPackage);
                if (!api.valid(hostSurface)) throw new IllegalStateException("Owned wallpaper host unavailable");
                geometry = transform = true;
            } else if (geometry) { backdrop.configure(size.x, plan, crossWindow ? plan.radius : 0, color); }
            if (!geometry && !transform && alpha == visibility) return;
            t = api.transaction.newInstance();
            if (geometry) {
                api.reparent.invoke(t, hostSurface, group);
                // Native wallpaper buffer children are z=0; our newer child is
                // after the wallpaper, while OEM immersive children are +1/+2.
                api.relative.invoke(t, group, parent, 0);
                crop.set(0, 0, size.x, plan.end); api.crop.invoke(t, group, crop);
                api.layer.invoke(t, hostSurface, 1);
                applyBlur(t, size.x, plan, crossWindow ? plan.radius : 0);
                api.show.invoke(t, hostSurface);
            }
            if (transform) {
                inverse.setScale(1f / sx, 1f / sy); inverse.preTranslate(-offset.x, -offset.y);
                api.matrix.invoke(t, group, inverse, matrixValues);
            }
            api.alpha.invoke(t, group, visibility); api.show.invoke(t, group); api.apply.invoke(t); commits++;
            width = size.x; height = plan.end; scaleX = sx; scaleY = sy; offsetX = offset.x; offsetY = offset.y; alpha = visibility;
            bridgeState = "attached";
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            Object parent = borrowedParent; release(); borrowedParent = failedParent = parent; failures++; bridgeState = "host-unavailable";
        } finally { if (api != null) api.close(t); }
    }
    private void applyBlur(Object transaction, int width, LockscreenBlurGeometry.Plan plan, int radius) {
        if (radius <= 0) {
            try { if (effectSurface != null) api.hide.invoke(transaction, effectSurface); }
            catch (ReflectiveOperationException | RuntimeException ignored) { }
            if (regionRadius != 0) {
                blurRegions = new float[0][]; regionRadius = 0;
                if (effectSurface != null) try { api.regions.invoke(transaction, effectSurface, (Object) blurRegions); }
                catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
            blurState = "disabled"; return;
        }
        if (!api.canBlur() || blurUnavailable) { blurState = "unavailable-mask-only"; return; }
        try {
            // One effect layer samples already-composed lower layers. Our group is
            // above wallpaper's z=0 buffer and below OEM foreground children +1/+2;
            // the host mask is +1 INSIDE our group and is also above the blur.
            if (effectSurface == null && radius > 0) {
                effectSurface = api.createEffect(group);
                api.layer.invoke(transaction, effectSurface, 0);
                crop.set(0, 0, width, plan.end); api.crop.invoke(transaction, effectSurface, crop);
            }
            if (effectSurface == null) { blurState = "disabled"; return; }
            if (blurRegions == null || regionWidth != width || regionCore != plan.core
                    || regionEnd != plan.end || regionRadius != radius) {
                blurRegions = WallpaperBlurRenderer.compositorRegions(width, plan, radius, api.blurParameters);
                regionWidth = width; regionCore = plan.core; regionEnd = plan.end; regionRadius = radius;
                api.regions.invoke(transaction, effectSurface, (Object) blurRegions);
            }
            api.show.invoke(transaction, effectSurface);
            blurState = radius > 0 ? "compositor-pixel-feather-unverified" : "disabled";
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            blurUnavailable = true; blurState = "unavailable-mask-only";
            if (effectSurface != null) try { api.regions.invoke(transaction, effectSurface, (Object) new float[0][]); }
            catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
    }
    private static float scalar(Object value) {
        float result = value instanceof Number ? ((Number)value).floatValue() : Float.NaN;
        return Float.isFinite(result) && result >= .25f && result <= 4f ? result : Float.NaN;
    }
    private void hide() {
        if (api == null || group == null || alpha == 0f) return;
        Object t = null;
        try { if (api.valid(group)) { t = api.transaction.newInstance(); api.hide.invoke(t, group); api.apply.invoke(t); commits++; } alpha = 0f; }
        catch (ReflectiveOperationException | RuntimeException unavailable) { release(); }
        finally { api.close(t); }
    }
    private void release() {
        if (backdrop != null) backdrop.releaseEffects();
        Object t = null;
        if (api != null) {
            try { if (api.valid(group)) { t = api.transaction.newInstance(); api.remove.invoke(t, group); api.apply.invoke(t); commits++; } }
            catch (ReflectiveOperationException | RuntimeException ignored) { }
            finally { api.close(t); }
            if (host != null) try { api.releaseHost.invoke(host); } catch (ReflectiveOperationException | RuntimeException ignored) { }
            // SurfacePackage is a copied OWNED host handle. Never release WindowInfo's parent.
            if (hostPackage != null) try { api.releasePackage.invoke(hostPackage); } catch (ReflectiveOperationException | RuntimeException ignored) { }
            if (group != null) try { api.release.invoke(group); } catch (ReflectiveOperationException | RuntimeException ignored) { }
            if (effectSurface != null) try { api.release.invoke(effectSurface); } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        group = effectSurface = host = hostPackage = hostSurface = borrowedParent = null; backdrop = null;
        blurRegions = null; regionRadius = -1; blurUnavailable = false; blurState = "inactive";
        width = height = 0; alpha = -1f; scaleX = scaleY = 0f;
    }
    synchronized void releaseRuntime() { enabled = false; controller.clear(); flow.clear(); shade.clear(); failedParent = null;
        currentPlan = null; plannedRevision = -1; request(); }
    synchronized String summary() { return "externalWallpaperBridge=" + (group != null) + ",externalBackdropHosts=" + builds
            + ",externalBackdropCommits=" + commits + ",externalBackdropFallback=" + failures
            + ",externalBackdropState=" + bridgeState + ",externalMaskAlpha=" + (color >>> 24)
            + ",externalMaskVisibility=" + (alpha < 0f ? 0f : alpha)
            + ",externalGradientBlur=" + blurState + ",externalBlurRegions=" + (blurRegions == null ? 0 : blurRegions.length)
            + ",externalBlurAlphaPrecision=255,externalBlurDistinctRadii=" + (regionRadius > 0 ? 1 : 0)
            + ",externalBackdropVisual=unverified"; }

    private static final class Backdrop extends FrameLayout {
        final Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        LockscreenBlurGeometry.Plan plan;
        int width, radius, color;
        Backdrop(Context context, NativeAccess api) {
            super(context);
            setClickable(false); setFocusable(false); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        void configure(int width, LockscreenBlurGeometry.Plan plan, int radius, int color) {
            boolean resize = this.width != width || this.plan == null || this.plan.end != plan.end;
            boolean shader = this.plan == null || this.plan.core != plan.core || this.plan.end != plan.end || this.color != color;
            this.width = width; this.plan = plan; this.radius = radius; this.color = color;
            if (shader) { mask.setShader(WallpaperBlurRenderer.gradient(plan, color)); invalidate(); }
            if (resize) requestLayout();
        }
        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(width, plan.end);
        }
        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }
        void releaseEffects() {
            mask.setShader(null);
        }
        @Override protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if ((color >>> 24) != 0 && plan != null) canvas.drawRect(0, 0, width, plan.end, mask);
        }
    }
    private static final class NativeAccess {
        final Constructor<?> builder, transaction, host;
        final Method name, parent, container, hidden, build, valid, same, release;
        final Method relative, layer, crop, reparent, matrix, alpha, show, hide, remove, apply, close;
        final Method display, displayId, displaySize, viewRoot, inputToken, setView, surfacePackage, packageSurface, releaseHost, releasePackage;
        final Method effect, regions;
        final float[] blurParameters;
        NativeAccess(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> sc = Class.forName("android.view.SurfaceControl"), b = Class.forName("android.view.SurfaceControl$Builder");
            Class<?> t = Class.forName("android.view.SurfaceControl$Transaction"), h = Class.forName("android.view.SurfaceControlViewHost");
            Class<?> p = Class.forName("android.view.SurfaceControlViewHost$SurfacePackage"), d = Class.forName("android.view.Display");
            Class<?> token = Class.forName("android.window.InputTransferToken"), vri = Class.forName("android.view.ViewRootImpl");
            builder = b.getDeclaredConstructor(); transaction = t.getDeclaredConstructor();
            host = h.getDeclaredConstructor(Context.class, d, token, String.class); host.setAccessible(true);
            name = required(b,"setName",String.class); parent = required(b,"setParent",sc); container = required(b,"setContainerLayer");
            hidden = required(b,"setHidden",boolean.class); build = required(b,"build"); valid = required(sc,"isValid");
            same = required(sc,"isSameSurface",sc); release = required(sc,"release");
            relative = required(t,"setRelativeLayer",sc,sc,int.class); layer = required(t,"setLayer",sc,int.class);
            crop = required(t,"setCrop",sc,android.graphics.Rect.class);
            reparent = required(t,"reparent",sc,sc); matrix = required(t,"setMatrix",sc,Matrix.class,float[].class);
            alpha = required(t,"setAlpha",sc,float.class); show = required(t,"show",sc); hide = required(t,"hide",sc);
            remove = required(t,"remove",sc); apply = required(t,"apply"); close = required(t,"close");
            display = required(Context.class,"getDisplay"); displayId = required(d,"getDisplayId"); displaySize = required(d,"getRealSize",Point.class);
            viewRoot = required(View.class,"getViewRootImpl"); inputToken = required(vri,"getInputTransferToken");
            setView = required(h,"setView",View.class,WindowManager.LayoutParams.class); surfacePackage = required(h,"getSurfacePackage");
            packageSurface = required(p,"getSurfaceControl"); releaseHost = required(h,"release"); releasePackage = required(p,"release");
            Method optionalEffect = null, optionalRegions = null;
            float[] nativeParameters = null;
            try {
                optionalEffect = method(b,"setEffectLayer"); optionalRegions = method(t,"setBlurRegions",sc,float[][].class);
                Class<?> params = Class.forName("com.oplus.graphics.OplusBlurParam",false,loader);
                Object value = required(params,"toFloatArray").invoke(params.getDeclaredConstructor().newInstance());
                if (value instanceof float[] && ((float[])value).length == 18) {
                    nativeParameters = ((float[])value).clone();
                    for (float parameter : nativeParameters) if (!Float.isFinite(parameter))
                        throw new IllegalStateException("Invalid native wallpaper blur parameters");
                    nativeParameters[0] = nativeParameters[1] = 0f; nativeParameters[2] = 1f;
                    for (int i = 3; i < 16; i++) nativeParameters[i] = 0f;
                }
            } catch (ReflectiveOperationException | RuntimeException optionalUnavailable) { nativeParameters = null; }
            effect = optionalEffect; regions = optionalRegions; blurParameters = nativeParameters;
        }
        boolean canBlur() { return effect != null && regions != null && blurParameters != null; }
        Object createEffect(Object owner) throws ReflectiveOperationException {
            Object b = builder.newInstance(); name.invoke(b,"C17 wallpaper compositor feather"); parent.invoke(b,owner);
            effect.invoke(b); hidden.invoke(b,true); return build.invoke(b);
        }
        Object createGroup(Object owner) throws ReflectiveOperationException {
            Object b = builder.newInstance(); name.invoke(b,"C17 external wallpaper status backdrop"); parent.invoke(b,owner);
            container.invoke(b); hidden.invoke(b,true); return build.invoke(b);
        }
        boolean valid(Object value) throws ReflectiveOperationException { return value != null && Boolean.TRUE.equals(valid.invoke(value)); }
        boolean same(Object a,Object b) throws ReflectiveOperationException { return a != null && b != null && Boolean.TRUE.equals(same.invoke(a,b)); }
        void close(Object value) { if (value != null) try { close.invoke(value); } catch (ReflectiveOperationException | RuntimeException ignored) { } }
    }
    private static final class WindowAccess {
        final Field type, parent, packageName, extension;
        final boolean ready;
        WindowAccess(Class<?> value) {
            type=field(value,"type"); parent=field(value,"mSurfaceControl"); packageName=field(value,"packageName"); extension=field(value,"extension");
            ready=type!=null&&parent!=null&&packageName!=null&&extension!=null;
        }
    }
    private static boolean named(Class<?> type,String name) { for(Class<?> at=type;at!=null;at=at.getSuperclass())if(name.equals(at.getName()))return true;return false; }
    private static Method required(Class<?> type,String name,Class<?>... parameters) throws NoSuchMethodException {
        Method value=method(type,name,parameters);if(value==null)throw new NoSuchMethodException(type.getName()+"."+name);return value;
    }
    private static Method method(Class<?> type,String name,Class<?>... parameters) {
        for(Class<?> at=type;at!=null;at=at.getSuperclass())try{Method value=at.getDeclaredMethod(name,parameters);value.setAccessible(true);return value;}
        catch(ReflectiveOperationException|RuntimeException ignored){}return null;
    }
    private static Field field(Class<?> type,String name) {
        for(Class<?> at=type;at!=null;at=at.getSuperclass())try{Field value=at.getDeclaredField(name);value.setAccessible(true);return value;}
        catch(ReflectiveOperationException|RuntimeException ignored){}return null;
    }
}
