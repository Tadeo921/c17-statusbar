package kswc;
import pantanal.app.bean.*;
public final class IN {
    public final CardViewInfo a;public volatile byte[] h;public volatile com.oplus.seedling.sdk.seedling.ISeedling q=value->{};public int updates;
    public IN(CardViewInfo info,byte[] pending){a=info;h=pending;}
    public void k(){if(h!=null&&q!=null){updates++;q.updateData(h);}h=null;}
}
