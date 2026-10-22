package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Usuario(
        @JsonProperty("id_usuario") @JsonAlias({"id", "idUsuario"}) Integer idUsuario,
        String nombre,
        @JsonAlias({"email"}) String correo,
        @JsonProperty("id_rol") @JsonAlias({"idRol"}) Integer idRol,
        @JsonAlias({"rol_nombre", "nombre_rol", "role"}) String rol,
        String status) {
}
