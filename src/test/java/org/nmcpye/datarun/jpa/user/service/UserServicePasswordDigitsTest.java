package org.nmcpye.datarun.jpa.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.jpa.userauthority.repository.AuthorityRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class UserServicePasswordDigitsTest {

    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(anyString())).thenReturn(mock(Cache.class));
        service = new UserService(
            repository,
            encoder,
            mock(AuthorityRepository.class),
            cacheManager,
            mock(UserAccessService.class));
    }

    @Test
    void storesAResetPasswordWithLatinDigits() {
        User user = new User();
        user.setLogin("worker");
        user.setResetDate(Instant.now());
        when(repository.findOneByResetKey("reset-key")).thenReturn(Optional.of(user));
        when(encoder.encode("pass456")).thenReturn("hash-456");

        service.completePasswordReset("pass۴۵۶", "reset-key");

        assertEquals("hash-456", user.getPassword());
    }

    @Test
    void checksAndStoresChangedPasswordsWithLatinDigits() {
        User user = new User();
        user.setLogin("worker");
        user.setPassword("hash-old");
        when(repository.findOneByLogin("worker")).thenReturn(Optional.of(user));
        when(encoder.matches("old123", "hash-old")).thenReturn(true);
        when(encoder.encode("new456")).thenReturn("hash-new");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("worker"));
            service.changePassword("old١٢٣", "new۴۵۶");
        }

        assertEquals("hash-new", user.getPassword());
    }

    @Test
    void storesLoginsWithLatinDigits() {
        User user = new User();
        user.setLogin("Field-١٢٣");

        assertEquals("field-123", user.getLogin());
    }
}
