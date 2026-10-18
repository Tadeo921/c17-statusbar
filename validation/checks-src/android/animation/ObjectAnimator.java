package android.animation;

import android.util.Property;
import android.view.View;

/** Same two-endpoint translationX holder used by the inspected SystemUI DEX. */
public class ObjectAnimator extends ValueAnimator {
    private Object target;
    private String propertyName;
    private Property property;
    public static ObjectAnimator ofFloat(Object target,String name,float... endpoints){
        ObjectAnimator result=new ObjectAnimator();result.target=target;result.propertyName=name;
        result.values=new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat(name,endpoints)};return result;
    }
    public static <T> ObjectAnimator ofFloat(T target,Property<T,Float> property,float... endpoints){
        ObjectAnimator result=ofFloat(target,property.getName(),endpoints);result.property=property;return result;
    }
    public String getPropertyName(){return propertyName;}
    public Object getTarget(){return target;}
    public void setTarget(Object value){target=value;}
    @Override public ObjectAnimator setDuration(long value){super.setDuration(value);return this;}
    @SuppressWarnings("unchecked") protected void applyForCheck(){
        if(values.length==0)return;float value=values[0].valueForCheck(getAnimatedFraction());
        if(property!=null)property.set(target,value);
        else if(target instanceof View&&"translationX".equals(propertyName))((View)target).setTranslationX(value);
    }
}
