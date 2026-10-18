package com.oplus.posteffect;
/** Six colour/mode fields of the captured ROM's native request contract. */
public class BlendParam {
    public int blendMode,blendColorA,blendColorB,fgBlendMode,fgBlendColorA,fgBlendColorB;
    public BlendParam copy(){BlendParam b=new BlendParam();b.blendMode=blendMode;b.blendColorA=blendColorA;b.blendColorB=blendColorB;b.fgBlendMode=fgBlendMode;b.fgBlendColorA=fgBlendColorA;b.fgBlendColorB=fgBlendColorB;return b;}
    public boolean empty(){return(blendMode|blendColorA|blendColorB|fgBlendMode|fgBlendColorA|fgBlendColorB)==0;}
}
