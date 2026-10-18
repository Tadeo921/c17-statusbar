package kswc;
import pantanal.app.bean.*;
public final class yN implements pantanal.app.Card {
    public final CardViewInfo c;public volatile PantanalUIData n;public IN i;
    public yN(CardViewInfo info,PantanalUIData cache){c=info;n=cache;}
    public static void a(yN owner,PantanalUIData data){owner.n=data;}
    public PantanalUIData getUIData(){throw new AssertionError("must not initialize SDK cache fallback");}
}
