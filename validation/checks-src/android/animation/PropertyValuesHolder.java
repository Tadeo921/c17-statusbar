package android.animation;

/** Native-shaped finite keyframes; production tests reject unsupported holder shapes. */
public class PropertyValuesHolder {
    private final String name;
    private float[] values;
    public int writesForCheck;
    public PropertyValuesHolder(String name,float... values){this.name=name;this.values=values.clone();}
    public static PropertyValuesHolder ofFloat(String name,float... values){return new PropertyValuesHolder(name,values);}
    public String getPropertyName(){return name;}
    public void setFloatValues(float... values){this.values=values.clone();writesForCheck++;}
    public float[] valuesForCheck(){return values.clone();}
    public float valueForCheck(float fraction){return values[0]+(values[values.length-1]-values[0])*fraction;}
}
