package org.jdesktop.el.impl.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Private BeansBinding BeanInfo cache. Uses WeakHashMap and SoftReference for
 * proper garbage collection. Allows for IGNORE_ALL_BEANINFO to be used while
 * still providing a BeanInfo cache. Resolves the issue of other popular
 * frameworks clearing the JVM Introspector cache negating the performance
 * benefits of caching.
 *
 * @author Robert Stone
 */
public final class CachingIntrospector {

    private static final Map<Class<?>, SoftReference<BeanInfo>> BEANINFO_CACHE = new WeakHashMap<Class<?>, SoftReference<BeanInfo>>();

    public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        SoftReference<BeanInfo> softRef = BEANINFO_CACHE.get(beanClass);
        BeanInfo beanInfo;
        /* Introspect the bean or get the BeanInfo from the soft reference */
        if (softRef == null) {
            beanInfo = introspectBeanInfo(beanClass);
        } else {
            beanInfo = softRef.get();
        }
        /* If the soft reference value was garbage collected the beanInfo will be null */
        if (beanInfo == null) {
            beanInfo = introspectBeanInfo(beanClass);
        }
        return beanInfo;
    }

    private static BeanInfo introspectBeanInfo(Class<?> beanClass) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO);
        BEANINFO_CACHE.put(beanClass, new SoftReference<BeanInfo>(beanInfo));
        Class<?> flushClass = beanClass;
        do {
            /**
             * Flush the Introspector cache. This is normally not populated with
             * IGNORE_ALL_BEANINFO set. Flush anyways, just in case that bug is
             * fixed in the future.
             */
            Introspector.flushFromCaches(flushClass);
            flushClass = flushClass.getSuperclass();
        } while (flushClass != null);
        return beanInfo;
    }

    public static void flushCache() {
        BEANINFO_CACHE.clear();
    }
}
