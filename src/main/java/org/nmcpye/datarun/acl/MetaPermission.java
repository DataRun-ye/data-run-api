package org.nmcpye.datarun.acl;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import java.util.Set;

/**
 * {@code READ (1)}
 * {@code WRITE (2)}
 * {@code UPDATE (4)}
 * {@code DELETE (8)}
 * {@code ADMINISTRATION (16)}
 *
 * @author Hamza Assada, <7amza.it@gmail.com> <16-05-2025>
 */
public class MetaPermission extends BasePermission {
    // bit 0 → mask = 1, code = 'R' (or any unique char)
    public static final Permission READ = new MetaPermission(1 << 0, 'R');   // 1
    // bit 1 → mask = 2, code = 'W'
    public static final Permission WRITE = new MetaPermission(1 << 1, 'W');  // 2
    // bit 2 → mask = 4, code = 'C'
    public static final Permission UPDATE = new MetaPermission(1 << 2, 'U'); // 4
    // bit 4 → mask = 8, code = 'D'
    public static final Permission DELETE = new MetaPermission(1 << 3, 'D'); // 8
    // bit 8 → mask = 16, code = 'A' (admin)
    public static final Permission MANAGE = new MetaPermission(1 << 4, 'M'); // 16

    public static final Set<Permission> PERMISSIONS = Set.of(READ, WRITE, UPDATE, DELETE, MANAGE);

    protected MetaPermission(int mask, char code) {
        super(mask, code);
    }
}
