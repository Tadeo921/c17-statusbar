package dev.puitheme;

import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.FrameLayout;
import com.android.systemui.scrim.ScrimView;
import com.oplus.posteffect.BlurDrawable;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable;
import com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper;

/** Proven native source fixture shared only by clock material checks. */
final class ClockBackdropCheckScene implements AutoCloseable {
    final FrameLayout root=new FrameLayout(null);
    final ScrimView host=new ScrimView();
    final PlatformBlurDrawable wrapper=new PlatformBlurDrawable();
    final BlurDrawable engine=wrapper.blurDrawable;
    final Bitmap bitmap=Bitmap.createBitmap(new int[]{0xff2478d4,0xffffffff,0xfff48a24},3,1,Bitmap.Config.ARGB_8888);
    final ClockBackdropSource.Frame held;
    ClockBackdropCheckScene() {
        ClockBackdropSource.clear();Handler.postedForCheck.clear();
        host.setId(903);host.getResources().entryNames.put(903,"scrim_behind");root.addView(host);
        engine.params.radius=0;engine.setBounds(0,0,300,200);
        try(ClockBackdropSource.Scope scope=ClockBackdropSource.beginHelper(new PlatformBlurHelper(host))){ClockBackdropSource.created(wrapper);}
        engine.params.radius=40;ClockBackdropSource.onParams(engine,engine.params);ClockBackdropSource.onRegistration(engine);engine.addBlurDrawable();ClockBackdropSource.onRegistration(engine);
        engine.completeForCheck(bitmap);engine.preDrawContent();ClockBackdropSource.onDrawn(engine);
        held=ClockBackdropSource.snapshot(root);
        if(held==null)throw new AssertionError("real proven clock backdrop fixture unavailable");
    }
    @Override public void close(){ClockBackdropSource.clear();Handler.postedForCheck.clear();}
}
