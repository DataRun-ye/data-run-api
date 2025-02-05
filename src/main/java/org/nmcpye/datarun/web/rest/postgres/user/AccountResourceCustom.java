package org.nmcpye.datarun.web.rest.postgres.user;

import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.service.MailService;
import org.nmcpye.datarun.service.UserService;
import org.nmcpye.datarun.service.dto.AdminUserDTO;
import org.nmcpye.datarun.service.dto.PasswordChangeDTO;
import org.nmcpye.datarun.web.rest.errors.EmailAlreadyUsedException;
import org.nmcpye.datarun.web.rest.errors.InvalidPasswordException;
import org.nmcpye.datarun.web.rest.errors.LoginAlreadyUsedException;
import org.nmcpye.datarun.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api/custom")
public class AccountResourceCustom /*extends AbstractRelationalResource<User>*/ {

//    @Override
//    protected String getName() {
//        return "me";
//    }

    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private final Logger log = LoggerFactory.getLogger(AccountResourceCustom.class);

    private final UserRepository userRepository;

    private final UserService userService;

    public AccountResourceCustom(UserRepository userRepository, UserService userService, MailService mailService) {
//        super(userService, userRepository);
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * {@code GET  /me} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/me")
    public AdminUserDTO getAccount() {
        final var user = userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new);
        log.debug("Created Information for User: {}", user);
        return userService
            .getUserWithAuthorities()
            .map(AdminUserDTO::new)
            .orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code POST  /me} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/me")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.orElseThrow().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (!user.isPresent()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(
            userDTO.getFirstName(),
            userDTO.getLastName(),
            userDTO.getEmail(),
            userDTO.getLangKey(),
            userDTO.getImageUrl()
        );
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/me/changePassword")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
                password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
                password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }


    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());
        return ResponseEntity.ok(user);
    }
}
