package com.oplus.posteffect.image;
/** Existing native bitmap ownership. No copy or borrowed bitmap release. */
public final class ContentImage {
    private final android.graphics.Bitmap bitmap;
    public ContentImage(android.graphics.Bitmap bitmap){this.bitmap=bitmap;}
    public android.graphics.Bitmap get(){return bitmap;}
}
