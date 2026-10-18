// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.*;

/** Fixed data identities. A package can supply artwork but can never register native lookup rules. */
public final class IconRoleRegistry {
    public static final String[] SCOPES={"hint","wifi","cellular","battery","tile","slider","media","device","footer"};
    public static final class Target{
        public final String id,title,scope,reason;public final List<String> roles;public final Map<String,String> stateLabels;
        public final boolean supported,dynamicSupported;
        private Target(String id,String title,String scope,boolean supported,String reason,boolean dynamic,String... states){
            this.id=id;this.title=title;this.scope=scope;this.supported=supported;this.reason=reason;dynamicSupported=dynamic;
            Map<String,String> labels=new LinkedHashMap<>();List<String> roles=new ArrayList<>();
            for(int i=0;i<states.length;i+=2){roles.add(id+'.'+states[i]);labels.put(states[i],states[i+1]);}
            this.roles=Collections.unmodifiableList(roles);stateLabels=Collections.unmodifiableMap(labels);
        }
    }
    public static final class Role{
        public final String id,targetId,state,title,scope;public final boolean supported;
        private Role(Target target,String state){id=target.id+'.'+state;targetId=target.id;this.state=state;title=target.title+" · "+target.stateLabels.get(state);scope=target.scope;supported=target.supported;}
    }
    private static final Map<String,Target> TARGETS=new LinkedHashMap<>();private static final Map<String,Role> ROLES=new LinkedHashMap<>();
    private static final Map<String,String> TILE_SPECS=new LinkedHashMap<>();
    static{
        for(String slot:NativeStatusIcons.PRIORITY_SLOTS){String title=NativeStatusIcons.priorityLabel(slot);
            if("bluetooth".equals(slot))add(new Target("hint."+slot,title,"hint",true,"",false,"on","已连接","off","未连接"));
            else add(new Target("hint."+slot,title,"hint",true,"",false,"default","系统显示状态"));
        }
        add(new Target("wifi","Wi-Fi","wifi",true,"",false,"none","未连接","0","零格","1","一格","2","两格","3","三格","4","满格"));
        add(new Target("cellular","蜂窝信号","cellular",true,"",false,"none","未连接","0","零格","1","一格","2","两格","3","三格","4","满格"));
        for(String row:new String[]{"primary","secondary"})add(new Target("cellular.stacked_"+row,
                "primary".equals(row)?"双排信号 · 主卡上排":"双排信号 · 副卡下排","cellular",true,
                "保持共同画布中的上下排位置，不将每排单独放大",false,"none","无服务","0","零格","1","一格","2","两格","3","三格","4","满格"));
        for(String id:new String[]{"battery","battery.charging"}){String[] levels=new String[22];for(int i=0;i<=10;i++){levels[i*2]=Integer.toString(i*10);levels[i*2+1]=(i*10)+"%";}add(new Target(id,id.equals("battery")?"电池外形":"充电电池外形","battery",true,"",false,levels));}
        // Exact stock specs and state contracts: ICON-V3-QS-STATE-CONTRACT.zh-CN.md.
        for(String[] tile:new String[][]{{"wifi","wifi","Wi-Fi"},{"bluetooth","bt","蓝牙"},{"mobile_data","cell","移动数据"},{"hotspot","hotspot","个人热点"},{"airplane","airplane","飞行模式"},{"nfc","nfc","NFC"},{"flashlight","flashlight","手电筒"},{"rotation","rotation","屏幕旋转"},{"dark","darkmode","深色模式"},{"location","location","定位"},{"camera","cameratoggle","相机权限"},{"microphone","mictoggle","麦克风权限"}}){
            String off="关闭",on="开启";if(tile[0].equals("camera")||tile[0].equals("microphone")){off="阻止访问";on="允许访问";}else if(tile[0].equals("rotation")){off="旋转锁定";on="自动旋转";}else if(tile[0].equals("dark")){off="浅色模式";on="深色模式";}
            tile(tile[0],tile[1],tile[2],true,true,"off",off,"on",on,"unavailable","不可用");
        }
        add(new Target("tile.cast","投屏","tile",false,"当前 stock factory 无此 spec，保留系统图案",false,"off","关闭","on","开启","unavailable","不可用"));
        for(String[] tile:new String[][]{{"auto_brightness","autobrightness","自动亮度"},{"auto_color_temperature","autoscreentemperature","自动色温"},{"data_saver","saver","流量节省"},{"dolby","dobly","杜比音效"},{"breathing_light","breathinglight","呼吸灯"},{"work","work","工作资料"},{"osie","osie","OSIE 图像增强"},{"iris","lris","视频增强"},{"vpn","vpn","VPN 状态"},{"otg","otg","OTG"},{"dnd","dnd","勿扰模式"},{"no_disturb","nodisturb","原生免打扰"},{"battery_saver","battery","省电模式"},{"phone_anti_theft","phoneantitheft","防盗报警"},{"super_power_saver","superpowersave","超级省电模式"},{"vibration","vibration","当前铃声模式的振动"},{"small_screen","smallscreen","单手小屏"},{"envelope","envelope","红包助手"}}){
            boolean unavailable=Arrays.asList("iris","super_power_saver","small_screen").contains(tile[0]);
            tile(tile[0],tile[1],tile[2],true,false,unavailable?new String[]{"off","关闭","on","开启","unavailable","不可用"}:new String[]{"off","关闭","on","开启"});
        }
        for(String[] tile:new String[][]{{"alipay","alipay","支付宝付款"},{"ali_scan","aliscan","支付宝扫一扫"},{"wechat_pay","weixinpay","微信付款"},{"wechat_scan","weixinscan","微信扫一扫"},{"calculator","calculator","计算器"},{"clean","clear","清理"},{"google_now","googlenow","助手入口"},{"lock_screen","lockscreen","锁屏"},{"input_method","inputmethod","输入法选择"},{"shutdown","shutdown","电源操作"},{"qr_scanner","qr_code_scanner","系统扫码"},{"multi_system","multisystem","系统切换"},{"screenshot","screenshot","截屏"}})
            tile(tile[0],tile[1],tile[2],true,false,"default","操作入口");
        tile("ringer_mode","ringermode","铃声模式",true,false,"silent","静音","vibrate","振动","ring","响铃");
        tile("screen_record","screenrecord","录屏",true,false,"unavailable","不可用","idle","可开始录屏","recording","录屏活动");
        tile("controls","controls","设备控制",false,false,"default","系统服务入口");
        tile("wallet","wallet","钱包",false,false,"default","系统卡片与服务");
        add(new Target("slider.volume.normal","音量 · 本机扬声器","slider",true,"",false,"mute","静音","low","低音量","medium","中音量","high","高音量"));
        add(new Target("slider.volume.wired","音量 · 有线设备","slider",true,"",false,"mute","静音","unmute","有音量"));
        add(new Target("slider.volume.bluetooth","音量 · 蓝牙设备","slider",true,"",false,"mute","静音","unmute","有音量"));
        add(new Target("slider.brightness","亮度主图形","slider",true,"",false,"default","当前亮度"));
        add(new Target("slider.brightness_auto","自动亮度标记","slider",true,"仅在系统实际自动亮度状态显示",false,"default","自动模式"));
        for(String[] footer:new String[][]{{"settings","设置"},{"more","更多"}})add(new Target("footer."+footer[0],footer[1],"footer",true,"",false,"default","入口"));
        add(new Target("media.play_pause","媒体播放控制","media",true,"仅标准播放语义的静态末态，保留系统媒体动画",false,"play","播放","pause","暂停"));
        for(String[] media:new String[][]{{"previous","上一首"},{"next","下一首"},{"output","音频输出"}})add(new Target("media."+media[0],media[1],"media",true,"仅已核对的标准操作与默认输出分支，保留自定义动作和设备图",false,"default","系统标准操作"));
        add(new Target("device.entrance","设备控制入口","device",true,"仅系统连接设备入口",false,"default","入口"));
        for(String[] device:new String[][]{{"phone","手机"},{"pad","平板"},{"computer","电脑"},{"headset","耳机"},{"tv","电视"},{"watch","手表"},{"wristband","手环"}})add(new Target("device."+device[0],device[1],"device",true,"仅类型、空照片来源与当前原生默认资源均匹配的通用图形，保留品牌图",false,"default","系统通用图形"));
    }
    private static void add(Target target){TARGETS.put(target.id,target);for(String state:target.stateLabels.keySet()){Role role=new Role(target,state);ROLES.put(role.id,role);}}
    private static void tile(String id,String spec,String title,boolean supported,boolean dynamic,String... states){String target="tile."+id;add(new Target(target,title,"tile",supported,supported?"":"复杂服务或卡片入口保留原生图案",dynamic,states));TILE_SPECS.put(spec,target);}
    /** Only the audited OEM factory identities; aliases, labels and third party custom specs never match. */
    public static String tileTarget(String spec){return TILE_SPECS.get(spec);}
    public static List<Target> targets(String scope){List<Target> result=new ArrayList<>();for(Target target:TARGETS.values())if(scope==null||target.scope.equals(scope))result.add(target);return Collections.unmodifiableList(result);}
    public static Target target(String id){return TARGETS.get(id);}
    public static Role role(String id){return ROLES.get(id);}
    /** These two native layers share one viewport; their transparent space separates the rows. */
    public static boolean sharedSignalCanvas(String role){Role found=ROLES.get(role);return found!=null&&
            (found.targetId.equals("cellular.stacked_primary")||found.targetId.equals("cellular.stacked_secondary"));}
    public static boolean validScope(String scope){return Arrays.asList(SCOPES).contains(scope);}
    public static String scopeTitle(String scope){String[] titles={"系统提示","Wi-Fi","蜂窝信号","电池","控制中心磁贴","滑块","媒体控制","设备图标","页脚入口"};for(int i=0;i<SCOPES.length;i++)if(SCOPES[i].equals(scope))return titles[i];throw new IllegalArgumentException("未知图标范围");}
    public static String scopeOfRole(String role){Target target=targetForRole(role);if(target!=null)return target.scope;int end=role==null?-1:role.indexOf('.');return end>0&&validScope(role.substring(0,end))?role.substring(0,end):null;}
    public static Target targetForRole(String role){
        if(role==null)return null;Target longest=null;for(Target target:TARGETS.values())if(role.startsWith(target.id+'.')&&(longest==null||target.id.length()>longest.id.length()))longest=target;
        if(longest==null&&role.startsWith("hint."))longest=TARGETS.get(role);return longest;
    }
    public static boolean validResourceRole(String role){return role!=null&&role.length()<=128&&role.matches("(?:hint|wifi|cellular|battery|tile|slider|media|device|footer)(?:\\.[a-z0-9][a-z0-9_]{0,47}){1,5}");}
    public static boolean knownResourceRole(String role){return ROLES.containsKey(role)||role!=null&&role.startsWith("hint.")&&TARGETS.containsKey(role);}
    public static boolean validKnownState(String role){Target target=targetForRole(role);return target==null||ROLES.containsKey(role)||target.id.equals(role)&&"hint".equals(target.scope);}
    public static List<String> roles(String scope){List<String> result=new ArrayList<>();for(Target target:targets(scope))result.addAll(target.roles);return Collections.unmodifiableList(result);}
    private IconRoleRegistry(){ }
}
