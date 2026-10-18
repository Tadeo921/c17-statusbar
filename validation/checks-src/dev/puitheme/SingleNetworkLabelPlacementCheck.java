package dev.puitheme;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams;

/** Uses the actual OEM three-sibling constraint shape and verifies isolated ownership. */
public final class SingleNetworkLabelPlacementCheck {
    private static final int SIGNAL=0x7f0a088e,LABEL=0x7f0a0898,ROAM=0x7f0a088b;
    private static int checks;
    private static void check(boolean value,String why) {checks++;if(!value)throw new AssertionError(why);}
    private static final class R extends android.content.res.Resources {
        @Override public int getIdentifier(String name,String type,String pkg) {
            return name.equals("mobile_signal_data_outer")?SIGNAL:name.equals("mobile_roaming_text")?ROAM:0;
        }
    }
    private static final class Label extends TextView {
        int direction;
        private final R resources=new R();Label(){super(null);setId(LABEL);setGravity(81);}
        @Override public android.content.res.Resources getResources(){return resources;}
        @Override public int getLayoutDirection(){return direction;}
    }
    private static LayoutParams labelParams(){LayoutParams p=new LayoutParams(-2,-2);p.endToEnd=0;p.bottomToBottom=0;
        p.bottomMargin=4;p.setMarginEnd(3);return p;}
    private static LayoutParams signalParams(){LayoutParams p=new LayoutParams(-2,0);p.endToEnd=0;p.topToTop=0;p.bottomToBottom=0;
        p.setMarginStart(2);return p;}
    private static LayoutParams roamParams(){LayoutParams p=new LayoutParams(-2,-2);p.startToStart=0;p.endToStart=SIGNAL;
        p.bottomToBottom=0;p.bottomMargin=2;p.setMarginEnd(1);return p;}
    public static void main(String[] args){
        ConstraintLayout parent=new ConstraintLayout();Label label=new Label();View signal=new View(null),roam=new View(null);
        signal.setId(SIGNAL);roam.setId(ROAM);LayoutParams nativeLabel=labelParams(),nativeSignal=signalParams(),nativeRoam=roamParams();
        parent.addView(label,nativeLabel);parent.addView(signal,nativeSignal);parent.addView(roam,nativeRoam);
        SingleNetworkLabelPlacement placement=new SingleNetworkLabelPlacement(label);check(placement.apply(),"audited native shape supported");
        LayoutParams l=(LayoutParams)label.getLayoutParams(),s=(LayoutParams)signal.getLayoutParams(),r=(LayoutParams)roam.getLayoutParams();
        check(l!=nativeLabel&&s!=nativeSignal&&r!=nativeRoam,"native LP objects never mutated");
        check(nativeLabel.endToEnd==0&&nativeSignal.startToEnd==-1&&nativeRoam.startToStart==0,"OEM baseline intact");
        check(l.startToEnd==SIGNAL&&l.endToEnd==0&&l.startToStart==-1,"default label follows the same logical signal as Compose");
        check(r.startToStart==0&&r.endToStart==SIGNAL&&r.startToEnd==-1&&r.endToEnd==-1,"R remains before signal in its own slot");
        check(s.startToEnd==ROAM&&s.endToStart==LABEL&&s.endToEnd==-1,"default signal precedes independent label");
        check(r.horizontalChainStyle==2&&s.horizontalChainStyle==2,"roaming and signal chain heads retain packed alignment");
        check(l.horizontalChainStyle==2&&l.horizontalBias==1f,"packed reciprocal chain is anchored");
        check(l.width==-2&&l.height==-2&&l.topToTop==0&&l.bottomToBottom==0,"label wraps without badge vertical pin");
        check(l.bottomMargin==0&&l.getMarginEnd()==0&&label.getGravity()==17,"old badge offsets cleared");
        check(r.bottomToBottom==0&&r.bottomMargin==2&&r.getMarginEnd()==1,"native roaming vertical offset retained");
        check(s.height==0&&s.topToTop==0&&s.bottomToBottom==0&&s.getMarginStart()==2,"signal measurement unchanged");
        int writes=label.layoutRequests+signal.layoutRequests+roam.layoutRequests;
        for(int i=0;i<100;i++)check(placement.apply(),"repeat remains supported");
        check(writes==label.layoutRequests+signal.layoutRequests+roam.layoutRequests,"repeat doesn't reassign LPs");
        roam.setVisibility(View.GONE);check(placement.apply(),"native GONE R remains supported chain member");
        placement.restore();check(label.getLayoutParams()==nativeLabel&&signal.getLayoutParams()==nativeSignal&&roam.getLayoutParams()==nativeRoam,"restore original three identities");
        check(label.getGravity()==81&&roam.getVisibility()==View.GONE,"restore doesn't overwrite OEM visibility");
        parent.removeView(roam);check(placement.apply(),"two-item variant works");
        l=(LayoutParams)label.getLayoutParams();s=(LayoutParams)signal.getLayoutParams();
        check(l.startToEnd==SIGNAL&&s.endToStart==LABEL,"default Compose order has no roaming dependency when absent");
        for(int[] scenario:new int[][]{{0,0,1},{1,0,0},{0,1,0},{1,1,0},{0,2,1},{1,2,1}}){
            label.direction=scenario[0];placement.configure(scenario[1],0,0);check(placement.apply(),"default/physical order applies in both directions");
            l=(LayoutParams)label.getLayoutParams();boolean physicallyAfter=label.direction==View.LAYOUT_DIRECTION_RTL?l.endToStart==SIGNAL:l.startToEnd==SIGNAL;
            check(physicallyAfter==(scenario[2]==1),"default follows Compose logical direction; explicit left/right stays physical");
        }
        label.direction=0;placement.configure(0,0,0);check(placement.apply(),"default restores after physical order choices");
        LayoutParams foreign=labelParams();label.setLayoutParams(foreign);placement.restore();
        check(label.getLayoutParams()==foreign&&signal.getLayoutParams()==nativeSignal,"new OEM LP never overwritten");
        label.setLayoutParams(new ViewGroup.LayoutParams(-2,-2));check(!placement.apply(),"unsupported container fails closed");
        check(signal.getLayoutParams()==nativeSignal,"unsupported case leaves signal unchanged");
        System.out.println("SingleNetworkLabelPlacementCheck: "+checks+" checks passed");
    }
}
