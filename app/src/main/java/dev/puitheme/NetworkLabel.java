// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.Locale;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class NetworkLabel {
    private static final Pattern SEPARATORS=Pattern.compile("[\\s_\\-]");
    /** Every generation the status bar can name. Each has its own configurable display text. */
    enum Level {
        G("g","G","2G"), E("e","E","2G"), ONE_X("1x","1x","2G"), TWO_G("2g","2G","2G"),
        THREE_G("3g","3G","3G"), H("h","H","3G"), H_PLUS("h_plus","H+","3G"),
        FOUR_G("4g","4G","4G"), FOUR_G_PLUS("4g_plus","4G+","4G"), FOUR_POINT_FIVE("4_5g","4.5G","4G"),
        FIVE_G("5g","5G","5G"), FIVE_G_PLUS("5g_plus","5G+","5G"), FIVE_G_A("5ga","5GA","5G"),
        FIVE_POINT_FIVE("5_5g","5.5G","5G"), FIVE_G_SA("5g_sa","5G SA","5G"), FIVE_G_NSA("5g_nsa","5G NSA","5G");
        final String id,text,family;
        Level(String id,String text,String family){this.id=id;this.text=text;this.family=family;}
    }

    private NetworkLabel() {
    }

    /** Alias recognition without collapsing enhanced generations; absent states are empty, unknown text is kept. */
    static String canonical(String str) {
        if (str == null) return "";
        String raw=str.trim(),token=token(raw);
        if(absent(token))return "";
        Level level=level(raw);return level==null?raw:level.text;
    }
    private static String token(String raw){return SEPARATORS.matcher(raw.toUpperCase(Locale.ROOT)).replaceAll("");}
    private static boolean absent(String value){return value.isEmpty()||value.equals("UNKNOWN")||value.equals("NONE")
            ||value.equals("NOSERVICE")||value.equals("OUTOFSERVICE")||value.equals("Sin servicio");}
    static Level level(String raw) {
        if(raw==null)return null;String value=token(raw.trim());
        if(value.startsWith("TIGO5G"))value=value.substring(4);
        switch(value) {
            case "G":case "GPRS":return Level.G;
            case "E":case "EDGE":return Level.E;
            case "1X":case "1XRTT":return Level.ONE_X;
            case "2":case "2G":case "GSM":case "CDMA":case "IDEN":return Level.TWO_G;
            case "3":case "3G":case "UMTS":case "WCDMA":case "TDSCDMA":case "EVDO":case "EVDO0":case "EVDOA":case "EVDOB":case "EHRPD":return Level.THREE_G;
            case "H":case "HSPA":case "HSDPA":case "HSUPA":return Level.H;
            case "H+":case "HPLUS":case "HSPA+":case "HSPAP":return Level.H_PLUS;
            case "4":case "4G":case "LTE":return Level.FOUR_G;
            case "4G+":case "4GPLUS":case "LTE+":case "LTEPLUS":case "LTEA":case "LTECA":case "LTEADVANCED":return Level.FOUR_G_PLUS;
            case "4.5G":case "LTEAPRO":case "LTEADVANCEDPRO":return Level.FOUR_POINT_FIVE;
            case "5":case "5G":case "NR":case "NR5G":case "5GBASIC":case "5GCONNECTED":case "5GNOTRESTRICTED":return Level.FIVE_G;
            case "5G+":case "5GPLUS":case "NR+":case "NRADVANCED":return Level.FIVE_G_PLUS;
            case "5GA":case "5GADVANCED":return Level.FIVE_G_A;
            case "5.5G":return Level.FIVE_POINT_FIVE;
            case "NRSA":case "5GSA":return Level.FIVE_G_SA;
            case "NRNSA":case "5GNSA":return Level.FIVE_G_NSA;
            default:return null;
        }
    }
    /** Android data RAT/display overrides. NR_ADVANCED is 5G+, not proof of 5G-A. */
    static String forDataNetwork(int type,int displayedType,int override) {
        boolean same=type==displayedType||(type==13||type==19)&&(displayedType==13||displayedType==19);
        return forDataNetwork(type,same?override:0);
    }
    static String forDataNetwork(int type,int override) {
        if(type==0||type==18)return "";
        if(type==13||type==19||type==20) {
            if(override==4||override==5)return "5G+";
            if(override==3)return "5G";
            if(type==20)return "5G";
            if(override==2)return "4.5G";
            if(override==1||type==19)return "4G+";
            return "4G";
        }
        switch(type) {
            case 1:return "G";case 2:return "E";case 7:return "1x";
            case 4:case 11:case 16:return "2G";
            case 3:case 5:case 6:case 12:case 14:case 17:return "3G";
            case 8:case 9:case 10:return "H";case 15:return "H+";
            default:return "";
        }
    }

    /** Unknown switch states are never interpreted as OFF. A connected route is not a switch. */
    static boolean radiosDisabled(Boolean wifiEnabled,Boolean mobileDataEnabled) {
        return Boolean.FALSE.equals(wifiEnabled)&&Boolean.FALSE.equals(mobileDataEnabled);
    }

    static String normalize(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        String strReplace = strTrim.toUpperCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (strReplace.isEmpty() || strReplace.equals("UNKNOWN") || strReplace.equals("NONE") || strReplace.equals("NOSERVICE") || strReplace.equals("OUTOFSERVICE") || strReplace.equals("Sin servicio")) {
            return "";
        }
        if (strReplace.startsWith("5G") || strReplace.startsWith("5.5G") || strReplace.startsWith("NR5G") || strReplace.startsWith("TIGO5G")) {
            return "5G";
        }
        if (strReplace.equals("NR") || strReplace.equals("NRSA") || strReplace.equals("NRNSA")
                || strReplace.equals("NR+")) {
            return "5G";
        }
        if (strReplace.startsWith("4G") || strReplace.startsWith("4.5G")
                || strReplace.startsWith("LTE") || strReplace.equals("4")) {
            return "4G";
        }
        if (strReplace.startsWith("3G") || strReplace.equals("3")
                || strReplace.equals("H") || strReplace.equals("H+")
                || strReplace.startsWith("HSPA") || strReplace.equals("UMTS")
                || strReplace.equals("WCDMA") || strReplace.equals("TDSCDMA")
                || strReplace.equals("TD-SCDMA") || strReplace.startsWith("EVDO")) {
            return "3G";
        }
        return strTrim;
    }
}
