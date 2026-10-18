package dev.puitheme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;

/** Production background composition keeps native texture/alpha and never takes foreground ownership. */
public final class AodNotificationBackgroundCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError("AodGlass " + checks + ": " + why); }
    private static void equal(int expected, int actual, String why) { check(expected == actual, why + " expected=" + Integer.toHexString(expected) + " actual=" + Integer.toHexString(actual)); }
    private static void near(float expected, float actual, String why) { check(Math.abs(expected - actual) < .001f, why); }
    private static Bundle settings(boolean on) { Bundle value = new Bundle(); value.putBoolean(AodNotificationBackground.ENABLED, on); return value; }
    private static final class Row extends ExpandableNotificationRow {
        int childReads;
        Row() { super(new Context(),100,0f,0); }
        @Override public int getChildCount() { childReads++; return super.getChildCount(); }
    }
    private static final class Card {
        final Row row = new Row(); final NotificationBackgroundView body = new NotificationBackgroundView();
        final NotificationBackgroundViewExtImp extension = (NotificationBackgroundViewExtImp)body.mExt;
        final TextView title = new TextView(new Context()); final ImageView icon = new ImageView(new Context());
        Card() { row.addView(body);row.addView(title);row.addView(icon);body.mOnKeyguard=true; }
    }
    private static final class Icon extends Drawable {
        int draws,level;
        public void draw(Canvas canvas) { draws++;level=canvas.getSaveCount(); }
        public void setAlpha(int value) { }
        public void setColorFilter(ColorFilter value) { }
        public int getOpacity() { return -3; }
    }
    public static void main(String[] args) throws Throwable {
        defaultsAndScenes(); glassPreservesNativePixels(); nativeDrawAndIcons(); foregroundUntouched(); actualHostsAndMedia(); lifecycle();
        System.out.println("AodNotificationBackgroundCheck passed " + checks + " checks");
    }
    private static void defaultsAndScenes() throws Throwable {
        check(Boolean.FALSE.equals(LockNotificationSettings.BOOLEANS.get(AodNotificationBackground.ENABLED)),"compatible setting still defaults off");
        check("lock_aod".equals(SettingsCatalog.item(AodNotificationBackground.ENABLED).groupId),"glass switch stays on lock AOD page");
        check("".equals(SettingsCatalog.unavailableReason(AodNotificationBackground.ENABLED,new java.util.LinkedHashMap<String,Object>())),"independent from other material masters");
        AodNotificationBackground helper=new AodNotificationBackground();Card card=new Card();Canvas canvas=new Canvas();helper.observeBody(card.body);helper.scene(true,true);
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(0,canvas.layers,"default does not add glass");
        helper.configure(settings(true));helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"enabled AOD adds glass");
        helper.scene(true,false);helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"lit lock remains native");
        helper.scene(false,true);helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"dozing alone cannot affect non-keyguard surfaces");
        helper.scene(true,true);helper.configure(settings(false));helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"disable immediately uses original background");
        Bundle safe=settings(true);safe.putBoolean(StatusBarSettings.SAFE_MODE,true);helper.configure(safe);helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"safe mode keeps native background");
        helper.release();
    }
    private static float channel(float[] matrix,int row,int red,int green,int blue,int alpha) {
        int start=row*5;return matrix[start]*red+matrix[start+1]*green+matrix[start+2]*blue+matrix[start+3]*alpha+matrix[start+4];
    }
    private static void glassPreservesNativePixels() throws Throwable {
        AodNotificationBackground helper=new AodNotificationBackground();Card card=new Card();Canvas canvas=new Canvas();helper.configure(settings(true));helper.scene(true,true);
        helper.drawNotificationBackground(card.body,canvas,c -> null);
        float[] matrix=((ColorMatrixColorFilter)canvas.lastLayerPaint.getColorFilter()).matrix.values;
        check(matrix[0]>.8f&&matrix[0]<1f,"light veil retains most native red texture instead of replacing it");
        near(matrix[0],matrix[6],"green texture uses same slope");near(matrix[0],matrix[12],"blue texture uses same slope");
        check(matrix[4]>0&&matrix[4]<40,"white contribution is slight rather than an opaque white base");
        for(int alpha=0;alpha<=255;alpha++) {
            equal(alpha,Math.round(channel(matrix,3,35,103,204,alpha)),"every native alpha is exact including transparent holes");
            near(0,channel(matrix,3,255,255,255,0),"white tint cannot fill a transparent pixel");
        }
        for(int input=0;input<=255;input++)for(int row=0;row<3;row++) {
            float output=channel(matrix,row,input,input,input,143);
            near(input*.88f+255*.12f,output,"native colour contributes 88 percent of glass output");
            check(output>=input&&output<=255,"glass gently lightens without clipping native highlights");
        }
        float dark=channel(matrix,1,22,61,35,180),light=channel(matrix,1,65,179,91,180);
        near((179-61)*.88f,light-dark,"wallpaper blur contrast and texture survive with proportional differences");
        check(light>dark+80,"two native green wallpaper patches remain visibly different");
        float red=channel(matrix,0,35,103,64,180),green=channel(matrix,1,35,103,64,180);
        check(green>red+50,"native green wallpaper colour still passes through the white glass");
        equal(255,canvas.lastLayerPaint.getAlpha(),"layer paint never promotes native alpha");
        equal(0,canvas.rects+canvas.rectFills+canvas.colorFills,"no flat white fill hides wallpaper");
        helper.release();
    }
    private static void nativeDrawAndIcons() throws Throwable {
        AodNotificationBackground helper=new AodNotificationBackground();Card card=new Card();Icon icon=new Icon();card.extension.icon(icon);
        helper.configure(settings(true));helper.scene(true,true);Canvas canvas=new Canvas();final int[] calls={0};
        Object result=helper.drawNotificationBackgroundExtension(card.extension,canvas,c -> {
            calls[0]++;equal(2,c.getSaveCount(),"native blur/template/stacked/AOD background draws inside glass layer");
            check(helper.deferNotificationIcon(card.extension,c),"app icon deferred outside glass layer");
            return helper.drawNotificationBackground(card.body,c,nested -> { calls[0]++;return "native blur/wallpaper/stack/AOD"; });
        });
        check("native blur/wallpaper/stack/AOD".equals(result),"original native draw path/result retained");equal(2,calls[0],"native callbacks each execute once");
        equal(1,canvas.layers,"fallback does not double tint");equal(1,icon.draws,"original app icon drawn once");equal(1,icon.level,"original app icon stays outside material tint");
        equal(1,canvas.getSaveCount(),"canvas restored before content dispatch");
        int reads=card.row.childReads,posts=Handler.postedForCheck.size();Object paint=canvas.lastLayerPaint;
        for(int n=0;n<100;n++) { helper.observeExtension(card.extension);helper.drawNotificationBackgroundExtension(card.extension,canvas,c -> null); }
        equal(reads,card.row.childReads,"stable frames never inspect foreground/content children");equal(posts,Handler.postedForCheck.size(),"no background polling or deferred content scans");check(paint==canvas.lastLayerPaint,"glass paint/filter reused");
        try { helper.drawNotificationBackground(card.body,canvas,c -> { throw new IllegalStateException("native failure"); });throw new AssertionError("missing failure"); }
        catch(IllegalStateException expected) { equal(1,canvas.getSaveCount(),"native failure restores layer"); }
        check(!helper.deferNotificationIcon(card.extension,canvas),"failed frame leaves no icon deferral state");
        int layers=canvas.layers;canvas.failLayerOnce=true;helper.drawNotificationBackground(card.body,canvas,c -> { equal(1,c.getSaveCount(),"layer failure safely uses original draw");return null; });equal(layers,canvas.layers,"no substitution when layer unavailable");
        helper.release();
    }
    private static void foregroundUntouched() throws Throwable {
        AodNotificationBackground helper=new AodNotificationBackground();Card card=new Card();
        ColorStateList text=new ColorStateList(new int[][]{{1},{}},new int[]{0x51ffffff,0xcceeeeee});
        ColorStateList image=new ColorStateList(new int[][]{{1},{}},new int[]{0x92ffffff,0xff2ca155});card.title.setTextColor(text);card.icon.setImageTintList(image);
        helper.observeBody(card.body);helper.configure(settings(true));helper.scene(true,true);Canvas canvas=new Canvas();
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"native selectors do not block transparent glass");
        check(card.title.getTextColors()==text,"native text selector object and every state stay unchanged");check(card.icon.getImageTintList()==image,"native image selector stays unchanged");
        card.title.setTextColor(0x38ffffff);card.icon.setImageTintList(ColorStateList.valueOf(0x67ffffff));
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(0x38ffffff,card.title.getCurrentTextColor(),"native dynamic white text and alpha stay untouched");equal(0x67ffffff,card.icon.getImageTintList().getDefaultColor(),"native dynamic monochrome icon stays untouched");
        for(int i=0;i<250;i++)card.row.addView(new View(new Context()));SurfaceView external=new SurfaceView();card.row.addView(external);
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(3,canvas.layers,"large or external content never requires scanning or recolouring for glass background");
        int reads=card.row.childReads;helper.scene(true,false);helper.configure(settings(false));helper.release();equal(reads,card.row.childReads,"turning off also never scans content");
        equal(0x38ffffff,card.title.getCurrentTextColor(),"wake/disable/release never replace latest native foreground");
    }
    private static void actualHostsAndMedia() throws Throwable {
        AodNotificationBackground helper=new AodNotificationBackground();helper.configure(settings(true));helper.scene(true,true);Canvas canvas=new Canvas();
        OplusCustomRow row=new OplusCustomRow();NotificationBackgroundView body=new NotificationBackgroundView();row.addView(body);body.mOnKeyguard=true;
        CustomRowEntry entry=new CustomRowEntry();row.setEntry(entry);TextView cached=new TextView(new Context());entry.customView=cached;
        helper.observeBody(body);helper.drawNotificationBackground(body,canvas,c -> null);equal(1,canvas.layers,"real custom/media notification background is supported");equal(0xccffffff,cached.getCurrentTextColor(),"cached content is never touched");
        row.removeView(body);ViewGroup floating=new ViewGroup(new Context());floating.addView(body);helper.drawNotificationBackground(body,canvas,c -> null);equal(1,canvas.layers,"moved body has no real notification row owner");
        floating.removeView(body);row.addView(body);helper.observeBody(body);helper.drawNotificationBackground(body,canvas,c -> null);equal(2,canvas.layers,"body returned to real row recovers");
        helper.drawNotificationBackground(new View(new Context()),canvas,c -> null);equal(2,canvas.layers,"arbitrary app view is never treated as card background");
        com.oplus.systemui.media.controls.ui.OplusMediaCarouselView media=new com.oplus.systemui.media.controls.ui.OplusMediaCarouselView();
        com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable repository=(com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable)media.oplusPanoramicAodRepository;
        TextView title=new TextView(new Context());media.addView(title);helper.observeMedia(media);helper.drawMediaBackground(media,canvas,c -> null);equal(2,canvas.layers,"standard media requires native AOD progress");
        repository.progressForCheck(.75f);helper.observeMedia(media);helper.drawMediaBackground(media,canvas,c -> null);equal(3,canvas.layers,"standard media onDraw glass supports native AOD card");equal(0xccffffff,title.getCurrentTextColor(),"standard media native white foreground is retained");
        repository.progressForCheck(Float.NaN);helper.drawMediaBackground(media,canvas,c -> null);equal(3,canvas.layers,"unknown media progress retains native");
        repository.progressForCheck(1f);helper.scene(true,false);helper.drawMediaBackground(media,canvas,c -> null);equal(3,canvas.layers,"lit media is never tinted despite unwinding progress");helper.release();
    }
    private static void lifecycle() throws Throwable {
        AodNotificationBackground helper=new AodNotificationBackground();Card card=new Card();card.body.mOnKeyguard=false;helper.configure(settings(true));helper.observeBody(card.body);
        com.android.systemui.statusbar.StatusBarStateControllerImpl state=new com.android.systemui.statusbar.StatusBarStateControllerImpl();state.state=1;state.dozing=true;helper.onNativeState(state);Canvas canvas=new Canvas();
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(0,canvas.layers,"early bind before body keyguard flag stays native");
        card.body.mOnKeyguard=true;helper.observeBody(card.body);helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"native setOnKeyguard prepares late body without scans");
        com.android.systemui.statusbar.policy.KeyguardStateControllerImpl keyguard=new com.android.systemui.statusbar.policy.KeyguardStateControllerImpl();keyguard.showingForCheck(false);helper.onNativeKeyguard(keyguard);helper.onNativeState(state);
        helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"confirmed native unlock wins over stale status state");
        keyguard.showingForCheck(true);helper.onNativeKeyguard(keyguard);helper.detach(card.row);card.body.attached=false;helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"detached native body is not tinted");
        helper.release();card.body.attached=true;helper.drawNotificationBackground(card.body,canvas,c -> null);equal(1,canvas.layers,"released helper uses native draw");
    }
}
