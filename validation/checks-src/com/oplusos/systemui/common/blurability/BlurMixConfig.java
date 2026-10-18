package com.oplusos.systemui.common.blurability;
public class BlurMixConfig {
    public static final class BlurMixSingle extends BlurMixConfig {
        private final MixColor mixColor;
        public BlurMixSingle(Object recipe){this(recipe,new MixColor(2,0xb3445566,0x40223344));}
        public BlurMixSingle(MixColor color){this(null,color);}
        private BlurMixSingle(Object recipe,MixColor color){super(recipe);mixColor=color;}
        public MixColor getMixColor(){return mixColor;}
        public BlurMixSingle copy(MixColor color){return new BlurMixSingle(recipe,color);}
    }
    public static final class BlurMixMulti extends BlurMixConfig {
        private final MixColor foregroundMixColor,backgroundMixColor;
        public BlurMixMulti(Object recipe){this(recipe,new MixColor(2,0xb3445566,0x40223344),new MixColor(3,0x80556677,0x10556677));}
        public BlurMixMulti(MixColor foreground,MixColor background){this(null,foreground,background);}
        private BlurMixMulti(Object recipe,MixColor foreground,MixColor background){super(recipe);foregroundMixColor=foreground;backgroundMixColor=background;}
        public MixColor getForegroundMixColor(){return foregroundMixColor;}
        public MixColor getBackgroundMixColor(){return backgroundMixColor;}
        public BlurMixMulti copy(MixColor foreground,MixColor background){return new BlurMixMulti(recipe,foreground,background);}
    }
    public static final class BlurMixMultiWithShader extends BlurMixConfig {
        private final MixColorWithShader foregroundShaderParam,backgroundShaderParam;
        // These intentionally shadow the superclass fields just as the ROM does.
        private float mirrorScale;
        private boolean alphaWithBlurAmount;
        private int placeHolderColor;
        private final java.util.ArrayList<Object> mixMultiShaderParams=new java.util.ArrayList<>();
        public BlurMixMultiWithShader(Object recipe){this(recipe,new MixColorWithShader(23,0xb3445566,29,0x40223344),new MixColorWithShader(3,0x80556677,3,0x10556677),1f,false,0);}
        public BlurMixMultiWithShader(MixColorWithShader foreground,MixColorWithShader background,float mirror,boolean alpha,int placeholder){this(null,foreground,background,mirror,alpha,placeholder);}
        private BlurMixMultiWithShader(Object recipe,MixColorWithShader foreground,MixColorWithShader background,float mirror,boolean alpha,int placeholder){super(recipe);foregroundShaderParam=foreground;backgroundShaderParam=background;mirrorScale=mirror;alphaWithBlurAmount=alpha;placeHolderColor=placeholder;}
        public MixColorWithShader getForegroundShaderParam(){return foregroundShaderParam;}
        public MixColorWithShader getBackgroundShaderParam(){return backgroundShaderParam;}
        public java.util.ArrayList<Object> getMixMultiShaderParams(){return mixMultiShaderParams;}
        public BlurMixMultiWithShader copy(MixColorWithShader foreground,MixColorWithShader background,float mirror,boolean alpha,int placeholder){return new BlurMixMultiWithShader(recipe,foreground,background,mirror,alpha,placeholder);}
        @Override public float getMirrorScale(){return mirrorScale;}
        @Override public void setMirrorScale(float value){mirrorScale=value;}
        @Override public boolean getAlphaWithBlurAmount(){return alphaWithBlurAmount;}
        @Override public void setAlphaWithBlurAmount(boolean value){alphaWithBlurAmount=value;}
        @Override public int getPlaceHolderColor(){return placeHolderColor;}
        @Override public void setPlaceHolderColor(int value){placeHolderColor=value;}
    }
    public final Object recipe;
    public boolean alphaWithBlurAmount;
    public float mirrorScale=1f;
    public int placeHolderColor;
    public BlurMixConfig(Object recipe){this.recipe=recipe;}
    public boolean getAlphaWithBlurAmount(){return alphaWithBlurAmount;}
    public float getMirrorScale(){return mirrorScale;}
    public int getPlaceHolderColor(){return placeHolderColor;}
    public void setAlphaWithBlurAmount(boolean value){alphaWithBlurAmount=value;}
    public void setMirrorScale(float value){mirrorScale=value;}
    public void setPlaceHolderColor(int value){placeHolderColor=value;}
}
