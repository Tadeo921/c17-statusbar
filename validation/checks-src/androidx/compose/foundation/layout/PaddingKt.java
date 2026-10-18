package androidx.compose.foundation.layout;
import androidx.compose.ui.Modifier;
/** Exact signature shape of Compose's physical padding modifier, for protocol tests. */
public final class PaddingKt {
    public static Modifier absolutePadding(Modifier base,float left,float top,float right,float bottom) {
        return base.then(new PaddingElement(left,top,right,bottom));
    }
    public static final class PaddingElement extends Modifier.Element {
        public final float left,top,right,bottom;
        public PaddingElement(float left,float top,float right,float bottom) { this.left=left;this.top=top;this.right=right;this.bottom=bottom; }
    }
}
