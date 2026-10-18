// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/** Audited business identity only: no notification text, instance keys or card payloads are read. */
public final class FluidCloudIdentity {
    private static final String VIEW_ITEM = "com.oplus.systemui.plugins.seedling.card.ui.model.o";
    private static final String ENTRY = "com.android.systemui.plugins.custom.CustomRowEntry";
    private static final String SEEDLING_ENTRY = "com.oplus.systemui.plugins.seedling.notification.data.model.c";
    private static final String CARD_REPOSITORY = "com.oplus.systemui.plugins.seedling.card.data.repo.d";
    private static final String INTERACTOR = "com.oplus.systemui.plugins.seedling.data.interactor.n";
    private static final String MODEL = "com.oplus.systemui.plugins.livealert.lib.m";
    private static final Contract UNAVAILABLE = new Contract(null, null, null, null);
    private static final Map<Class<?>, WeakReference<Contract>> CONTRACTS = new WeakHashMap<>();
    public static final Identity UNKNOWN = new Identity("", "");

    private FluidCloudIdentity() { }

    public static final class Identity {
        public final String serviceId, packageName;
        private Identity(String serviceId, String packageName) {
            this.serviceId = serviceId == null ? "" : serviceId;
            this.packageName = packageName == null ? "" : packageName;
        }
    }

    /** Read at CardView.h(ViewItem) binding time, before the native background initializer runs. */
    public static Identity fromViewItem(Object item) {
        if (item == null || !VIEW_ITEM.equals(item.getClass().getName())) return UNKNOWN;
        Contract contract = contract(item.getClass());
        if (contract.serviceField == null) return UNKNOWN;
        try {
            String service = (String) contract.serviceField.get(item);
            String packageName = "";
            // The optional repository chain is independent: a changed ROM may still expose the audited service field.
            if (contract.source != null) try { packageName = contract.source.businessPackage(item); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
            return identity(service, packageName);
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return UNKNOWN; }
    }

    /** The notification/lock-screen entry exposes its own service and publishing package. */
    public static Identity fromEntry(Object entry) {
        if (entry == null || !entryType(entry.getClass())) return UNKNOWN;
        Contract contract = contract(entry.getClass());
        if (contract.serviceGetter == null || contract.packageGetter == null) return UNKNOWN;
        try {
            return identity((String) contract.serviceGetter.invoke(entry), (String) contract.packageGetter.invoke(entry));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return UNKNOWN; }
    }

    private static Identity identity(String service, String packageName) {
        return (service == null || service.isEmpty()) && (packageName == null || packageName.isEmpty())
                ? UNKNOWN : new Identity(service, packageName);
    }

    /** Cache audited native members. Weak values avoid retaining a disposed plugin class loader through its members. */
    private static Contract contract(Class<?> type) {
        synchronized (CONTRACTS) {
            WeakReference<Contract> cached = CONTRACTS.get(type);
            Contract contract = cached == null ? null : cached.get();
            if (contract != null) return contract;
            try {
                if (VIEW_ITEM.equals(type.getName())) {
                    Field service = type.getDeclaredField("b");
                    if (service.getType() != String.class || Modifier.isStatic(service.getModifiers())
                            || !Modifier.isFinal(service.getModifiers()) || !Modifier.isFinal(type.getModifiers()))
                        throw new NoSuchFieldException("Live Alert ViewItem identity changed");
                    service.setAccessible(true);
                    PackageSource source = null;
                    try { source = packageSource(type); }
                    catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
                    contract = new Contract(service, null, null, source);
                } else if (entryType(type)) {
                    Class<?> owner = entryOwner(type);
                    Method service = entryGetter(owner, "getServiceId");
                    Method packageName = entryGetter(owner, "getPackageName");
                    contract = new Contract(null, service, packageName, null);
                } else contract = UNAVAILABLE;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                contract = UNAVAILABLE;
            }
            CONTRACTS.put(type, new WeakReference<>(contract));
            return contract;
        }
    }

    private static Method getter(Class<?> type, String name) throws ReflectiveOperationException {
        Method result = type.getDeclaredMethod(name);
        if (result.getReturnType() != String.class || Modifier.isStatic(result.getModifiers())
                || !Modifier.isPublic(result.getModifiers())) throw new NoSuchMethodException(name);
        result.setAccessible(true);
        return result;
    }

    private static boolean entryType(Class<?> type) {
        return ENTRY.equals(type.getName()) || SEEDLING_ENTRY.equals(type.getName());
    }

    /** Audited SystemUIPlugin classes.dex: notification.data.model.c is PUBLIC FINAL and directly extends
     * CustomRowEntry. Its constructor at 0x1ed4fc delegates identity parameters to that native base.
     * SystemUI classes2.dex declares both identity getters as PUBLIC FINAL ()String at 0x222c14/0x222c44.
     * Resolve those inherited final methods on the verified base, never on an arbitrary subclass. */
    private static Class<?> entryOwner(Class<?> type) throws ReflectiveOperationException {
        if (ENTRY.equals(type.getName())) return type;
        Class<?> parent = type.getSuperclass();
        if (!SEEDLING_ENTRY.equals(type.getName()) || !Modifier.isFinal(type.getModifiers())
                || parent == null || !ENTRY.equals(parent.getName()))
            throw new NoSuchMethodException("Live Alert concrete notification entry changed");
        return parent;
    }

    private static Method entryGetter(Class<?> type, String name) throws ReflectiveOperationException {
        Method method = getter(type, name);
        if (!Modifier.isFinal(method.getModifiers())) throw new NoSuchMethodException(name);
        return method;
    }

    /** ViewItem.d -> CardRepository.a -> Interactor.a -> BaseModel.e(): the original business package, not card text. */
    private static PackageSource packageSource(Class<?> itemType) throws ReflectiveOperationException {
        Field repository = sourceField(itemType, "d", CARD_REPOSITORY);
        Field interactor = sourceField(repository.getType(), "a", INTERACTOR);
        Field model = sourceField(interactor.getType(), "a", MODEL);
        if (!model.getType().isInterface()) throw new NoSuchMethodException("Live Alert base model changed");
        Method business = getter(model.getType(), "e");
        return new PackageSource(repository, interactor, model, business);
    }

    private static Field sourceField(Class<?> type, String name, String valueType) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        if (!valueType.equals(field.getType().getName()) || Modifier.isStatic(field.getModifiers())
                || !Modifier.isFinal(field.getModifiers())) throw new NoSuchFieldException(name);
        field.setAccessible(true);
        return field;
    }

    private static final class PackageSource {
        final Field repository, interactor, model;
        final Method business;
        PackageSource(Field repository, Field interactor, Field model, Method business) {
            this.repository = repository; this.interactor = interactor; this.model = model; this.business = business;
        }
        String businessPackage(Object item) throws ReflectiveOperationException {
            Object repo = repository.get(item);
            if (repo == null) return "";
            Object owner = interactor.get(repo);
            if (owner == null) return "";
            Object source = model.get(owner);
            return source == null ? "" : (String) business.invoke(source);
        }
    }

    private static final class Contract {
        final Field serviceField;
        final Method serviceGetter, packageGetter;
        final PackageSource source;
        Contract(Field field, Method service, Method packageName, PackageSource source) {
            serviceField = field; serviceGetter = service; packageGetter = packageName; this.source = source;
        }
    }
}
