package com.heytap.log.nx.obus;
/** Simulates the intercepted native BackgroundUtils without implementing OEM blur. */
public final class a {
    public static dev.puitheme.FluidCloudAccent controller;
    public static dev.puitheme.FluidCloudStyle style;
    public static int builds, mixed, gradient;
    public static android.graphics.drawable.Drawable nativeBackgroundForCheck;
    /** The real custom gradient has no ConstantState; recreate it through the
     * OEM initializer rather than injecting a resource GradientDrawable copy. */
    public interface NativeBackgroundFactoryForCheck {
        android.graphics.drawable.Drawable create(android.view.View host, com.oplus.view.ViewRootManager manager);
    }
    public static NativeBackgroundFactoryForCheck nativeBackgroundFactoryForCheck;
    public a(android.content.Context context, int ignored) { }
    public void b(android.view.View host, com.oplus.view.ViewRootManager manager) {
        dev.puitheme.FluidCloudAccent.BackgroundScope scope = controller == null ? null : controller.beginBackground(this, host, manager);
        dev.puitheme.FluidCloudStyle.BackgroundScope material = style == null ? null : style.beginBackground(this, host, manager);
        try {
            builds++;
            if(nativeBackgroundFactoryForCheck!=null)host.setBackground(nativeBackgroundFactoryForCheck.create(host,manager));
            else if(nativeBackgroundForCheck!=null)host.setBackground(nativeBackgroundForCheck.getConstantState().newDrawable().mutate());
            if(manager==null){
                android.graphics.drawable.GradientDrawable fallback=new android.graphics.drawable.GradientDrawable();
                fallback.setColor(0xb3262626);fallback.setCornerRadius(36f);fallback.setStroke(2,0x40ffffff);
                host.setBackground(fallback);
                if(scope!=null)scope.completed();
                if(material!=null)material.completed();
                return;
            }
            mixed = color(host, 1, 0xb3262626);
            com.oplus.graphics.OplusBlurParam param = new com.oplus.graphics.OplusBlurParam();
            float[] values = param.toFloatArray();
            values[4] = ((mixed >> 16) & 255) / 255f; values[5] = ((mixed >> 8) & 255) / 255f;
            values[6] = (mixed & 255) / 255f; values[7] = (mixed >>> 24) / 255f;
            param.fromFloatArray(values); manager.setBlurParams(param);
            gradient = color(host, 2, 0xff000000);
            if (scope != null) scope.completed();
            if (material != null) material.completed();
        } finally { if (material != null) material.close(); if (scope != null) scope.close(); }
    }
    private int color(android.view.View host,int id,int nativeColor) {
        int result=controller == null ? nativeColor : controller.resourceColor(host.getResources(),id,nativeColor);
        return style == null ? result : style.resourceColor(host.getResources(),id,result);
    }
}
