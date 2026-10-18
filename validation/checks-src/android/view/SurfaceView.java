package android.view;
public class SurfaceView extends View {
    public SurfaceControlViewHost.SurfacePackage childPackage;
    public SurfaceView(){super(new android.content.Context());}
    public void setChildSurfacePackage(SurfaceControlViewHost.SurfacePackage value){childPackage=value;}
}
