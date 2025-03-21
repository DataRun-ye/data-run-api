package org.nmcpye.datarun.useraccess;

import java.util.Collection;
import java.util.Set;

/**
 * @author Hamza, 21/03/2025
 */
public interface userAccessInterface {
    String getLogin();
    boolean isSuper();
    boolean isAuthorized(String auth);
    boolean hasAnyAuthority(Collection<String> auths);
    Set<String> getAllAuthorities();

}
