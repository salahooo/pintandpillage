package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.config.JwtTokenUtil;
import nl.duckstudios.pintandpillage.controller.AuthController;
import nl.duckstudios.pintandpillage.dao.UserDAO;
import nl.duckstudios.pintandpillage.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class AuthControllerRegisterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_shouldHashPassword_andPersistUser_andReturnToken() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("new.user@example.com");
        newUser.setUsername("newUser");
        newUser.setPassword("PlainPassword123!");

        String hashedPassword = "hashed-password-value";
        String expectedToken = "jwt-token";

        when(userDAO.findByEmail("new.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("PlainPassword123!")).thenReturn(hashedPassword);
        when(userDAO.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenUtil.generateToken("new.user@example.com")).thenReturn(expectedToken);

        // Act
        Map<String, Object> result = authController.register(newUser);

        // Assert
        InOrder inOrder = inOrder(userDAO, passwordEncoder, jwtTokenUtil);
        inOrder.verify(userDAO).findByEmail("new.user@example.com");
        inOrder.verify(passwordEncoder).encode("PlainPassword123!");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        inOrder.verify(userDAO).save(savedUserCaptor.capture());
        inOrder.verify(jwtTokenUtil).generateToken("new.user@example.com");

        verify(passwordEncoder, times(1)).encode("PlainPassword123!");
        verify(userDAO, times(1)).save(any(User.class));

        User savedUser = savedUserCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo(hashedPassword);
        assertThat(savedUser.getPassword()).isNotEqualTo("PlainPassword123!");

        assertThat(result).containsEntry("token", expectedToken);
        verify(userDAO, times(1)).findByEmail("new.user@example.com");

        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }
}
