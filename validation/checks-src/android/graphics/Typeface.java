package android.graphics;
public class Typeface {
    public static final Typeface DEFAULT=new Typeface();
    public static final int BOLD=1,NORMAL=0,ITALIC=2;
    public int weight;
    public boolean italic;
    /** A baked variable glyph axis is independent of the requested style's getWeight(). */
    public boolean variableWeight;
    /** Distinguishes a provider-loaded fixture family from its native fallback. */
    public String sourceForCheck;
    public float variationWeight,variationWidth=Float.NaN,variationRound=Float.NaN,variationHeight=Float.NaN;
    public static int variationCreations;
    public boolean isItalic() {return italic;}
    public int getWeight() { return weight > 0 ? weight : 400; }
    public boolean isBold() { return getWeight() >= 600; }
    public float effectiveWeight() {return variableWeight&&variationWeight>0?variationWeight:getWeight();}
    public static Typeface withVariation(Typeface family,android.graphics.fonts.FontVariationAxis[] axes) {
        Typeface value=new Typeface();value.weight=family.getWeight();value.italic=family.italic;
        value.variableWeight=family.variableWeight;value.variationWeight=family.variationWeight;
        value.sourceForCheck=family.sourceForCheck;
        value.variationWidth=family.variationWidth;value.variationRound=family.variationRound;value.variationHeight=family.variationHeight;
        if(axes!=null)for(android.graphics.fonts.FontVariationAxis axis:axes){
            if("wght".equals(axis.getTag()))value.variationWeight=axis.getStyleValue();
            else if("wdth".equals(axis.getTag()))value.variationWidth=axis.getStyleValue();
            else if("rond".equals(axis.getTag()))value.variationRound=axis.getStyleValue();
            else if("HGHT".equals(axis.getTag()))value.variationHeight=axis.getStyleValue();
        }
        variationCreations++;return value;
    }
    /** Mirrors the hidden framework factory; unlike new Paint's axes this is baked into a face. */
    public static Typeface createFromTypefaceWithVariation(Typeface family,java.util.List<android.graphics.fonts.FontVariationAxis> axes) {
        return withVariation(family==null?DEFAULT:family,axes.toArray(new android.graphics.fonts.FontVariationAxis[0]));
    }
    private static final java.util.Map<String,Typeface> cache=new java.util.HashMap<>();
    public static Typeface create(String family, int style) { return new Typeface(); }
    public static Typeface create(Typeface family,int weight,boolean italic) {
        String key=System.identityHashCode(family)+":"+weight+":"+italic;
        Typeface value=cache.get(key);if(value==null){value=new Typeface();value.weight=weight;value.italic=italic;
            if(family!=null){value.variableWeight=family.variableWeight;value.variationWeight=family.variationWeight;
                value.sourceForCheck=family.sourceForCheck;
                value.variationWidth=family.variationWidth;value.variationRound=family.variationRound;value.variationHeight=family.variationHeight;}
            cache.put(key,value);}return value;
    }
    public static Typeface create(Typeface family,int style) { return create(family,(style&BOLD)!=0?700:400,(style&ITALIC)!=0); }
    public static final class Builder {
        private final java.io.File file;
        private final java.io.FileDescriptor descriptor;
        private int weight=400;
        private boolean italic;
        private String axes;
        public Builder(java.io.File file){this.file=file;descriptor=null;}
        public Builder(java.io.FileDescriptor descriptor){this.descriptor=descriptor;file=null;}
        public Builder setWeight(int value){weight=value;return this;}
        public Builder setItalic(boolean value){italic=value;return this;}
        public Builder setFontVariationSettings(String value){axes=value;return this;}
        public Builder setFallback(String value){return this;}
        public Typeface build(){
            if(file!=null?!file.isFile()||file.length()==0:descriptor==null||!descriptor.valid())throw new IllegalStateException("Font fixture unavailable");
            Typeface face=new Typeface();face.weight=weight;face.italic=italic;face.variableWeight=true;face.sourceForCheck="custom";
            return axes==null?face:withVariation(face,android.graphics.fonts.FontVariationAxis.fromFontVariationSettings(axes));
        }
    }
}
