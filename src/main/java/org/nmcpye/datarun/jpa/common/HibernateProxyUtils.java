package org.nmcpye.datarun.jpa.common;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.util.Objects;

@Slf4j
public class HibernateProxyUtils {

    private HibernateProxyUtils() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("rawtypes")
    public static Class getRealClass(Object object) {
        Objects.requireNonNull(object);

        if (object instanceof Class) {
            throw new IllegalArgumentException("Input can't be a Class instance!");
        }

        return getClassWithoutInitializingProxy(object);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T unproxy(T proxy) {
        return (T) Hibernate.unproxy(proxy);
    }

    public static <T> void initializeAndUnproxy(T entity) {
        if (entity == null) {
            return;
        }

        Hibernate.initialize(entity);
        if (entity instanceof HibernateProxy) {
            ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        }
    }

    public static Class getClassWithoutInitializingProxy(Object object) {
        if (object instanceof HibernateProxy) {
            HibernateProxy proxy = (HibernateProxy) object;
            LazyInitializer li = proxy.getHibernateLazyInitializer();
            return li.getPersistentClass();
        } else {
            return object.getClass();
        }
    }
}
