package fr.ecp.sio.appenginedemo.model;

import com.google.appengine.repackaged.com.google.common.base.Function;
import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.googlecode.objectify.Ref;

import java.util.List;

/**
 * Created by cquenum on 29/01/2016.
 */
public class Deref {
    public static <T> T deref(Ref<T> ref) {
        return ref == null ? null : ref.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> List<T> deref(List<Ref<T>> reflist) {
        return Lists.transform(reflist, (Func) Func.INSTANCE);
    }

    public static class Func<T> implements Function<Ref<T>, T> {
        public static Func<Object> INSTANCE = new Func<Object>();

        @Override
        public T apply(Ref<T> ref) {
            return deref(ref);
        }
    }
}