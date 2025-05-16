package org.nmcpye.datarun.acl;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import java.util.Set;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <16-05-2025>
 */
public class DataPermission extends BasePermission {
    public static final Permission DATA_READ = new DataPermission(1 << 5, 'V');
    public static final Permission DATA_WRITE = new DataPermission(1 << 6, 'A');
    public static final Permission DATA_UPDATE = new DataPermission(1 << 7, 'E');
    public static final Permission DATA_DELETE = new DataPermission(1 << 8, 'P');
    public static final Permission DATA_MANAGE = new DataPermission(1 << 9, 'M');


    public static final Set<Permission> PERMISSIONS = Set.of(DATA_READ,
        DATA_WRITE, DATA_UPDATE, DATA_DELETE, DATA_MANAGE);

    protected DataPermission(int mask, char code) {
        super(mask, code);
    }
}
