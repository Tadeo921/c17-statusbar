package android.view;
/** Native in-process SurfacePackage owner contract, with copied layer handles. */
public class SurfaceControlViewHost {
    public static final java.util.List<SurfaceControlViewHost> hostsForCheck=new java.util.ArrayList<>();
    public static boolean failForCheck;
    public int releasesForCheck;
    private SurfaceControl ownRoot;
    public WindowManager.LayoutParams layoutForCheck;
    public SurfaceControlViewHost(){}
    public SurfaceControlViewHost(android.content.Context context,Display display,android.window.InputTransferToken token,String name){
        if(failForCheck)throw new IllegalStateException("host unavailable");
        ownRoot=SurfaceControl.ownedHostForCheck(name);hostsForCheck.add(this);
    }
    public static final class SurfacePackage {
        private final SurfaceControl control;
        public int releasesForCheck;
        public SurfacePackage(SurfaceControl control){this.control=control;}
        public SurfaceControl getSurfaceControl(){return control;}
        public void release(){releasesForCheck++;control.release();}
    }
    private View view;
    public void setView(View view,int width,int height){this.view=view;}
    public void setView(View view,WindowManager.LayoutParams layout){this.view=view;layoutForCheck=layout;}
    public View getView(){return view;}
    public SurfacePackage getSurfacePackage(){if(ownRoot==null)return null;SurfaceControl copy=SurfaceControl.ownedHostForCheck("copied wallpaper host handle");copy.sameLayerForCheck=ownRoot;return new SurfacePackage(copy);}
    public void release(){releasesForCheck++;view=null;if(ownRoot!=null)ownRoot.release();}
}
