package nl.duckstudios.pintandpillage;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.duckstudios.pintandpillage.controller.AuthController;
import nl.duckstudios.pintandpillage.dao.UserDAO;
import nl.duckstudios.pintandpillage.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerLoginWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserDAO userDAO;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockBean
    private nl.duckstudios.pintandpillage.config.JwtTokenUtil jwtTokenUtil;

    @MockBean
    private nl.duckstudios.pintandpillage.service.UserService userService;

    @Test
    void login_withEmail_returns200AndToken() throws Exception {
        String email = "user@example.com";
        String plainPassword = "Secret!23";
        String hashedPassword = "hashed-secret";
        String expectedToken = "jwt-abc";

        User storedUser = new User();
        storedUser.setEmail(email);
        storedUser.setPassword(hashedPassword);
        storedUser.setFirstTimeLoggedIn(false);

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(expectedToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", email,
                                "password", plainPassword
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andExpect(jsonPath("$.isFirstTimeLoggedIn").value(false));

        verify(userDAO).findByEmail(email);
        verify(userDAO, never()).findByUsername(anyString());
        verify(passwordEncoder).matches(plainPassword, hashedPassword);
        verify(jwtTokenUtil).generateToken(email);
        verify(userDAO, never()).save(any(User.class));
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }

    @Test
    void login_withUsername_returns200AndToken() throws Exception {
        String username = "player123";
        String email = "player@example.com";
        String plainPassword = "Secret!23";
        String hashedPassword = "hashed-player";
        String expectedToken = "jwt-xyz";

        User storedUser = new User();
        storedUser.setUsername(username);
        storedUser.setEmail(email);
        storedUser.setPassword(hashedPassword);
        storedUser.setFirstTimeLoggedIn(false);

        when(userDAO.findByEmail(username)).thenReturn(Optional.empty());
        when(userDAO.findByUsername(username)).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(expectedToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", username,
                                "password", plainPassword
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andExpect(jsonPath("$.isFirstTimeLoggedIn").value(false));

        var inOrder = inOrder(userDAO, passwordEncoder, jwtTokenUtil);
        inOrder.verify(userDAO).findByEmail(username);
        inOrder.verify(userDAO).findByUsername(username);
        inOrder.verify(passwordEncoder).matches(plainPassword, hashedPassword);
        inOrder.verify(jwtTokenUtil).generateToken(email);
        verify(userDAO, never()).save(any(User.class));
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }

    @Test
    void login_wrongPassword_returnsForbiddenWithoutToken() throws Exception {
        String email = "user@example.com";
        String plainPassword = "WrongPass!23";
        String hashedPassword = "hashed-secret";

        User storedUser = new User();
        storedUser.setEmail(email);
        storedUser.setPassword(hashedPassword);
        storedUser.setFirstTimeLoggedIn(false);

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches(plainPassword, hashedPassword)).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", email,
                                "password", plainPassword
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid Login Credentials"))
                .andExpect(jsonPath("$.token").doesNotExist());

        var inOrder = inOrder(userDAO, passwordEncoder);
        inOrder.verify(userDAO).findByEmail(email);
        inOrder.verify(passwordEncoder).matches(plainPassword, hashedPassword);

        verify(userDAO, never()).findByUsername(anyString());
        verify(userDAO, never()).save(any(User.class));
        verify(jwtTokenUtil, never()).generateToken(anyString());
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }

    @Test
    void login_unknownIdentifier_returnsNotFoundWithoutToken() throws Exception {
        String identifier = "unknown";

        when(userDAO.findByEmail(identifier)).thenReturn(Optional.empty());
        when(userDAO.findByUsername(identifier)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "identifier", identifier,
                                "password", "AnyPass!23"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("User not found for identifier " + identifier))
                .andExpect(jsonPath("$.token").doesNotExist());

        var inOrder = inOrder(userDAO);
        inOrder.verify(userDAO).findByEmail(identifier);
        inOrder.verify(userDAO).findByUsername(identifier);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userDAO, never()).save(any(User.class));
        verify(jwtTokenUtil, never()).generateToken(anyString());
        verifyNoMoreInteractions(userDAO, passwordEncoder, jwtTokenUtil);
    }
}
