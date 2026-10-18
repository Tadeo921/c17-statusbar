package dev.puitheme;

import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Handler;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;

/** Surface composition contracts use synthetic layer identities, never private user data. */
public final class NotificationRemoteClockEdgeCheck {
    private static int checks;
    private static void require(boolean value,String detail){checks++;if(!value)throw new AssertionError(detail);}
    private static final class Root extends View {
        Root(){super(null);}
        @Override public void setRenderEffect(RenderEffect value){super.setRenderEffect(value);NotificationRemoteClockEdge.nativeEffect(this,value);}
    }
    private static final class Setup {
        final ViewGroup stack=new ViewGroup(null);
        final OplusCustomRow row=new OplusCustomRow();
        final SurfaceView surface=new SurfaceView();
        final Root root=new Root();
        final Object host=new Object();
        Setup(boolean link) {
            CustomRowEntry entry=new CustomRowEntry();entry.miniCardView=surface;row.setEntry(entry);
            stack.addView(row);surface.parent=row;surface.setTranslationY(40f);
            if(link){SurfaceControlViewHost.SurfacePackage pack=new SurfaceControlViewHost.SurfacePackage(new SurfaceControl("live-alert",null));
                FluidCloudSurfaceRoots.hostView(host,root);FluidCloudSurfaceRoots.hostPackage(host,pack);
                FluidCloudSurfaceRoots.childPackage(surface,pack);}
        }
        void update(boolean active){NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,active);Handler.drainForCheck();}
        void close(){NotificationRemoteClockEdge.releaseStack(stack);FluidCloudSurfaceRoots.releaseHost(host);}
    }
    public static void main(String[] args) {
        Setup notTracked=new Setup(true);notTracked.update(true);
        require(notTracked.root.renderEffect==null,"unknown original effect must remain native without early tracking");notTracked.close();
        NotificationRemoteClockEdge.trackingReady();
        Setup local=new Setup(true);
        RenderEffect original=RenderEffect.createBlurEffect(1f,1f,Shader.TileMode.CLAMP);
        local.root.setRenderEffect(original);local.root.setAlpha(.7f);local.surface.setAlpha(.8f);
        local.update(true);
        RenderEffect effect=local.root.renderEffect;
        require(effect!=null&&effect!=original,"local mapped Surface root receives content effect");
        require(effect.inner==original,"native effect remains the input of our local effect");
        require(effect.outer!=null&&effect.outer.shader!=null,"contents use runtime shader rather than background blur");
        RuntimeShader shader=effect.outer.shader;
        require(effect.outer.inputName.equals("content"),"runtime input is remote card content");
        float[] edge=shader.floats.get("edge");
        require(edge[0]==10f&&edge[1]==10f&&edge[2]==50f,"stack bounds converted into remote content coordinates");
        require(shader.floats.get("radius")[0]==8f,"native density radius applied exactly once");
        require(shader.source.contains("if(p.y>=edge.z)return sharp"),"clear part of card bypasses all blur samples");
        require(shader.source.contains("if(p.y<edge.x)return half4(0)"),"pixels hidden behind header are clipped in content");
        require(local.root.getAlpha()==.7f&&local.surface.getAlpha()==.8f,"whole layer alpha never altered");
        int writes=local.root.renderEffectWrites,uploads=shader.uploadCount;
        local.update(true);
        require(local.root.renderEffect==effect&&local.root.renderEffectWrites==writes,"unchanged geometry reuses effect without setter writes");
        require(shader.uploadCount==uploads,"unchanged geometry does not upload uniforms");
        local.surface.setTranslationY(45f);local.update(true);
        require(local.root.renderEffect==effect,"scroll updates cached shader, no shader recreation");
        require(shader.floats.get("edge")[0]==5f,"new row offset tracks spatial boundary");
        local.surface.setTranslationY(100f);local.update(true);
        require(local.root.renderEffect==original,"clear card restores original native effect");
        local.surface.setTranslationY(40f);local.update(true);
        require(local.root.renderEffect!=original,"return into edge reattaches bounded effect");
        RenderEffect replacement=RenderEffect.createBlurEffect(2f,2f,Shader.TileMode.CLAMP);
        local.root.setRenderEffect(replacement);local.update(true);
        require(local.root.renderEffect.inner==replacement,"native writer replaces baseline instead of being overwritten with stale effect");
        local.update(false);
        require(local.root.renderEffect==replacement,"horizontal paging or feature off restores latest native baseline");
        local.update(true);local.stack.removeView(local.row);local.update(true);
        require(local.root.renderEffect==replacement,"despawn releases content effect");
        local.stack.addView(local.row);local.update(true);NotificationClockEdge.detach(local.stack);
        require(local.root.renderEffect==replacement,"stack detach releases native root effect");
        local.update(true);NotificationClockEdge.releaseResources();
        require(local.root.renderEffect==replacement,"global feature resource release restores root");
        local.update(true);NotificationRemoteClockEdge.release(local.root);
        require(local.root.renderEffect==replacement,"native Surface host release restores content before destruction");local.close();
        Setup unknown=new Setup(false);unknown.update(true);
        require(unknown.root.renderEffectWrites==0,"unknown cross-process Surface must remain native");unknown.close();
        Setup plain=new Setup(false);CustomRowEntry entry=plain.row.getEntry();entry.miniCardView=plain.root;plain.update(true);
        require(plain.root.renderEffectWrites==0,"plain notification view is not blurred twice");plain.close();
        Setup failed=new Setup(true);failed.root.setRenderEffect(original);RenderEffect.fail=true;failed.update(true);RenderEffect.fail=false;
        require(failed.root.renderEffect==original,"graphics failure preserves native baseline");
        writes=failed.root.renderEffectWrites;failed.update(true);
        require(failed.root.renderEffectWrites==writes,"failed shader does not retry compilation every frame");failed.close();
        Setup coalesced=new Setup(true);
        NotificationRemoteClockEdge.update(coalesced.stack,50f,50f,90f,8f,true);
        NotificationRemoteClockEdge.update(coalesced.stack,50f,50f,100f,8f,true);
        require(coalesced.root.renderEffectWrites==0,"draw submits no immediate ViewRoot writes");
        require(Handler.postedForCheck.size()==1,"successive draw requests share one main-loop job");
        Handler.drainForCheck();
        require(coalesced.root.renderEffect.shader.floats.get("edge")[2]==60f,"coalesced task uses newest geometry");
        coalesced.update(false);
        NotificationRemoteClockEdge.update(coalesced.stack,50f,50f,90f,8f,true);
        writes=coalesced.root.renderEffectWrites;coalesced.close();Handler.drainForCheck();
        require(coalesced.root.renderEffectWrites==writes,"detach cancels queued material writes");
        Setup swapped=new Setup(true);ViewGroup card=new ViewGroup(null);card.addView(swapped.surface);
        swapped.row.getEntry().miniCardView=card;swapped.update(true);
        require(swapped.root.renderEffect!=null,"nested Surface content is associated once");
        card.removeView(swapped.surface);SurfaceView replacementSurface=new SurfaceView();card.addView(replacementSurface);
        replacementSurface.setTranslationY(40f);Root replacementRoot=new Root();Object replacementHost=new Object();
        SurfaceControlViewHost.SurfacePackage replacementPackage=new SurfaceControlViewHost.SurfacePackage(new SurfaceControl("next-live-alert",null));
        FluidCloudSurfaceRoots.hostView(replacementHost,replacementRoot);FluidCloudSurfaceRoots.hostPackage(replacementHost,replacementPackage);
        FluidCloudSurfaceRoots.childPackage(replacementSurface,replacementPackage);swapped.update(true);
        require(swapped.root.renderEffect==null&&replacementRoot.renderEffect!=null,
                "equal child-count replacement revokes old cached Surface and binds the actual new one");
        swapped.close();FluidCloudSurfaceRoots.releaseHost(replacementHost);
        System.out.println("Notification remote edge checks passed: "+checks);
    }
}
