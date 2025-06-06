package org.nmcpye.datarun.acl;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import java.util.Set;

/**
 * @author Hamza Assada 16/05/2025 <7amza.it@gmail.com>
 */
public class DataPermission extends BasePermission {
    public static final Permission DATA_READ = new DataPermission(1 << 5, 'r');   // 32
    public static final Permission DATA_WRITE = new DataPermission(1 << 6, 'w');  // 64
    public static final Permission DATA_UPDATE = new DataPermission(1 << 7, 'u'); // 128
    public static final Permission DATA_DELETE = new DataPermission(1 << 8, 'd'); // 256
    public static final Permission DATA_MANAGE = new DataPermission(1 << 9, 'm'); // 512


    public static final Set<Permission> PERMISSIONS = Set.of(DATA_READ,
        DATA_WRITE, DATA_UPDATE, DATA_DELETE, DATA_MANAGE);

    protected DataPermission(int mask, char code) {
        super(mask, code);
    }
}
