package dev.puitheme;

import com.android.systemui.plugins.custom.CustomRowEntry;
import com.oplus.systemui.plugins.seedling.card.ui.model.o;
import com.oplus.systemui.plugins.seedling.card.data.repo.d;
import com.oplus.systemui.plugins.seedling.data.interactor.n;

/** Native service identities, reused contracts, incompatible owners and payload privacy. */
public final class FluidCloudIdentityCheck {
    private static int checks;

    private static void check(boolean value, String why) {
        checks++; if (!value) throw new AssertionError(why);
    }
    private static void identity(FluidCloudIdentity.Identity identity, String service, String packageName, String why) {
        check(service.equals(identity.serviceId) && packageName.equals(identity.packageName), why);
    }

    public static final class UnrelatedItem {
        public final String b = "untrusted";
    }
    public static final class UnrelatedEntry {
        public String getServiceId() { throw new AssertionError("Unrelated getters must not be called"); }
        public String getPackageName() { throw new AssertionError("Unrelated getters must not be called"); }
    }

    public static void main(String[] args) {
        identity(FluidCloudIdentity.fromViewItem(null), "", "", "null item is unknown");
        identity(FluidCloudIdentity.fromEntry(null), "", "", "null entry is unknown");
        identity(FluidCloudIdentity.fromViewItem(new UnrelatedItem()), "", "", "matching fields on an unrelated class are rejected");
        identity(FluidCloudIdentity.fromEntry(new UnrelatedEntry()), "", "", "matching getters on an unrelated class are rejected");

        o first = new o("test.timer");
        for (int i = 0; i < 40; i++)
            identity(FluidCloudIdentity.fromViewItem(first), "test.timer", "", "cached native item contract " + i);
        identity(FluidCloudIdentity.fromViewItem(new o("test.alarm")), "test.alarm", "", "one native class can bind another business");
        identity(FluidCloudIdentity.fromViewItem(new o(null)), "", "", "null service remains unknown");
        identity(FluidCloudIdentity.fromViewItem(new o()), "", "", "legacy fixture with no identity remains unknown");
        identity(FluidCloudIdentity.fromViewItem(new o("test.timer",new d(new n(()->"test.clock")))), "test.timer", "test.clock", "original business package comes from the audited model link");
        identity(FluidCloudIdentity.fromViewItem(new o("test.timer",new d(null))), "test.timer", "", "missing interactor preserves the service");
        identity(FluidCloudIdentity.fromViewItem(new o("test.timer",new d(new n(null)))), "test.timer", "", "missing source preserves the service");
        identity(FluidCloudIdentity.fromViewItem(new o("test.timer",new d(new n(()->null)))), "test.timer", "", "null business package preserves the service");
        identity(FluidCloudIdentity.fromViewItem(new o("test.timer",new d(new n(()->{throw new IllegalStateException("Disposed model");})))), "test.timer", "", "unavailable optional source preserves the service");

        CustomRowEntry row = new CustomRowEntry("test.pickup", "test.publisher");
        identity(FluidCloudIdentity.fromEntry(row), "test.pickup", "test.publisher", "audited entry reads service and publishing package");
        check(row.keyReadsForCheck == 0 && row.extrasReadsForCheck == 0, "instance key and card extras are never read");
        identity(FluidCloudIdentity.fromEntry(new CustomRowEntry(null, "test.publisher")), "", "test.publisher", "publisher survives an absent service");
        identity(FluidCloudIdentity.fromEntry(new CustomRowEntry("test.timer", null)), "test.timer", "", "service survives an absent publisher");
        identity(FluidCloudIdentity.fromEntry(new CustomRowEntry()), "", "", "legacy entry fixture remains compatible");
        row.identityUnavailableForCheck = true;
        identity(FluidCloudIdentity.fromEntry(row), "", "", "disposed native entry keeps native behavior");
        identity(FluidCloudIdentity.fromViewItem(row), "", "", "notification entry cannot masquerade as a floating item");
        identity(FluidCloudIdentity.fromEntry(first), "", "", "floating item cannot masquerade as a notification entry");
        identity(FluidCloudIdentity.fromEntry(new CustomRowEntry("test.timer", "test.publisher") { }), "", "", "unverified entry subclasses remain unknown");
        com.oplus.systemui.plugins.seedling.notification.data.model.c nativeRow=
                new com.oplus.systemui.plugins.seedling.notification.data.model.c("268451854","com.coloros.alarmclock");
        identity(FluidCloudIdentity.fromEntry(nativeRow),"268451854","com.coloros.alarmclock","audited final native subtype reads its inherited base identity");
        check(nativeRow.keyReadsForCheck==0&&nativeRow.extrasReadsForCheck==0,"concrete native subtype never reads instance keys or card payloads");
        identity(FluidCloudIdentity.fromEntry(new com.oplus.systemui.plugins.seedling.notification.data.model.c("268451917","test.clock")),"268451917","test.clock","cached concrete subtype contract does not retain the prior business");
        identity(FluidCloudIdentity.fromEntry(new com.oplus.systemui.plugins.seedling.notification.data.model.c(null,null)),"","","concrete native subtype with missing identity stays unknown");
        nativeRow.identityUnavailableForCheck=true;
        identity(FluidCloudIdentity.fromEntry(nativeRow),"","","disposed native subtype keeps native behavior");
        System.out.println("FluidCloudIdentityCheck: " + checks + " checks passed");
    }
}
