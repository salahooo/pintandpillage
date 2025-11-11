package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.UserAlreadyExistsException;
import nl.duckstudios.pintandpillage.config.JwtTokenUtil;
import nl.duckstudios.pintandpillage.controller.AuthController;
import nl.duckstudios.pintandpillage.dao.UserDAO;
import nl.duckstudios.pintandpillage.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerRegisterDuplicateEmailTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_shouldRejectDuplicateEmail_withoutHashingSavingOrToken() {
        // Arrange
        User incomingUser = new User();
        incomingUser.setEmail("duplicate@example.com");
        incomingUser.setUsername("duplicateUser");
        incomingUser.setPassword("ValidPass123!");

        User existingUser = new User();
        existingUser.setEmail("duplicate@example.com");

        when(userDAO.findByEmail("duplicate@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> authController.register(incomingUser))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email adres is taken duplicate@example.com");

        InOrder inOrder = inOrder(userDAO, passwordEncoder, jwtTokenUtil);
        inOrder.verify(userDAO).findByEmail("duplicate@example.com");
        inOrder.verifyNoMoreInteractions();

        verify(passwordEncoder, never()).encode(anyString());
        verify(userDAO, never()).save(any(User.class));
        verify(jwtTokenUtil, never()).generateToken(anyString());
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }
}
