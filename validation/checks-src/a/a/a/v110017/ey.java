package a.a.a.v110017;
/** Exact native u(Drawable) delivery and existing ImageView.mDrawable storage. */
public final class ey extends android.widget.ImageView {
    public int getterCalls;
    public ey(){super(new android.content.Context());}
    public void u(android.graphics.drawable.Drawable image){setImageDrawable(image);}
    @Override public android.graphics.drawable.Drawable getDrawable(){getterCalls++;throw new AssertionError("native getter can rasterize; inspect existing mDrawable");}
}
