package org.nmcpye.datarun.drun.postgres.common;


import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class IdentifiableObjectUtils {

    public static final String SEPARATOR = "-";

    public static final String SEPARATOR_JOIN = ", ";

    /**
     * Returns a list of uids for the given collection of IdentifiableObjects.
     *
     * @param objects the list of IdentifiableObjects.
     * @return a list of uids.
     */
    public static <T extends IdentifiableEntity> List<String> getUids(Collection<T> objects) {
        return objects != null ? objects.stream().filter(Objects::nonNull).map(o -> o.getUid()).collect(Collectors.toList()) : null;
    }
//
//    /**
//     * Returns a list of codes for the given collection of IdentifiableObjects.
//     *
//     * @param objects the list of IdentifiableObjects.
//     * @return a list of codes.
//     */
//    public static <T extends IdentifiableObject> List<String> getCodes(Collection<T> objects) {
//        return objects != null ? objects.stream().map(o -> o.getCode()).collect(Collectors.toList()) : null;
//    }

//    /**
//     * Returns a list of internal identifiers for the given collection of
//     * IdentifiableObjects.
//     *
//     * @param objects the list of IdentifiableObjects.
//     * @return a list of identifiers.
//     */
//    public static <T extends IdentifiableObject<Long>> List<Long> getIdentifiers(Collection<T> objects) {
//        return objects != null ? objects.stream().map(o -> o.getId()).collect(Collectors.toList()) : null;
//    }

//    /**
//     * Returns a map from internal identifiers to IdentifiableObjects, for the
//     * given collection of IdentifiableObjects.
//     *
//     * @param objects the collection of IdentifiableObjects
//     * @return a map from the object internal identifiers to the objects
//     */
//    public static <T extends IdentifiableObject<Long>> Map<Long, T> getIdentifierMap(Collection<T> objects) {
//        java.util.Map<Long, T> map = new java.util.HashMap<>();
//
//        for (T object : objects) {
//            map.put(object.getId(), object);
//        }
//
//        return map;
//    }

//    /**
//     * Removes duplicates from the given list while maintaining the order.
//     *
//     * @param list the list.
//     */
//    public static <T extends IdentifiableObject<Long>> List<T> removeDuplicates(List<T> list) {
//        final List<T> temp = new ArrayList<>(list);
//        list.clear();
//
//        for (T object : temp) {
//            if (!list.contains(object)) {
//                list.add(object);
//            }
//        }
//
//        return list;
//    }

//    /**
//     * Returns a mapping between the uid and the name of the given identifiable
//     * objects.
//     *
//     * @param objects the identifiable objects.
//     * @return mapping between the uid and the name of the given objects.
//     */
//    public static <T extends IdentifiableObject<Long>> Map<String, T> getUidObjectMap(Collection<T> objects) {
//        return objects != null ? Maps.uniqueIndex(objects, T::getUid) : Maps.newHashMap();
//    }
//
//    /**
//     * Converts the given {@link Set} to a mutable {@link List} and sorts the
//     * items by the ID property.
//     *
//     * @param <T>
//     * @param set the {@link Set}.
//     * @return a {@link List}.
//     */
//    public static <T extends IdentifiableObject<Long>> List<T> sortById(Set<T> set) {
//        List<T> list = new ArrayList<>(set);
//        Collections.sort(list, Comparator.comparingLong(T::getId));
//        return list;
//    }

//    /**
//     * Compare two {@link IdentifiableObject} using UID property.
//     *
//     * @param object object to compare.
//     * @param target object to compare with.
//     * @return TRUE if both objects are null or have same UID or both UIDs are
//     * null. Otherwise, return FALSE.
//     */
//    public static boolean equalByUID(IdentifiableObject object, IdentifiableObject target) {
//        if (ObjectUtils.allNotNull(object, target)) {
//            if (ObjectUtils.allNotNull(object.getUid(), target.getUid())) {
//                return object.getUid().equals(target.getUid());
//            }
//
//            return ObjectUtils.allNull(object.getUid(), target.getUid());
//        }
//
//        return ObjectUtils.allNull(object, target);
//    }
}
