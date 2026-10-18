// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Business selection for the style page. Unknown businesses retain the old behaviour by default. */
final class FluidCloudScope {
    enum Category {
        MEDIA("media"), RIDE_HAILING("ride_hailing"), FOOD_DELIVERY("food_delivery"),
        PARCEL_PICKUP("parcel_pickup"), FOOD_PICKUP("food_pickup"), TIMER("timer"),
        ALARM("alarm"), NAVIGATION("navigation"), TRAVEL("travel"), BATTERY("battery"),
        EARPHONES("earphones"), SMART_SUGGESTIONS("smart_suggestions"), OTHER("other");
        final String id, key;
        Category(String id) { this.id=id;this.key="fluid_cloud_scope_"+id+"_enabled"; }
    }
    static final Map<String,Boolean> BOOLEANS;
    private static final int ALL=(1<<Category.values().length)-1;
    static {
        Map<String,Boolean> values=new LinkedHashMap<>();
        for(Category category:Category.values())values.put(category.key,true);
        BOOLEANS=Collections.unmodifiableMap(values);
    }
    static boolean isScopeKey(String key){return BOOLEANS.containsKey(key);}

    /** IDs audited from Clock/ExSystemService UPK manifests, UMS registries and the SystemUI media controller.
     * Service IDs are global business identifiers; publishers such as UMS and Meituan serve several businesses.
     * Never infer a pickup code from its displayed text or classify an entire multipurpose publisher as food delivery.
     */
    private static final Map<String,Category> SERVICES;
    static {
        Map<String,Category> services=new LinkedHashMap<>();
        services.put("268451854",Category.TIMER);services.put("268451943",Category.TIMER);
        services.put("268451917",Category.ALARM);services.put("268451944",Category.ALARM);
        services.put("268451937",Category.ALARM);
        services.put("268451953",Category.ALARM);
        services.put("268452006",Category.MEDIA);
        services.put("default_com.android.systemui_268452006",Category.MEDIA);
        services.put("268451900",Category.MEDIA);services.put("268451910",Category.MEDIA);services.put("268451911",Category.MEDIA);
        services.put("536875051",Category.RIDE_HAILING);services.put("536877203",Category.RIDE_HAILING);services.put("536877202",Category.RIDE_HAILING);
        services.put("536875052",Category.FOOD_DELIVERY);services.put("536877201",Category.FOOD_DELIVERY);
        services.put("536878642",Category.FOOD_DELIVERY);services.put("536878973",Category.FOOD_DELIVERY);
        services.put("536875050",Category.FOOD_PICKUP);
        services.put("268451931",Category.FOOD_PICKUP);
        services.put("536878265",Category.PARCEL_PICKUP);services.put("536878933",Category.PARCEL_PICKUP);services.put("268452041",Category.PARCEL_PICKUP);
        services.put("268452031",Category.NAVIGATION);services.put("536879377",Category.NAVIGATION);services.put("536879383",Category.NAVIGATION);services.put("536879312",Category.NAVIGATION);
        services.put("536871053",Category.TRAVEL);services.put("536871054",Category.TRAVEL);
        services.put("536871055",Category.TRAVEL);services.put("536871056",Category.TRAVEL);services.put("536879177",Category.TRAVEL);
        services.put("268451902",Category.BATTERY);services.put("268451924",Category.BATTERY);
        services.put("268451845",Category.EARPHONES);
        services.put("268451855",Category.SMART_SUGGESTIONS);
        services.put("536874238",Category.SMART_SUGGESTIONS);services.put("536879148",Category.SMART_SUGGESTIONS);
        services.put("536874236",Category.SMART_SUGGESTIONS);services.put("536879151",Category.SMART_SUGGESTIONS);services.put("536874237",Category.SMART_SUGGESTIONS);
        SERVICES=Collections.unmodifiableMap(services);
    }
    private static final java.util.regex.Pattern IDENTIFIER=java.util.regex.Pattern.compile("[A-Za-z0-9_.:-]{1,160}");
    static String diagnosticId(String value){return value!=null&&value.length()<=160&&IDENTIFIER.matcher(value).matches()?value:"unavailable";}
    static Category classify(String serviceId,String packageName,boolean nativeMedia){
        String id=serviceId==null?"":serviceId;
        if(id.length()>160)return nativeMedia?Category.MEDIA:Category.OTHER;
        Category known=SERVICES.get(id);
        return known!=null?known:nativeMedia?Category.MEDIA:Category.OTHER;
    }

    static final class Policy {
        static final Policy DEFAULT=new Policy(ALL);
        private final int mask;
        private Policy(int mask){this.mask=mask;}
        static Policy from(Bundle settings){
            int mask=ALL;
            if(settings!=null)for(Category category:Category.values())
                if(Boolean.FALSE.equals(settings.get(category.key)))mask&=~(1<<category.ordinal());
            return mask==ALL?DEFAULT:new Policy(mask);
        }
        boolean allows(Category category){return (mask&(1<<(category==null?Category.OTHER:category).ordinal()))!=0;}
        @Override public boolean equals(Object other){return other instanceof Policy&&mask==((Policy)other).mask;}
        @Override public int hashCode(){return mask;}
    }
    private FluidCloudScope(){ }
}
