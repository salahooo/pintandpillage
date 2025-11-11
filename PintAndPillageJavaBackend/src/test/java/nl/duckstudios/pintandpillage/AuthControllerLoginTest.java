package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.config.JwtTokenUtil;
import nl.duckstudios.pintandpillage.controller.AuthController;
import nl.duckstudios.pintandpillage.dao.UserDAO;
import nl.duckstudios.pintandpillage.entity.User;
import nl.duckstudios.pintandpillage.model.JwtResult;
import nl.duckstudios.pintandpillage.model.LoginCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_withEmail_success_returnsToken() {
        // Arrange
        String email = "email@example.com";
        String plainPassword = "ValidPass123!";
        String hashedPassword = "hashed-secret";
        String expectedToken = "jwt-token-email";

        User user = new User();
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setFirstTimeLoggedIn(false);

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(expectedToken);

        LoginCredentials credentials = new LoginCredentials();
        credentials.username = email;
        credentials.password = plainPassword;

        // Act
        JwtResult result = authController.login(credentials);

        // Assert
        assertThat(result.token).isEqualTo(expectedToken);
        assertThat(result.isFirstTimeLoggedIn).isFalse();

        InOrder inOrder = inOrder(userDAO, passwordEncoder, jwtTokenUtil);
        inOrder.verify(userDAO).findByEmail(email);
        inOrder.verify(passwordEncoder).matches(plainPassword, hashedPassword);
        inOrder.verify(jwtTokenUtil).generateToken(email);

        verify(userDAO, never()).findByUsername(anyString());
        verify(userDAO, never()).save(any(User.class));
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }

    @Test
    void login_withUsername_success_returnsToken() {
        // Arrange
        String username = "thePlayer";
        String email = "player@example.com";
        String plainPassword = "SecretPass456!";
        String hashedPassword = "hashed-secret-player";
        String expectedToken = "jwt-token-username";

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setFirstTimeLoggedIn(false);

        when(userDAO.findByEmail(username)).thenReturn(Optional.empty());
        when(userDAO.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(expectedToken);

        LoginCredentials credentials = new LoginCredentials();
        credentials.username = username;
        credentials.password = plainPassword;

        // Act
        JwtResult result = authController.login(credentials);

        // Assert
        assertThat(result.token).isEqualTo(expectedToken);
        assertThat(result.isFirstTimeLoggedIn).isFalse();

        InOrder inOrder = inOrder(userDAO, passwordEncoder, jwtTokenUtil);
        inOrder.verify(userDAO).findByEmail(username);
        inOrder.verify(userDAO).findByUsername(username);
        inOrder.verify(passwordEncoder).matches(plainPassword, hashedPassword);
        inOrder.verify(jwtTokenUtil).generateToken(email);

        verify(userDAO, never()).save(any(User.class));
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }
}
