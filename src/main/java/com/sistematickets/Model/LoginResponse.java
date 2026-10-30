package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResponse(
        @JsonAlias({"accessToken", "jwt"}) String token,
        @JsonAlias({"user", "data"}) Usuario usuario) {
}
