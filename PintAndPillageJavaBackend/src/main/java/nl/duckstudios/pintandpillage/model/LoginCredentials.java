package nl.duckstudios.pintandpillage.model;

import com.fasterxml.jackson.annotation.JsonSetter;

public class LoginCredentials {

    public String username;
    public String password;

    public LoginCredentials() {
    }

    public LoginCredentials(String email, String password) {
        this.username = email;
        this.password = password;
    }

    @JsonSetter("identifier")
    public void setIdentifier(String identifier) {
        this.username = identifier; // REFACTOR (ITSTEN H2): Map unified identifier input onto existing username field.
    }
}
