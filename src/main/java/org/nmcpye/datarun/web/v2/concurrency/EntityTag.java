package org.nmcpye.datarun.web.v2.concurrency;

/**
 * An entity tag (RFC 9110 section 8.8.3): an opaque tag, strong or weak. v2 sends strong tags only, one per
 * versioned resource: its version in quotes, {@code "3"}.
 *
 * @param tag  the opaque tag without its quotes
 * @param weak whether it was sent as {@code W/"..."}
 */
public record EntityTag(String tag, boolean weak) {

    public EntityTag {
        if (tag == null) {
            throw new IllegalArgumentException("An entity tag needs a tag");
        }
        for (int i = 0; i < tag.length(); i++) {
            if (!isTagChar(tag.charAt(i))) {
                throw new IllegalArgumentException("Not an entity tag character at " + i);
            }
        }
    }

    /** The strong tag of a resource at a version: {@code "3"}. */
    public static EntityTag ofVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("A version is never negative");
        }
        return new EntityTag(Long.toString(version), false);
    }

    /**
     * RFC 9110's strong comparison, which {@code If-Match} requires: both tags strong and their opaque tags equal.
     * A weak tag never matches.
     */
    public boolean strongMatches(EntityTag other) {
        return !weak && !other.weak && tag.equals(other.tag);
    }

    /** The tag as a header carries it: {@code "3"} or {@code W/"3"}. */
    @Override
    public String toString() {
        return (weak ? "W/" : "") + '"' + tag + '"';
    }

    /** etagc = %x21 / %x23-7E / obs-text: any visible character but the double quote. */
    static boolean isTagChar(char c) {
        return c == 0x21 || (c >= 0x23 && c <= 0x7E) || (c >= 0x80 && c <= 0xFF);
    }
}
