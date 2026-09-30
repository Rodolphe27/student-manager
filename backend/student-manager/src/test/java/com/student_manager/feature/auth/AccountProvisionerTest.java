package com.student_manager.feature.auth;

import com.student_manager.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountProvisionerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AccountProvisioner provisioner;

    @BeforeEach
    void setUp() {
        provisioner = new AccountProvisioner(userRepository, passwordEncoder, "testuser12");
        lenient().when(passwordEncoder.encode(any())).thenAnswer(i -> "enc:" + i.getArgument(0));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void aBlankPasswordFallsBackToTheConfiguredDefault() {
        User user = provisioner.create("anna", "anna@example.com", " ", Role.STUDENT);

        assertThat(user.getPasswordHash()).isEqualTo("enc:testuser12");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void aSuppliedPasswordIsUsedInsteadOfTheDefault() {
        User user = provisioner.create("anna", "anna@example.com", "Secret123", Role.TEACHER);

        assertThat(user.getPasswordHash()).isEqualTo("enc:Secret123");
    }

    @Test
    void aBlankUsernameIsDerivedFromTheEmailLocalPart() {
        User user = provisioner.create(null, "anna.mueller@example.com", null, Role.STUDENT);

        assertThat(user.getUsername()).isEqualTo("anna.mueller");
    }

    @Test
    void unsupportedCharactersInTheDerivedUsernameAreReplaced() {
        User user = provisioner.create("", "anna+uni@example.com", null, Role.STUDENT);

        assertThat(user.getUsername()).isEqualTo("anna_uni");
    }

    @Test
    void aTooShortDerivedUsernameAsksForExplicitOne() {
        assertThatThrownBy(() -> provisioner.create(null, "ab@example.com", null, Role.STUDENT))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void aTakenUsernameIsRejected() {
        when(userRepository.existsByUsername("anna")).thenReturn(true);

        assertThatThrownBy(() -> provisioner.create("anna", "anna@example.com", null, Role.STUDENT))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void aTakenEmailIsRejected() {
        when(userRepository.existsByEmail("anna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> provisioner.create("anna", "anna@example.com", null, Role.STUDENT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void theSavedAccountIsTheOneReturned() {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);

        User returned = provisioner.create("anna", "anna@example.com", null, Role.STUDENT);

        verify(userRepository).save(saved.capture());
        assertThat(returned).isSameAs(saved.getValue());
    }
}
