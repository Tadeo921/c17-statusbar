package androidx.compose.foundation.layout;
import androidx.compose.ui.layout.MeasurePolicy;
import androidx.compose.ui.node.LayoutNode;
import androidx.compose.ui.node.MeasurePassDelegate;
import androidx.compose.ui.node.LookaheadPassDelegate;
import java.util.List;
/** Mirrors native Row's width/RTL placement; child modifiers remain attached to the actual node. */
public class RowMeasurePolicy implements MeasurePolicy {
    public Object lastScope;
    public long lastConstraints;
    public List<?> lastChildren;
    public dev.puitheme.NetworkIconOrder placementForCheck;
    public int lastMeasuredWidth;
    private static LayoutNode node(Object child) { return child instanceof MeasurePassDelegate ? ((MeasurePassDelegate)child).getLayoutNode() : ((LookaheadPassDelegate)child).nativeNode(); }
    public java.util.Map<String,Integer> measure(Object scope,List<?> children,long constraints) {
        lastScope=scope;lastConstraints=constraints;lastChildren=children;
        int x=0,total=0;for(Object child:children)total+=node(child).width;
        int[] positions=new int[children.size()];boolean rtl=Boolean.TRUE.equals(scope);
        try { rtl|=scope!=null&&scope.getClass().getMethod("getLayoutDirection").invoke(scope).toString().equals("Rtl"); }
        catch(ReflectiveOperationException ignored) { }
        for(int i=0;i<children.size();i++){LayoutNode child=node(children.get(i));positions[i]=rtl?total-x-child.width:x;x+=child.width;}
        Object[] args={children.toArray(),scope,0,positions,total,80,null,0,children.size(),0};
        if(placementForCheck!=null)args=placementForCheck.placeHelperArguments(this,args);
        return placeHelper((Object[])args[0],args[1],(Integer)args[2],(int[])args[3],(Integer)args[4],(Integer)args[5],(int[])args[6],(Integer)args[7],(Integer)args[8],(Integer)args[9]);
    }
    public java.util.Map<String,Integer> placeHelper(Object[] placeables,Object scope,int baseline,int[] positions,
            int width,int height,int[] offsets,int start,int end,int offset){
        lastMeasuredWidth=width;java.util.Map<String,Integer> result=new java.util.LinkedHashMap<>();
        for(int i=0;i<placeables.length;i++)result.put(node(placeables[i]).name,positions[i]);return result;
    }
    public int minIntrinsicWidth(Object scope,List<?> children,int height) {int total=0;for(Object child:children)total+=node(child).width;return total;}
}
