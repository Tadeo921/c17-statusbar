package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Desktop checks for native fake-frame ink bounds, scoped drawing and cache/lifecycle ownership. */
public final class StatusBarLeftCopySpacingCheck {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void near(float actual, float wanted, String message) {
        check(Float.isFinite(actual) && Math.abs(actual - wanted) < .0001f,
                message + ": " + actual + " != " + wanted);
    }

    private static final class Node extends View {
        int width, height = 100, direction, propertyWrites, screenX;
        float globalX, globalY, globalScaleX = 1f, globalScaleY = 1f;
        float skewX, skewY, perspectiveX, perspectiveY, homogeneous = 1f;
        ViewGroup.MarginLayoutParams originalParams;
        int originalWidth, originalHeight;
        final Resources resources = new Resources();
        Node(int size) { super(new Context()); width = size; resources.getDisplayMetrics().density = 4f; }
        @Override public int getWidth() { return width; }
        @Override public void getLocationOnScreen(int[] output) { output[0] = screenX; output[1] = 0; }
        @Override public int getHeight() { return height; }
        @Override public int getLayoutDirection() { return direction; }
        @Override public Resources getResources() { return resources; }
        @Override public void transformMatrixToGlobal(Matrix target) {
            // The fixture installs the final fake-frame transform, including native spring/pivot/parents.
            // Matrix.setRotate in the shared stub only records its arguments, so custom axes are explicit.
            try {
                Field field = Matrix.class.getDeclaredField("values"); field.setAccessible(true);
                float[] axes = (float[]) field.get(target);
                float[] values = {globalScaleX, skewX, globalX, skewY, globalScaleY, globalY,
                        perspectiveX, perspectiveY, homogeneous};
                System.arraycopy(values, 0, axes, 0, values.length);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
        @Override public void setAlpha(float value) { propertyWrites++; super.setAlpha(value); }
        @Override public void setTransitionAlpha(float value) { propertyWrites++; super.setTransitionAlpha(value); }
        @Override public void setTranslationX(float value) { propertyWrites++; super.setTranslationX(value); }
        @Override public void setTranslationY(float value) { propertyWrites++; super.setTranslationY(value); }
        @Override public void setScaleX(float value) { propertyWrites++; super.setScaleX(value); }
        @Override public void setScaleY(float value) { propertyWrites++; super.setScaleY(value); }
        @Override public void setVisibility(int value) { propertyWrites++; super.setVisibility(value); }
        @Override public void setLayoutParams(ViewGroup.LayoutParams value) { propertyWrites++; super.setLayoutParams(value); }
        @Override public void setPadding(int l, int t, int r, int b) { propertyWrites++; super.setPadding(l, t, r, b); }
        @Override public void setClipBounds(android.graphics.Rect value) { propertyWrites++; super.setClipBounds(value); }
    }

    /** The shared Canvas records saves without restoring transforms; this fixture models that real contract. */
    private static final class ScopedCanvas extends Canvas {
        final Map<Integer, float[]> stack = new HashMap<>();
        @Override public int save() {
            int count = super.save();
            stack.put(count, new float[]{translateX, translateY, scaleX, scaleY}); return count;
        }
        @Override public void restoreToCount(int count) {
            float[] state = stack.get(count);
            if (state == null) throw new AssertionError("Unbalanced Canvas restore: " + count);
            translateX = state[0]; translateY = state[1]; scaleX = state[2]; scaleY = state[3];
            for (Integer key : new ArrayList<>(stack.keySet())) if (key >= count) stack.remove(key);
            super.restoreToCount(count);
        }
    }

    private static final class Fixture {
        final ViewGroup phoneRoot = new ViewGroup(new Context()), row = new ViewGroup(new Context());
        final ViewGroup clockContainer = new ViewGroup(new Context()), iconContainer = new ViewGroup(new Context());
        final Node clock = new Node(182), icons = new Node(1217), timeSource = new Node(182), iconSource = new Node(119);
        float clockOffset;
        final StatusBarLeftCopySpacing owner = new StatusBarLeftCopySpacing(source -> clockOffset);
        Fixture() {
            phoneRoot.addView(timeSource); phoneRoot.addView(iconSource);
            row.addView(clockContainer); row.addView(iconContainer);
            clockContainer.addView(clock); iconContainer.addView(icons);
            clock.globalX = 71f; icons.globalX = 223f;
            for (Node view : nodes()) {
                view.setTranslationX(9f); view.setTranslationY(-7f);
                view.setScaleX(.85f); view.setScaleY(.75f);
                view.setAlpha(.8f); view.setTransitionAlpha(.9f);
                view.originalParams = new ViewGroup.MarginLayoutParams(view.width, view.height);
                view.originalParams.leftMargin = 11; view.originalParams.rightMargin = 13;
                view.originalWidth = view.width; view.originalHeight = view.height;
                view.setLayoutParams(view.originalParams);
                view.propertyWrites = 0; view.layoutRequests = 0;
            }
            owner.configure(true);
            restingGap(40f);
        }
        /** Where the real views keep the clock and notifications at rest: the room the animation may ask for. */
        void restingGap(float pixels) {
            timeSource.screenX = 71;
            iconSource.screenX = (int) (timeSource.screenX + clockOffset + timeSource.width + pixels);
        }
        Node[] nodes() { return new Node[]{clock, icons, timeSource, iconSource}; }
        void bind() {
            owner.observe(clock, timeSource, StatusIconTransition.CLOCK, 0f);
            owner.observe(icons, iconSource, StatusIconTransition.NOTIFICATIONS, 30f);
        }
        void noPropertyWrites(String phase) {
            for (Node view : nodes()) {
                check(view.propertyWrites == 0 && view.layoutRequests == 0, phase + " preserves native View properties");
                near(view.getTranslationX(), 9f, phase + " preserves native X");
                near(view.getTranslationY(), -7f, phase + " preserves native Y");
                near(view.getAlpha(), .8f, phase + " preserves native alpha");
                near(view.getTransitionAlpha(), .9f, phase + " preserves native transition alpha");
                near(view.getScaleX(), .85f, phase + " preserves native scale X");
                near(view.getScaleY(), .75f, phase + " preserves native scale Y");
                check(view.getLayoutParams() == view.originalParams && view.originalParams.width == view.originalWidth
                        && view.originalParams.height == view.originalHeight && view.originalParams.leftMargin == 11
                        && view.originalParams.rightMargin == 13, phase + " retains native LayoutParams identity and fields");
            }
        }
        void disposed() {
            owner.clear();
            check(row.getViewTreeObserver().preDrawListeners.isEmpty() && row.attachListeners.isEmpty(),
                    "Clear removes the exact row listeners");
            for (Node view : nodes()) check(view.attachListeners.isEmpty(), "Clear releases fake/source attach listeners");
        }
    }

    private static float distance(Fixture fixture) {
        ScopedCanvas canvas = new ScopedCanvas();
        StatusBarLeftCopySpacing.DrawScope scope = fixture.owner.beginDraw(fixture.icons, canvas);
        float value = canvas.translateX;
        if (scope != null) scope.close();
        near(canvas.translateX, 0f, "Drawing releases its Canvas translation");
        check(canvas.getSaveCount() == 1, "Drawing balances the native save count");
        return value;
    }
    private static void predraw(View row) {
        for (ViewTreeObserver.OnPreDrawListener listener : new ArrayList<>(row.getViewTreeObserver().preDrawListeners))
            check(listener.onPreDraw(), "Spacing never cancels the native traversal");
    }
    private static void detach(View view) {
        view.attached = false;
        for (View.OnAttachStateChangeListener listener : new ArrayList<>(view.attachListeners))
            listener.onViewDetachedFromWindow(view);
    }

    private static void nativeOffsetsAndScreenGap() {
        Fixture f = new Fixture(); f.bind();
        near(distance(f), 16f, "Native 30px starts notifications at 253; only screen 4dp is added");
        ScopedCanvas clockCanvas = new ScopedCanvas();
        check(f.owner.beginDraw(f.clock, clockCanvas) == null && clockCanvas.saves == 0,
                "The clock canvas is never shifted by the notification helper");
        f.clockOffset = 22f;
        near(distance(f), 38f, "Clock draw X participates in the full native ink budget");
        f.timeSource.width = 300; f.restingGap(40f);
        near(distance(f), 156f, "A long native clock source uses its whole width without glyph guesses");
        f.icons.globalX = 420f;
        near(distance(f), 0f, "Existing native spacing is never reduced");
        f.icons.resources.getDisplayMetrics().density = 2f; f.icons.globalX = 363f;
        near(distance(f), 8f, "The minimum is screen dp rather than a fixed pixel constant");
        f.noPropertyWrites("LTR and native geometry changes"); f.disposed();

        Fixture scaled = new Fixture(); scaled.bind();
        scaled.clock.globalScaleX = scaled.clock.globalScaleY = .5f;
        scaled.icons.globalScaleX = scaled.icons.globalScaleY = .5f;
        scaled.icons.globalX = 147f;
        near(distance(scaled), 32f, "Parent spring scale .5 requires local 32px to produce screen 16px");
        scaled.clockOffset = 12f;
        near(distance(scaled), 44f, "The clock local offset is mapped through its own spring once");
        scaled.noPropertyWrites("Native spring"); scaled.disposed();

        Fixture rtl = new Fixture(); rtl.bind();
        rtl.clock.direction = rtl.icons.direction = View.LAYOUT_DIRECTION_RTL;
        rtl.clock.globalX = 1000f; rtl.icons.globalX = -187f;
        near(distance(rtl), -16f, "RTL uses fakeWidth minus sourceWidth minus native 30px");
        rtl.clock.globalScaleX = rtl.clock.globalScaleY = .5f;
        rtl.icons.globalScaleX = rtl.icons.globalScaleY = .5f; rtl.icons.globalX = 406.5f;
        near(distance(rtl), -32f, "RTL screen gap survives the native spring scale");
        rtl.noPropertyWrites("RTL"); rtl.disposed();
    }

    /** The animation converges on the real layout: no gap it asks for may disappear when the real views take over. */
    private static void resting() {
        Fixture f = new Fixture(); f.bind();
        f.restingGap(0f);
        near(distance(f), 0f, "A real layout that keeps no gap gets no extra gap from the copies");
        f.restingGap(10f);
        near(distance(f), 10f, "The copies keep exactly the real resting gap when it is under 4dp");
        f.restingGap(16f); near(distance(f), 16f, "The 4dp rule applies once the real gap reaches it");
        f.restingGap(300f); near(distance(f), 16f, "A larger real gap never raises the requested 4dp");
        f.restingGap(0f);
        float previous = Float.MAX_VALUE;
        for (int x = 200; x <= 253; x += 3) {
            f.icons.globalX = x - 30f;
            float now = distance(f);
            check(now <= previous + .0001f, "The correction shrinks steadily as the copies approach the real layout");
            previous = now;
        }
        f.icons.globalX = 223f; near(distance(f), 0f, "The copies end where the real views are: nothing is left to jump");
        f.restingGap(-12f); f.icons.globalX = 223f;
        near(distance(f), 0f, "Overlap that the real layout already has is not removed from the copies");
        f.noPropertyWrites("Resting gap"); f.disposed();
    }

    private static void scopedNativeDrawing() {
        Fixture f = new Fixture(); f.bind(); ScopedCanvas canvas = new ScopedCanvas();
        canvas.translate(3f, 5f); canvas.scale(.7f, .6f);
        int saves = canvas.getSaveCount();
        StatusBarLeftCopySpacing.DrawScope scope = f.owner.beginDraw(f.icons, canvas);
        check(scope != null, "Adjacent copies open one scoped drawing correction");
        near(canvas.translateX, 19f, "Correction composes with an existing Canvas translation");
        near(canvas.translateY, 5f, "Correction preserves native Canvas Y");
        int nativeSave = canvas.save(); canvas.translate(30f, 0f);
        near(canvas.translateX, 49f, "Native NotificationFakeFrame still applies its own 30px");
        canvas.restoreToCount(nativeSave); scope.close();
        near(canvas.translateX, 3f, "Scope close restores the preexisting native Canvas X");
        near(canvas.translateY, 5f, "Scope close restores native Canvas Y");
        near(canvas.scaleX, .7f, "Scope retains Canvas scale X");
        near(canvas.scaleY, .6f, "Scope retains Canvas scale Y");
        check(canvas.getSaveCount() == saves, "Scope restores the original save count");
        int restores = canvas.restores; scope.close();
        check(canvas.restores == restores, "Repeated close performs no second restore");
        try (StatusBarLeftCopySpacing.DrawScope ignored = f.owner.beginDraw(f.icons, canvas)) {
            throw new IllegalStateException("native source draw failed");
        } catch (IllegalStateException expected) {
            check("native source draw failed".equals(expected.getMessage()), "The native draw failure propagates intact");
        }
        near(canvas.translateX, 3f, "An exceptional native draw also restores the prior Canvas X");
        check(canvas.getSaveCount() == saves, "An exceptional native draw balances the save count");
        check(f.owner.beginDraw(f.icons, null) == null, "A missing Canvas preserves the native draw");
        f.noPropertyWrites("Scoped drawing"); f.disposed();
    }

    private static void transparentCases() {
        Fixture f = new Fixture(); f.bind();
        f.clock.setAlpha(0f); near(distance(f), 16f, "A transparent clock prepares the same projected gap before its first visible frame");
        f.clock.setAlpha(.8f); f.icons.setTransitionAlpha(0f);
        near(distance(f), 16f, "Notification fade-in retains the same X correction");
        f.icons.setTransitionAlpha(.9f); f.row.setAlpha(0f);
        near(distance(f), 16f, "Native row opacity never changes horizontal spacing");
        f.row.setAlpha(1f); f.clockContainer.setVisibility(View.INVISIBLE);
        near(distance(f), 16f, "Native invisible preflight prepares the same first-frame projected gap");
        f.clockContainer.setVisibility(View.VISIBLE); f.icons.globalY = 100f;
        near(distance(f), 16f, "A vertical spring never switches horizontal spacing at an overlap threshold");
        f.clockContainer.setVisibility(View.GONE);
        near(distance(f), 0f, "Gone geometry remains outside the spacing owner");
        f.clockContainer.setVisibility(View.VISIBLE);
        for (int frame = 0; frame <= 100; frame++) {
            f.clock.setAlpha(frame / 100f); f.icons.setTransitionAlpha(frame / 100f);
            f.icons.globalY = 120f - frame * 1.2f;
            near(distance(f), 16f, "Fade and Y spring share a continuous projected X budget");
        }
        f.icons.globalY = 0f; f.timeSource.setAlpha(0f); f.iconSource.setVisibility(View.INVISIBLE);
        near(distance(f), 16f, "Phone source visibility does not suppress a directly drawn native fake");
        f.timeSource.width = 0; near(distance(f), 0f, "Unknown clock width does not invent an ink budget");
        f.timeSource.width = 182; f.iconSource.width = 0;
        near(distance(f), 0f, "Unknown notification source width retains native drawing");
        f.iconSource.width = 119; f.clockOffset = Float.NaN;
        near(distance(f), 0f, "Nonfinite clock draw offsets retain native drawing");
        f.clockOffset = 0f;
        for (Node fake : new Node[]{f.clock, f.icons}) {
            fake.skewX = .1f; near(distance(f), 0f, "Unverified X skew retains native drawing"); fake.skewX = 0f;
            fake.skewY = .1f; near(distance(f), 0f, "Unverified Y skew retains native drawing"); fake.skewY = 0f;
            fake.perspectiveX = .01f; near(distance(f), 0f, "Unverified perspective retains native drawing"); fake.perspectiveX = 0f;
            fake.globalX = Float.NaN; near(distance(f), 0f, "Nonfinite fake transforms retain native drawing");
            fake.globalX = fake == f.clock ? 71f : 223f;
            fake.globalScaleX = 0f; near(distance(f), 0f, "Collapsed X transforms retain native drawing"); fake.globalScaleX = 1f;
        }
        f.owner.observe(f.icons, f.iconSource, StatusIconTransition.NOTIFICATIONS, Float.NaN);
        near(distance(f), 0f, "Unknown native notification offset releases the correction");
        check(f.row.getViewTreeObserver().preDrawListeners.isEmpty(), "An unknown native offset releases its listener");
        f.disposed();
        near(StatusBarLeftCopySpacing.correction(new RectF(0f, 0f, 10f, 10f),
                new RectF(10f, 0f, 20f, 10f), false, Float.NaN), 0f, "Nonfinite requested gap remains transparent");
    }

    private static void cacheChangesOnly() {
        Fixture f = new Fixture(); f.bind();
        check(f.row.getViewTreeObserver().preDrawListeners.size() == 1, "One exact native row receives one cache observer");
        near(distance(f), 16f, "Initial drawing records the current correction");
        int invalidations = f.icons.invalidations;
        for (int i = 0; i < 500; i++) predraw(f.row);
        check(f.icons.invalidations == invalidations, "A steady native spring never re-records the display list");
        f.icons.globalX = 238f; predraw(f.row);
        check(f.icons.invalidations == invalidations + 1, "A changed gap invalidates the exact notification fake once");
        for (int i = 0; i < 500; i++) predraw(f.row);
        check(f.icons.invalidations == invalidations + 1, "A changed gap is remembered before any new drawing occurs");
        near(distance(f), 1f, "The new display list uses the current native geometry");
        f.icons.globalX = 260f; predraw(f.row);
        check(f.icons.invalidations == invalidations + 2, "Return to sufficient native spacing re-records the unshifted list");
        near(distance(f), 0f, "An unshifted native draw clears the previously recorded correction");
        for (int i = 0; i < 100; i++) { f.bind(); predraw(f.row); }
        check(f.row.getViewTreeObserver().preDrawListeners.size() == 1 && f.icons.invalidations == invalidations + 2,
                "Repeated binding never duplicates cache observers or redraws");
        check(f.clock.invalidations == 0 && f.timeSource.invalidations == 0 && f.iconSource.invalidations == 0
                && f.row.invalidations == 0 && f.clockContainer.invalidations == 0 && f.iconContainer.invalidations == 0,
                "Only the inner notification fake is invalidated");
        f.noPropertyWrites("Pre-draw cache observer"); f.disposed();
    }

    private static void bindingAndLifecycle() {
        Fixture f = new Fixture(); f.bind();
        Node replacement = new Node(220); f.phoneRoot.addView(replacement);
        f.owner.observe(f.clock, replacement, StatusIconTransition.CLOCK, 0f);
        check(f.timeSource.attachListeners.isEmpty(), "Clock rebinding releases the old native source");
        check(replacement.attachListeners.size() == 1 && f.row.getViewTreeObserver().preDrawListeners.size() == 1,
                "Clock rebinding installs exactly one source and row listener");
        near(distance(f), 54f, "Clock rebinding immediately adopts the new native ink width");
        f.timeSource.width = 600;
        near(distance(f), 54f, "An old source can never change the current row's budget");
        Node iconReplacement = new Node(200); f.phoneRoot.addView(iconReplacement);
        f.owner.observe(f.icons, iconReplacement, StatusIconTransition.NOTIFICATIONS, 30f);
        check(f.iconSource.attachListeners.isEmpty() && iconReplacement.attachListeners.size() == 1,
                "Notification rebinding releases its old source listener");
        f.owner.observe(f.icons, null, StatusIconTransition.NOTIFICATIONS, 30f);
        check(f.row.getViewTreeObserver().preDrawListeners.isEmpty() && iconReplacement.attachListeners.isEmpty(),
                "Native unbind releases drawing and its exact source listener");
        near(distance(f), 0f, "An unbound fake is immediately unshifted");
        f.owner.observe(f.icons, iconReplacement, StatusIconTransition.NOTIFICATIONS, 30f);
        check(f.row.getViewTreeObserver().preDrawListeners.size() == 1, "Rebinding after native unbind restores one observer");
        f.owner.configure(false);
        check(f.row.getViewTreeObserver().preDrawListeners.isEmpty() && replacement.attachListeners.isEmpty()
                && iconReplacement.attachListeners.isEmpty() && f.clock.attachListeners.isEmpty() && f.icons.attachListeners.isEmpty(),
                "Disable/safe/close releases both fake bindings and native sources");
        near(distance(f), 0f, "Disable restores native drawing immediately");
        int invalidations = f.icons.invalidations; f.owner.configure(false); f.owner.clear();
        check(f.icons.invalidations == invalidations, "Repeated close/clear performs no extra redraw");
        f.restingGap(40f); f.owner.configure(true); f.bind(); near(distance(f), 434f, "A later native bind starts from current source dimensions");
        f.disposed();

        for (int target = 0; target < 4; target++) {
            Fixture detached = new Fixture(); detached.bind();
            Node victim = detached.nodes()[target]; detach(victim);
            check(detached.row.getViewTreeObserver().preDrawListeners.isEmpty(), "Fake/source detach immediately releases the cache observer");
            check(victim.attachListeners.isEmpty(), "Detached binding releases its attach listener");
            near(distance(detached), 0f, "Fake/source detach immediately restores native draw");
            detached.disposed();
        }
        Fixture rowDetached = new Fixture(); rowDetached.bind(); detach(rowDetached.row);
        check(rowDetached.row.getViewTreeObserver().preDrawListeners.isEmpty() && rowDetached.row.attachListeners.isEmpty(),
                "Native row detach immediately removes its cache observers");
        near(distance(rowDetached), 0f, "Detached rows retain no drawing correction"); rowDetached.disposed();
    }

    private static void exactRowAndWindow() {
        Fixture f = new Fixture();
        ViewGroup otherRow = new ViewGroup(new Context()), otherContainer = new ViewGroup(new Context());
        f.row.removeView(f.iconContainer); otherRow.addView(f.iconContainer); otherRow.addView(otherContainer);
        f.bind(); near(distance(f), 0f, "Native fake frames in different rows never pair");
        check(f.row.getViewTreeObserver().preDrawListeners.isEmpty() && otherRow.getViewTreeObserver().preDrawListeners.isEmpty(),
                "Unmatched rows receive no cache observer");
        otherRow.removeView(f.iconContainer); f.row.addView(f.iconContainer); f.bind();
        near(distance(f), 16f, "Exact sibling containers under one native row can pair");
        ViewGroup otherPhoneRoot = new ViewGroup(new Context());
        f.phoneRoot.removeView(f.iconSource); otherPhoneRoot.addView(f.iconSource);
        near(distance(f), 0f, "Sources from different native windows never share a correction");
        otherPhoneRoot.removeView(f.iconSource); f.phoneRoot.addView(f.iconSource);
        near(distance(f), 16f, "The two real sources in the same Phone root can draw again");
        f.row.removeView(f.iconContainer); otherRow.addView(f.iconContainer); predraw(f.row);
        check(f.row.getViewTreeObserver().preDrawListeners.isEmpty(), "A moved fake frame immediately releases the old row observer");
        near(distance(f), 0f, "A moved fake frame never uses an old row's clock");
        f.disposed();
    }

    private static void notificationFirstBinding() {
        Fixture f = new Fixture();
        f.clock.setAlpha(0f); f.icons.setTransitionAlpha(0f);
        f.owner.observe(f.icons, f.iconSource, StatusIconTransition.NOTIFICATIONS, 30f);
        check(!f.owner.paired(f.icons), "A notification-first binding waits for the exact clock source");
        // The verified native sibling is bound before dispatch, even if the clock has not drawn yet.
        f.owner.observe(f.clock, f.timeSource, StatusIconTransition.CLOCK, 0f);
        check(f.owner.paired(f.icons), "A sibling clock binding pairs before either copy draws");
        near(distance(f), 16f, "The first transparent notification recording already has its final gap");
        f.clock.setAlpha(.01f); f.icons.setTransitionAlpha(.01f);
        near(distance(f), 16f, "The first visible display list has no registration/alpha jump");
        f.disposed();
    }

    private static void mixedClockSources() {
        Fixture f = new Fixture();
        ViewGroup secondPhone = new ViewGroup(new Context()), secondContainer = new ViewGroup(new Context());
        Node secondClock = new Node(300), secondSource = new Node(300);
        secondPhone.addView(secondSource); f.row.addView(secondContainer); secondContainer.addView(secondClock);
        f.owner.observe(secondClock, secondSource, StatusIconTransition.CLOCK, 0f);
        f.bind();
        near(distance(f), 16f, "A clock from another source window cannot occupy the valid pair");
        f.disposed();
        check(secondClock.attachListeners.isEmpty() && secondSource.attachListeners.isEmpty(),
                "All duplicate-clock source listeners are released");
    }

    public static void main(String[] args) {
        nativeOffsetsAndScreenGap(); resting(); scopedNativeDrawing(); transparentCases();
        cacheChangesOnly(); bindingAndLifecycle(); exactRowAndWindow(); notificationFirstBinding(); mixedClockSources();
        System.out.println("Native left-copy spacing checks passed: " + checks);
    }
}
