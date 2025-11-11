package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.ForbiddenException;
import nl.duckstudios.pintandpillage.config.JwtTokenUtil;
import nl.duckstudios.pintandpillage.controller.AuthController;
import nl.duckstudios.pintandpillage.dao.UserDAO;
import nl.duckstudios.pintandpillage.entity.User;
import nl.duckstudios.pintandpillage.Exceptions.UserNotFoundException;
import nl.duckstudios.pintandpillage.model.LoginCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
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
class AuthControllerLoginUnhappyTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_wrongPassword_isRejected_withoutToken() {
        // Arrange
        String email = "user@example.com";
        String plainPassword = "WrongPass123!";
        String storedPassword = "hashed-password";

        User storedUser = new User();
        storedUser.setEmail(email);
        storedUser.setPassword(storedPassword);

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(plainPassword, storedPassword)).thenReturn(false);

        LoginCredentials credentials = new LoginCredentials();
        credentials.username = email;
        credentials.password = plainPassword;

        // Act & Assert
        assertThatThrownBy(() -> authController.login(credentials))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Invalid Login Credentials");

        InOrder inOrder = inOrder(userDAO, passwordEncoder);
        inOrder.verify(userDAO).findByEmail(email);
        inOrder.verify(passwordEncoder).matches(plainPassword, storedPassword);

        verify(userDAO, never()).findByUsername(anyString());
        verify(userDAO, never()).save(any(User.class));
        verify(jwtTokenUtil, never()).generateToken(anyString());
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }

    @Test
    void login_unknownIdentifier_isRejected_withoutPasswordCheck_orToken() {
        // Arrange
        String identifier = "unknownUser";

        when(userDAO.findByEmail(identifier)).thenReturn(Optional.empty());
        when(userDAO.findByUsername(identifier)).thenReturn(Optional.empty());

        LoginCredentials credentials = new LoginCredentials();
        credentials.username = identifier;
        credentials.password = "AnyPassword!1";

        // Act & Assert
        assertThatThrownBy(() -> authController.login(credentials))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for identifier " + identifier);

        InOrder inOrder = inOrder(userDAO);
        inOrder.verify(userDAO).findByEmail(identifier);
        inOrder.verify(userDAO).findByUsername(identifier);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userDAO, never()).save(any(User.class));
        verify(jwtTokenUtil, never()).generateToken(anyString());
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }
}
