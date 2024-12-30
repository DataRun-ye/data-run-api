package org.nmcpye.datarun.web.rest.postgres;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.nmcpye.datarun.config.Constants;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.service.MailService;
import org.nmcpye.datarun.service.UserService;
import org.nmcpye.datarun.service.dto.AdminUserDTO;
import org.nmcpye.datarun.web.rest.errors.EmailAlreadyUsedException;
import org.nmcpye.datarun.web.rest.errors.InvalidPasswordException;
import org.nmcpye.datarun.web.rest.errors.LoginAlreadyUsedException;
import org.nmcpye.datarun.web.rest.vm.KeyAndPasswordVM;
import org.nmcpye.datarun.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api/custom")
public class AccountResourceCustom extends AbstractRelationalResource<User> {

    @Override
    protected String getName() {
        return "me";
    }

    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private final Logger log = LoggerFactory.getLogger(AccountResourceCustom.class);

    private final UserRepository userRepository;

    private final UserService userService;

    public AccountResourceCustom(UserRepository userRepository, UserService userService, MailService mailService) {
        super(userService, userRepository);
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
     * {@code POST   /me/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/me/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
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

    /**
     * {@code POST  /register-list} : register a list of users.
     *
     * @param managedUserVMList the list of managed user View Models.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if any password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if any email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if any login is already used.
     */
    @PostMapping("/registerList")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccountList(@Valid @RequestBody List<ManagedUserVM> managedUserVMList) {
        userService.registerUserList(managedUserVMList);
        managedUserVMList.forEach(managedUserVM -> {
            User user = userService.findUserByLogin(managedUserVM.getLogin()).orElseThrow();
//            mailService.sendActivationEmail(user);
        });
    }

    /**
     * {@code PUT /admin/users} : Updates an existing User.
     *
     * @param userDTO the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated user.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping({"/users", "/users/{login}"})
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AdminUserDTO> updateUser(
        @PathVariable(name = "login", required = false) @Pattern(regexp = Constants.LOGIN_REGEX) String login,
        @Valid @RequestBody ManagedUserVM userDTO
    ) {
        log.debug("REST request to update User : {}", userDTO);
        Optional<User> existingUser = userRepository.findOneByLogin(userDTO.getLogin());
        if (existingUser.isEmpty()) {
            throw new UsernameNotFoundException(userDTO.getLogin());
        }
//        existingUser = userRepository.findOneByLogin(userDTO.getLogin().toLowerCase());
//        if (existingUser.isPresent() && (!existingUser.orElseThrow().getId().equals(userDTO.getId()))) {
//            throw new LoginAlreadyUsedException();
//        }

        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO, userDTO.getPassword());

        return ResponseUtil.wrapOrNotFound(
            updatedUser,
            HeaderUtil.createAlert(applicationName, "userManagement.updated", userDTO.getLogin())
        );
    }
}
