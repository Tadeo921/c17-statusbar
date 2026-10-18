package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Renderer contract: real event snapshots, fresh owned resources, no meter
 * mutation, exact span retention and no resource/battery work in repeated draw. */
public final class BigClockSystemIconCheck {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    private static Object field(Object owner,String name)throws Exception {
        Field value=owner instanceof Class?((Class<?>)owner).getDeclaredField(name):owner.getClass().getDeclaredField(name);
        value.setAccessible(true);return value.get(owner instanceof Class?null:owner);
    }
    public static class Graphic extends Drawable {
        int draws,tint,alpha=255,boundsChanges;
        public void draw(Canvas canvas){draws++;}
        public void setTint(int value){tint=value;}
        public void setAlpha(int value){alpha=value;}
        public void setColorFilter(ColorFilter value){}
        public int getOpacity(){return -3;}
        @Override public void setBounds(int l,int t,int r,int b){boundsChanges++;super.setBounds(l,t,r,b);}
    }
    public static final class Source extends Resources {
        final Map<String,Integer> ids=new HashMap<>();final Map<Integer,String> names=new HashMap<>();
        final Map<String,Graphic> last=new HashMap<>();int loads,lookups;boolean guarded=true,fail;
        java.util.Set<String> available;
        public int getIdentifier(String name,String type,String pkg){lookups++;if(fail||available!=null&&!available.contains(name))return 0;Integer id=ids.get(name);if(id==null){id=ids.size()+1;ids.put(name,id);names.put(id,name);}return id;}
        public Drawable getDrawable(int id,Theme theme){loads++;guarded&=BigClockSystemIcon.nativeLookupActive();Graphic result=new Graphic();last.put(names.get(id),result);return result;}
        public String getResourceEntryName(int id){String name=names.get(id);if(name==null)throw new NotFoundException("Missing fixture drawable");return name;}
    }
    public static final class Environment extends Context {
        final Source nativeSource=new Source(),module=new Source();
        boolean moduleUnavailable;int packageContexts,settingsReads;
        public Resources getResources(){return nativeSource;}
        public android.content.ContentResolver getContentResolver(){settingsReads++;return super.getContentResolver();}
        public Context createPackageContext(String pkg,int flags){packageContexts++;if(moduleUnavailable)throw new IllegalStateException("No assets");return new Context(){public Resources getResources(){return module;}};}
        public ClassLoader getClassLoader(){return new ClassLoader(BigClockSystemIconCheck.class.getClassLoader()){
            public Class<?> loadClass(String name)throws ClassNotFoundException {
                if(name.equals("com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable"))return PrivateBattery.class;
                return super.loadClass(name);
            }
        };}
    }
    /** Dedicated fixture mirrors only audited native constructor and setters;
     * unrelated global OEM fixtures remain untouched. */
    public static class PrivateBattery extends Graphic {
        static int created,destroyed,setLevels,setCharges,palettes;
        public int batteryLevel,chargeIconId,progressColor,backgroundColor,outlineColor;
        public boolean isShowPercentIn;
        public PrivateBattery(Context context,int unused){created++;check(unused==0,"native binder uses constructor argument zero");}
        public void setShowPercentIn(boolean value){isShowPercentIn=value;}
        public void setBatteryLevel(int value){setLevels++;batteryLevel=value;}
        public void setChargeIconId(int value){setCharges++;chargeIconId=value;}
        public int getChargeIconId(){return chargeIconId;}
        public void updateDrawableTint(){palettes++;}
        public void onDestroy(){destroyed++;}
    }
    public static final class AlternateBattery extends PrivateBattery {
        public AlternateBattery(Context context,int unused){super(context,unused);}
    }
    public static final class VerticalBatteryContentDrawable extends PrivateBattery {
        public VerticalBatteryContentDrawable(Context context,int unused){super(context,unused);}
    }
    public static final class CircleBatteryContentDrawable extends Graphic {
        public final Paint mCircleFrontPaint=new Paint(),mCircleBackPaint=new Paint(),mCircleChargingPaint=new Paint(),circleFramePaint=new Paint();
        public Drawable chargingDrawable;public int level,charge;public boolean destroyed;
        public CircleBatteryContentDrawable(Context context,int unused){check(unused==0,"circle constructor argument");}
        public void setBatteryLevel(int value){level=value;}public void setChargeIconId(int value){charge=value;}
        public int getChargeIconId(){return charge;}public void onDestroy(){destroyed=true;}
    }
    public static final class Charge {public boolean isVisible;public int iconId;}
    public static final class Cell {
        final int direct,primary,secondary;int conversions;
        Cell(int direct,int primary,int secondary){this.direct=direct;this.primary=primary;this.secondary=secondary;}
        public int toSignalDrawableState(){conversions++;return direct;}
        public int toStackedSignalDrawableState(boolean first){conversions++;return first?primary:secondary;}
    }
    public static final class Dual {
        final Cell primary,secondary;
        Dual(Cell primary,Cell secondary){this.primary=primary;this.secondary=secondary;}
        public Cell getPrimary(){return primary;}public Cell getSecondary(){return secondary;}
    }
    public static final class ViewModel {
        final int sub;ViewModel(int sub){this.sub=sub;}public int getSubscriptionId(){return sub;}
    }
    public static final class Binding {
        final ViewModel model;Binding(int sub){model=new ViewModel(sub);}public ViewModel getViewModel(){return model;}
    }
    public static final class Meter extends View {
        final PrivateBattery original;final Charge charge=new Charge();Drawable candidate;int reads;
        Meter(Environment context){super(context);original=new PrivateBattery(context,0);candidate=original;original.isShowPercentIn=true;original.setBounds(0,0,116,68);}
        public Drawable getBatteryStyleDrawable(){reads++;return candidate;}
        public Charge getBatteryCharge(){return charge;}
    }
    private static BigClockSystemIcon.Span icon(int token) {return BigClockSystemIcon.obtain(token,BigClockSystemText.snapshot(),null);}
    private static Canvas draw(BigClockSystemIcon.Span span,Paint paint) {Canvas c=new Canvas();span.draw(c,"\ufffc",0,1,4,0,40,60,paint);return c;}
    private static void settleIcons()throws Exception{
        java.util.concurrent.ExecutorService work=(java.util.concurrent.ExecutorService)field(IconManagementRuntime.class,"WORK");
        for(int i=0;i<3;i++){work.submit(()->{}).get(30,java.util.concurrent.TimeUnit.SECONDS);android.os.Handler.drainForCheck();}
    }
    private static void resetIcons()throws Exception{IconManagementRuntime.releaseRuntime();settleIcons();}
    /** Real private library, validated ZIP/PNG and provider boundary. No runtime image/map injection. */
    private static final class V3Assets implements AutoCloseable {
        final java.nio.file.Path directory=java.nio.file.Files.createTempDirectory("c17-clock-v3-");
        final Thread caller=Thread.currentThread();int reads;boolean workerOnly=true;String records;
        final android.content.ContentResolver provider=new android.content.ContentResolver(){
            @Override public java.io.InputStream openInputStream(android.net.Uri uri)throws java.io.FileNotFoundException{
                reads++;workerOnly&=Thread.currentThread()!=caller;
                try{return java.nio.file.Files.newInputStream(IconManagementRepository.providerFile(owner,uri).toPath());}
                catch(java.io.IOException failed){throw new java.io.FileNotFoundException(failed.getMessage());}
            }
            @Override public void notifyChange(android.net.Uri uri,android.database.ContentObserver observer){super.notifyChange(uri,observer);dispatchSettingsChange();}
        };
        final Context owner=new Context(){public java.io.File getFilesDir(){return directory.toFile();}public android.content.ContentResolver getContentResolver(){return provider;}};
        V3Assets()throws Exception{}
        Bitmap installWifiMask()throws Exception{
            java.awt.image.BufferedImage png=new java.awt.image.BufferedImage(12,10,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=1;y<9;y++)for(int x=1;x<11;x++)png.setRGB(x,y,0xffffffff);
            java.io.ByteArrayOutputStream encoded=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(png,"png",encoded);
            String manifest=IconManagementJson.encode(Map.of("format",IconManagementManifest.FORMAT,"version",3,"id","clock-mask","name","真实大时钟蒙版检查","author","check","license","GPL-3.0-only","fallback","native","icons",Map.of("wifi.3",Map.of("source","assets/wifi.png","render","mask")),"transitions",Map.of()));
            java.nio.file.Path archive=directory.resolve("source.zip");
            try(java.util.zip.ZipOutputStream output=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(archive))){
                output.putNextEntry(new java.util.zip.ZipEntry("manifest.json"));output.write(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.closeEntry();
                output.putNextEntry(new java.util.zip.ZipEntry("assets/wifi.png"));output.write(encoded.toByteArray());output.closeEntry();
            }
            IconManagementRepository.Pack pack;try(IconManagementRepository.PreparedPack pending=IconManagementRepository.prepare(owner,archive.toFile(),"")){pack=pending.commit(()->true);}
            records=IconManagementSettings.applyPack(IconManagementSettings.DEFAULT,pack.id,Collections.singleton("wifi"),false);configure(records);
            Bitmap bitmap=IconManagementRuntime.bitmapForWifi(3,true);check(bitmap!=null&&bitmap.getConfig()==Bitmap.Config.ALPHA_8,"real v3 PNG admission did not extract an ALPHA_8 mask");
            check(reads>=2&&workerOnly,"v3 manifest/image provider reads did not run on the real worker");return bitmap;
        }
        void opacity(float value)throws Exception{records=IconManagementSettings.applyAppearance(records,"role:wifi.3",new IconManagementSettings.Appearance("inherit",0xff000000,0xff000000,value));configure(records);}
        void configure(String value)throws Exception{Bundle settings=new Bundle();settings.putString(IconManagementSettings.KEY,value);IconManagementRuntime.configure(owner,settings);settleIcons();}
        void disable()throws Exception{configure(IconManagementSettings.DEFAULT);}
        void publish()throws Exception{provider.notifyChange(android.net.Uri.parse(IconManagementRepository.URI_PREFIX),null);settleIcons();}
        @Override public void close()throws Exception{resetIcons();IconManagementRepository.clear(owner);try(java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.walk(directory)){for(java.nio.file.Path file:files.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(file);}}
    }
    private static void nativePresentation178()throws Exception {
        BigClockSystemIcon.releaseRuntime();resetIcons();
        Environment source=new Environment();source.nativeSource.getDisplayMetrics().density=4f;
        Bundle settings=new Bundle();settings.putBoolean(NotificationBigClockSettings.MASTER,true);
        settings.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);settings.putString(BigClockWidgetsSettings.RIGHT_TEXT,"（电池）");
        settings.putBoolean("battery_enabled",true);settings.putString(StatusBarSettings.BATTERY_STYLE,"pui");
        BigClockSystemIcon.configure(source,settings);
        Meter meter=new Meter(source);
        dev.puitheme.inline178.HorizontalBatteryContentDrawable original=new dev.puitheme.inline178.HorizontalBatteryContentDrawable(source,0);
        original.setBatteryLevel(62);meter.candidate=original;
        check(BigClockSystemIcon.nativeBatteryChanged(meter),"current native horizontal binder not accepted");
        BigClockSystemText.batteryRaw(62,100,3);
        Paint paint=new Paint();paint.setTextSize(18);paint.setColor(0xffabcdef);
        BigClockSystemIcon.Span unmeasured=icon(BigClockTextContent.BATTERY);
        check(unmeasured.getSize(paint,"x",0,1,null)==0&&((Number)BigClockSystemIcon.diagnostics().get("icon_battery_created")).intValue()==0,"binder-before-measure invented native battery coordinates");
        original.setBounds(0,0,116,68);
        check(BigClockSystemIcon.nativeBatteryChanged(meter),"real onLayout dimensions did not refresh source policy");unmeasured.close();
        BigClockSystemIcon.Span span=icon(BigClockTextContent.BATTERY);
        Object nativeHolder=field(span,"nativeBattery");
        check(nativeHolder!=null,"PUI component bypassed native geometry/text/charging renderer with hand-drawn slots");
        dev.puitheme.inline178.HorizontalBatteryContentDrawable owned=(dev.puitheme.inline178.HorizontalBatteryContentDrawable)field(nativeHolder,"drawable");
        Canvas canvas=draw(span,paint);
        check(owned!=original&&owned.nativeDraws==1&&original.nativeDraws==0,"component borrowed or bypassed actual native drawing");
        check(owned.getBounds().width()==116&&owned.getBounds().height()==68,"native density padding was recomputed in tiny text coordinates");
        check(owned.percentInPaint.getTypeface()==original.percentInPaint.getTypeface(),"component percentage lost actual native font family/axes");
        check(owned.insideDrawable!=original.insideDrawable&&owned.insideDrawable.getLevel()==6200,"owned PUI shapes did not share status-bar fill policy");
        check(canvas.textEffectiveWeight==750f,"component percentage replaced the native font with DEFAULT");
        int nativeLoads=source.nativeSource.loads,moduleLoads=source.module.loads,draws=owned.nativeDraws;
        for(int n=0;n<1000;n++)draw(span,paint);
        check(owned.nativeDraws==draws+1000&&source.nativeSource.loads==nativeLoads&&source.module.loads==moduleLoads,"stable draw reloads native or module assets");
        check(original.getBounds().width()==116&&original.percentInPaint.getTextSize()==37,"inline styling mutated real status-bar source");
        span.close();check(owned.destroyed==1&&original.destroyed==0,"private native lifecycle not isolated");
        settings.putString(StatusBarSettings.BATTERY_STYLE,"ios27");BigClockSystemIcon.configure(source,settings);
        BigClockSystemIcon.Span ios=icon(BigClockTextContent.BATTERY);draw(ios,paint);
        check(source.module.last.containsKey("c17_ios27_battery_bg")&&field(ios,"nativeBattery")!=null,"iOS component bypassed the shared native-slot style");
        ios.close();
        check(BigClockSystemIcon.nativeBatteryDetached(meter),"detaching the actual native source did not retire its policy");
        BigClockSystemIcon.Span retired=icon(BigClockTextContent.BATTERY);
        check(retired.getSize(paint,"x",0,1,null)==0&&field(retired,"nativeBattery")==null,"detached source kept a synthetic old battery");
        check(field(BigClockSystemIcon.class,"nativeOwner")==null,"detached native meter remained referenced");retired.close();
        BigClockSystemIcon.releaseRuntime();
    }
    private static void nativeSignals178()throws Exception {
        BigClockSystemIcon.releaseRuntime();BigClockSystemText.clear();resetIcons();
        Environment env=new Environment();Bundle settings=new Bundle();
        settings.putBoolean(NotificationBigClockSettings.MASTER,true);settings.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);
        settings.putString(BigClockWidgetsSettings.LEFT_TEXT,"（蜂窝信号）（网络状态）");settings.putBoolean("data_enabled",true);
        int p=env.nativeSource.getIdentifier("stat_signal_soft_signal_stacked_primary_2","drawable","com.android.systemui");
        int s=env.nativeSource.getIdentifier("stat_signal_lte_signal_stacked_secondary_4","drawable","com.android.systemui");
        int unstacked=env.nativeSource.getIdentifier("stat_signal_signal_novoice_2_os17","drawable","com.android.systemui");
        Cell first=new Cell(unstacked,p,0),second=new Cell(0,0,s);Dual dual=new Dual(first,second);
        check(!BigClockSystemIcon.nativeStackedChanged(dual)&&first.conversions==0,"disabled components evaluated native signal state");
        BigClockSystemIcon.configure(env,settings);
        check(BigClockSystemText.snapshot().cellularResourceId==p&&BigClockSystemText.snapshot().secondaryCellularResourceId==s,"activation did not seed exact current dual-SIM resources");
        BigClockSystemIcon.Span pair=icon(BigClockTextContent.CELLULAR);
        Graphic primary=(Graphic)field(pair,"graphic"),secondary=(Graphic)field(pair,"secondary");
        check(primary!=null&&secondary!=null&&env.module.last.containsKey("stat_signal_soft_signal_stacked_primary_2")
                &&env.module.last.containsKey("stat_signal_lte_signal_stacked_secondary_4"),"component guessed LTE primary from telephony bars or dropped second SIM");
        Paint paint=new Paint();paint.setTextSize(20);paint.setColor(0xffffffff);draw(pair,paint);
        check(primary.draws==1&&secondary.draws==1&&primary.getBounds().width()==secondary.getBounds().width(),"native two transparent layers were not superposed in the same box");
        int calls=first.conversions+second.conversions;long revision=BigClockSystemText.revision();
        for(int n=0;n<1000;n++)check(!BigClockSystemIcon.nativeStackedChanged(dual),"same native dual model dispatched another component pass");
        check(first.conversions+second.conversions==calls&&BigClockSystemText.revision()==revision,"stable native composition reselected resources");
        check(BigClockSystemIcon.nativeSingleChanged(new Binding(8),first,7)==false,"other SIM native binder replaced current-data glyph");
        check(BigClockSystemIcon.nativeSingleChanged(new Binding(7),first,7),"active-data native single binder not consumed");
        check(BigClockSystemText.snapshot().cellularResourceId==unstacked&&BigClockSystemText.snapshot().secondaryCellularResourceId==0,"single binder kept old dual resources");
        BigClockSystemIcon.Span single=icon(BigClockTextContent.CELLULAR);
        check(field(single,"secondary")==null&&env.module.last.containsKey("c17_signal_single_2"),"native no-voice selection was replaced by fabricated stacked primary");
        check(BigClockSystemIcon.nativeSingleChanged(new Binding(7),new Object(),7),"unsupported current native source kept the prior signal");
        check(BigClockSystemText.snapshot().cellularResourceId==0&&BigClockSystemText.snapshot().secondaryCellularResourceId==0,"unsupported source did not retire the previous glyph snapshot");
        BigClockSystemIcon.Span unknown=icon(BigClockTextContent.CELLULAR);
        check(unknown.getSize(paint,"x",0,1,null)==0&&field(unknown,"graphic")==null,"unknown native source guessed bars from stale telephony data");unknown.close();
        pair.close();single.close();BigClockSystemIcon.releaseRuntime();BigClockSystemText.clear();
    }
    public static void main(String[] args)throws Exception {
        BigClockSystemIcon.releaseRuntime();resetIcons();
        try(V3Assets v3=new V3Assets()){
        android.provider.Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,1);
        Environment disabled=new Environment();BigClockSystemIcon.configure(disabled,new Bundle());
        Meter disabledMeter=new Meter(disabled);int disabledCreated=PrivateBattery.created;
        check(!BigClockSystemIcon.nativeBatteryChanged(disabledMeter)&&disabledMeter.reads==0,"disabled components inspected native battery");
        check(disabled.settingsReads==0&&disabled.packageContexts==0&&PrivateBattery.created==disabledCreated,"disabled components queried Settings/resources or constructed glyphs");
        Object disabledWeak=field(BigClockSystemIcon.class,"nativeOwner");BigClockSystemIcon.nativeBatteryChanged(disabledMeter);
        check(field(BigClockSystemIcon.class,"nativeOwner")==disabledWeak,"same native meter allocated another weak reference");
        Bundle seed=new Bundle();seed.putBoolean(NotificationBigClockSettings.MASTER,true);seed.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);
        seed.putString(BigClockWidgetsSettings.LEFT_TEXT,"（电池）");
        BigClockSystemIcon.configure(disabled,seed);
        check(disabledMeter.reads==1&&!((String)field(BigClockSystemIcon.class,"batteryClass")).isEmpty(),"enabling components did not seed the existing attached meter");
        seed.putBoolean(NotificationBigClockSettings.MASTER,false);BigClockSystemIcon.configure(disabled,seed);
        int disabledReads=disabledMeter.reads;
        check(!BigClockSystemIcon.nativeBatteryChanged(disabledMeter)&&disabledMeter.reads==disabledReads,"disabled clock master still inspected native source");
        seed.putBoolean(NotificationBigClockSettings.MASTER,true);BigClockSystemIcon.configure(null,seed);
        check(disabledMeter.reads==disabledReads&&!BigClockSystemIcon.nativeBatteryChanged(disabledMeter),"null runtime context seeded or inspected a stale meter");
        BigClockSystemIcon.releaseRuntime();
        PrivateBattery.created=0;
        Environment env=new Environment();Bundle settings=new Bundle();settings.putBoolean("battery_enabled",true);
        settings.putBoolean(NotificationBigClockSettings.MASTER,true);
        settings.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);
        settings.putString(BigClockWidgetsSettings.LEFT_TEXT,"（蜂窝信号）（网络状态）（电池）");
        settings.putBoolean("data_enabled",true);settings.putBoolean("wifi_enabled",true);
        check(BigClockSystemIcon.configure(env,settings),"initial configuration not published");
        check(env.packageContexts==0,"configuration eagerly loaded module resources");
        long revision=BigClockSystemIcon.revision();
        check(!BigClockSystemIcon.configure(env,settings)&&revision==BigClockSystemIcon.revision(),"unchanged settings changed revision");
        long epoch=BigClockSystemText.selectSubscription(17);
        BigClockSystemText.airplane(false);BigClockSystemText.simAbsent(false);BigClockSystemText.service(epoch,17,true);BigClockSystemText.signal(epoch,17,2);
        BigClockSystemText.network(BigClockSystemText.Transport.CELLULAR,true);BigClockSystemText.batteryRaw(47,100,2);
        int actualCell=env.nativeSource.getIdentifier("stat_signal_lte_signal_stacked_primary_2","drawable","com.android.systemui");
        BigClockSystemText.cellularIcons(actualCell,"stat_signal_lte_signal_stacked_primary_2",0,null);
        Paint paint=new Paint();paint.setTextSize(20);paint.setColor(0x80445566);
        BigClockSystemIcon.Span cell=icon(BigClockTextContent.CELLULAR);
        Graphic raw=(Graphic)field(cell,"graphic");check(raw!=null,"known cellular icon missing");
        check(env.module.last.containsKey("stat_signal_lte_signal_stacked_primary_2"),"system layout does not use original primary family");
        check(BigClockSystemIcon.obtain(BigClockTextContent.CELLULAR,BigClockSystemText.snapshot(),cell)==cell,"same evidence rebuilt a span");
        int loads=env.module.loads,nativeLoads=env.nativeSource.loads;
        for(int i=0;i<1000;i++)draw(cell,paint);
        check(env.module.loads==loads&&env.nativeSource.loads==nativeLoads,"draw loaded resources");
        check(raw.boundsChanges==1&&raw.draws==1000,"repeated draw changed owned bounds");
        check(raw.tint==0xff445566&&raw.alpha==128,"text tint alpha applied more than once");
        check(env.module.guarded&&!BigClockSystemIcon.nativeLookupActive(),"raw lookup guard missing or leaked");
        settings.putBoolean("data_icon_enabled",false);BigClockSystemIcon.configure(env,settings);
        BigClockSystemIcon.Span nativeCell=icon(BigClockTextContent.CELLULAR);
        check(field(nativeCell,"graphic")!=null&&env.nativeSource.loads==1,"disabled cellular style did not use fresh native resource");
        settings.putBoolean("data_icon_enabled",true);
        settings.putBoolean("data_enabled",true);settings.putString(StatusBarSettings.SIGNAL_LAYOUT,"single");
        check(BigClockSystemIcon.configure(env,settings),"single-signal settings ignored");
        BigClockSystemIcon.Span single=icon(BigClockTextContent.CELLULAR);
        check(env.module.last.containsKey("c17_signal_single_2"),"effective single-signal asset ignored");
        BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.wifiIcon(504,"stat_signal_wifi_signal_3_os17",true);
        BigClockSystemIcon.Span wifi=icon(BigClockTextContent.NETWORK);
        check(env.module.last.containsKey("stat_signal_wifi_signal_3_os17"),"native Wi-Fi resource state ignored");
        draw(wifi,paint);Graphic wifiGraphic=(Graphic)field(wifi,"graphic");
        check(wifiGraphic.getBounds().width()>wifiGraphic.getBounds().height(),"Wi-Fi intrinsic aspect was stretched square");
        Paint tiny=new Paint(){public int getFontMetricsInt(FontMetricsInt m){m.top=m.ascent=-2;m.bottom=m.descent=1;return 3;}};
        tiny.setTextSize(80);tiny.setColor(0xff112233);draw(wifi,tiny);
        check(wifiGraphic.getBounds().height()<=3&&wifi.getSize(tiny,"x",0,1,null)<=6,"short-font icon exceeded actual line height");
        settings.putBoolean("wifi_icon_enabled",false);BigClockSystemIcon.configure(env,settings);
        int nativeBefore=env.nativeSource.loads;BigClockSystemIcon.Span nativeWifi=icon(BigClockTextContent.NETWORK);
        check(field(nativeWifi,"graphic")!=null&&env.nativeSource.loads==nativeBefore+1,"disabled Wi-Fi style still used module assets");
        settings.putBoolean("wifi_icon_enabled",true);BigClockSystemIcon.configure(env,settings);
        wifi.close();wifi=icon(BigClockTextContent.NETWORK);int readyLoads=env.module.loads;
        Bitmap wifiMask=v3.installWifiMask();
        long beforeReady=BigClockSystemIcon.revision();BigClockSystemIcon.assetsChanged();
        check(BigClockSystemIcon.revision()>beforeReady&&BigClockSystemIcon.obtain(BigClockTextContent.NETWORK,BigClockSystemText.snapshot(),wifi)==wifi,"pack ready rebuilt span or lost event revision");
        Canvas pack=draw(wifi,paint);
        check(pack.bitmapDraws==1&&pack.bitmapLog.get(0).bitmap==wifiMask&&pack.bitmapLog.get(0).alpha==128,"pack ALPHA mask ignored text opacity");
        check(env.module.loads==readyLoads,"pack redraw unexpectedly fetched a drawable");
        int providerReads=v3.reads;v3.opacity(50f);Canvas half=draw(wifi,paint);
        check(half.bitmapDraws==1&&half.bitmapLog.get(0).bitmap==wifiMask&&half.bitmapLog.get(0).alpha==64,"v3 50% icon opacity must multiply text Alpha128 once to64");
        for(int i=0;i<100;i++)draw(wifi,paint);
        check(v3.reads==providerReads&&env.module.loads==readyLoads,"appearance-only configure or stable inline draw reread/decoded resources");
        BigClockSystemText.wifiIcon(null,null,false);
        BigClockSystemIcon.Span hidden=icon(BigClockTextContent.NETWORK);
        check(draw(hidden,paint).bitmapDraws==0,"hidden/unknown Wi-Fi reused last full bars");
        v3.disable();
        BigClockSystemIcon.Span battery=icon(BigClockTextContent.BATTERY);
        check(field(battery,"nativeBattery")==null&&battery.getSize(paint,"x",0,1,null)==0,"missing native source invented a battery appearance");
        int batteryLoads=env.module.loads;Canvas number=draw(battery,paint);
        check(number.text==null&&number.bitmapDraws==0,"unverified battery source drew a guessed percentage or pack glyph");
        for(int i=0;i<100;i++)draw(battery,paint);
        check(env.module.loads==batteryLoads&&PrivateBattery.created==0,"PUI draw created native instance/resources");
        BigClockSystemIcon.Span anotherBattery=icon(BigClockTextContent.BATTERY);
        check(anotherBattery!=battery,"two slots share a mutable span");
        settings.putString(StatusBarSettings.BATTERY_STYLE,"ios27");BigClockSystemIcon.configure(env,settings);
        BigClockSystemIcon.Span ios=icon(BigClockTextContent.BATTERY);draw(ios,paint);
        check(ios.getSize(paint,"x",0,1,null)==0,"iOS guessed native geometry before a meter was bound");
        Environment nativeOnly=new Environment();nativeOnly.moduleUnavailable=true;
        settings.putBoolean("battery_enabled",false);BigClockSystemIcon.configure(nativeOnly,settings);
        Meter meter=new Meter(nativeOnly);meter.original.batteryLevel=91;meter.original.progressColor=0xff00ff00;
        meter.charge.isVisible=true;meter.charge.iconId=19;
        check(BigClockSystemIcon.nativeBatteryChanged(meter),"native battery event not accepted");
        check(!BigClockSystemIcon.nativeBatteryChanged(meter),"unchanged native policy changed revision");
        int originalCreates=PrivateBattery.created;
        BigClockSystemIcon.Span nativeSpan=icon(BigClockTextContent.BATTERY);
        Object holder=field(nativeSpan,"nativeBattery");check(holder!=null,"native fallback was not independently constructed");
        PrivateBattery privateBattery=(PrivateBattery)field(holder,"drawable");
        check(privateBattery!=meter.original&&privateBattery.batteryLevel==47&&privateBattery.chargeIconId==19,"fallback borrowed meter or lost snapshot/charging state");
        int sets=PrivateBattery.setLevels,charges=PrivateBattery.setCharges;
        for(int i=0;i<1000;i++)draw(nativeSpan,paint);
        check(PrivateBattery.created==originalCreates+1&&PrivateBattery.setLevels==sets&&PrivateBattery.setCharges==charges,"native draw created instance or set state each frame");
        check(privateBattery.boundsChanges==1&&PrivateBattery.palettes==1,"native draw repeated bound/palette update");
        check(meter.original.batteryLevel==91&&meter.original.progressColor==0xff00ff00,"fallback mutated real meter state");
        nativeSpan.close();nativeSpan.close();
        check(PrivateBattery.destroyed==1,"native font callback cleanup not exactly once");
        BigClockSystemIcon.Span reopened=BigClockSystemIcon.obtain(BigClockTextContent.BATTERY,BigClockSystemText.snapshot(),nativeSpan);
        check(reopened!=nativeSpan,"closed span reused");reopened.close();
        int draws=privateBattery.draws;draw(nativeSpan,paint);check(privateBattery.draws==draws,"closed span still drew");
        meter.candidate=new AlternateBattery(nativeOnly,0);meter.candidate.setBounds(0,0,116,68);BigClockSystemIcon.nativeBatteryChanged(meter);
        BigClockSystemIcon.Span alternate=icon(BigClockTextContent.BATTERY);
        check(field(field(alternate,"nativeBattery"),"drawable") instanceof AlternateBattery,"captured native style replaced with hard-coded horizontal class");alternate.close();
        meter.candidate=new VerticalBatteryContentDrawable(nativeOnly,0);meter.candidate.setBounds(0,0,40,68);BigClockSystemIcon.nativeBatteryChanged(meter);
        BigClockSystemIcon.Span vertical=icon(BigClockTextContent.BATTERY);draw(vertical,paint);
        Drawable verticalOwned=(Drawable)field(field(vertical,"nativeBattery"),"drawable");
        check(verticalOwned instanceof VerticalBatteryContentDrawable&&verticalOwned.getBounds().width()<verticalOwned.getBounds().height(),"native vertical battery stretched into horizontal box");vertical.close();
        CircleBatteryContentDrawable circleSource=new CircleBatteryContentDrawable(nativeOnly,0);circleSource.setBounds(0,0,68,68);meter.candidate=circleSource;
        check(BigClockSystemIcon.nativeBatteryChanged(meter),"circle native policy was discarded");
        BigClockSystemIcon.Span round=icon(BigClockTextContent.BATTERY);draw(round,paint);
        CircleBatteryContentDrawable roundOwned=(CircleBatteryContentDrawable)field(field(round,"nativeBattery"),"drawable");
        check(roundOwned!=circleSource&&roundOwned.getBounds().width()==roundOwned.getBounds().height(),"native circle was borrowed or stretched");
        check(roundOwned.mCircleFrontPaint.getColor()==0xff445566&&circleSource.mCircleFrontPaint.getColor()==0,"circle palette changed real source");
        round.close();check(roundOwned.destroyed,"circle native lifecycle not closed");
        Environment noService=new Environment();noService.moduleUnavailable=true;
        noService.nativeSource.available=Collections.singleton("stat_signal_noservice_lte_os17");
        settings.putBoolean("data_icon_enabled",false);BigClockSystemIcon.configure(noService,settings);
        BigClockSystemText.airplane(true);
        int noServiceId=noService.nativeSource.getIdentifier("stat_signal_noservice_lte_os17","drawable","com.android.systemui");
        BigClockSystemText.cellularIcons(noServiceId,"stat_signal_noservice_lte_os17",0,null);
        BigClockSystemIcon.Span absent=icon(BigClockTextContent.CELLULAR);
        check(field(absent,"graphic") instanceof Graphic,"native no-service token failed to create an owned glyph from the real resource name");
        check(noService.nativeSource.loads==1&&noService.nativeSource.last.containsKey("stat_signal_noservice_lte_os17"),"native no-service glyph used a fabricated level resource name");
        absent.close();
        BigClockSystemText.airplane(false);BigClockSystemText.simAbsent(true);
        for(String realName:new String[]{"stat_signal_noservice_lte","stat_signal_signal_null_lte","stat_signal_soft_noservice_os17"}) {
            Environment compatible=new Environment();compatible.moduleUnavailable=true;
            compatible.nativeSource.available=Collections.singleton(realName);BigClockSystemIcon.configure(compatible,settings);
            BigClockSystemText.cellularIcons(compatible.nativeSource.getIdentifier(realName,"drawable","com.android.systemui"),realName,0,null);
            BigClockSystemIcon.Span noSim=icon(BigClockTextContent.CELLULAR);
            check(field(noSim,"graphic") instanceof Graphic&&compatible.nativeSource.last.containsKey(realName),"native no-SIM fallback skipped exact resource "+realName);
            check(compatible.nativeSource.loads==1&&compatible.nativeSource.guarded,"native no-SIM factory borrowed a glyph or bypassed raw lookup guard");noSim.close();
        }
        View observer=new View(nativeOnly);BigClockSystemIcon.observe(observer);
        check(((Map<?,?>)field(IconManagementRuntime.class,"OWNERS")).containsKey(observer),"v3 invalidation did not observe real glyph view");
        int invalidations=observer.invalidations;v3.publish();check(observer.invalidations>invalidations,"real v3 provider publication did not invalidate the observed inline host");
        for(BigClockSystemIcon.Span span:new BigClockSystemIcon.Span[]{cell,nativeCell,single,wifi,nativeWifi,hidden,battery,anotherBattery,ios})span.close();
        BigClockSystemIcon.releaseRuntime();check(field(BigClockSystemIcon.class,"context")==null,"runtime retained Context");
        resetIcons();
        nativePresentation178();
        nativeSignals178();
        }
        System.out.println("BigClockSystemIconCheck: "+checks+" checks passed");
    }
}
