package com.sistematickets.Model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;


public class NombreLocalidadDeserializer extends JsonDeserializer<String> {

    private static final String[] CLAVES = {
        "nombre_localidad", "nombreLocalidad", "nombre", "localidad", "zona", "nombre_zona", "descripcion"
    };

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode n = p.getCodec().readTree(p);
        if (n == null || n.isNull()) return null;
        if (n.isValueNode()) return n.asText();
        if (n.isObject()) {
            for (String k : CLAVES) {
                JsonNode v = n.get(k);
                if (v != null && v.isTextual()) return v.asText();
            }

            for (var it = n.elements(); it.hasNext();) {
                JsonNode v = it.next();
                if (v.isTextual()) return v.asText();
            }
        }
        return null;
    }
}
