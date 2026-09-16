package io.github.apisecurity.tooling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("tooling")
class ScopedOpenApiGeneratorTest {

    private static final String SOURCE_PROPERTY = "openapi.spec";
    private static final String EXPECTED_SHA256 =
            "1CDC4B7D21F44E3EF62293EA8732210341A8AA5CA261F060DC38CB80540801C6";
    private static final Path OUTPUT_PATH = Path.of("target", "zap", "scoped-openapi.json");
    private static final Map<String, String> ALLOWED_OPERATIONS = allowedOperations();
    private static final Set<String> ALLOWED_REFERENCES = Set.of(
            "#/components/schemas/CRAPIResponse",
            "#/components/schemas/JwtResponse"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldGenerateScopedOpenApiFromOfficialSource() throws Exception {
        Path sourcePath = resolveSourcePath();
        assertEquals(EXPECTED_SHA256, sha256(sourcePath), "SHA-256 inesperado para a OpenAPI oficial");

        JsonNode source = objectMapper.readTree(sourcePath.toFile());
        ObjectNode scopedSpec = createScopedSpec(source);
        validateScope(scopedSpec);

        Files.createDirectories(OUTPUT_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT_PATH.toFile(), scopedSpec);

        JsonNode generatedSpec = objectMapper.readTree(OUTPUT_PATH.toFile());
        validateScope(generatedSpec);
        assertTrue(Files.size(OUTPUT_PATH) > 0, "O artefato temporário não pode estar vazio");

        System.out.printf(
                "OpenAPI temporária criada em %s com %d paths e %d operações.%n",
                OUTPUT_PATH.toAbsolutePath().normalize(),
                generatedSpec.path("paths").size(),
                countOperations(generatedSpec.path("paths"))
        );
    }

    private Path resolveSourcePath() {
        String configuredPath = System.getProperty(SOURCE_PROPERTY);
        assertNotNull(configuredPath, "Informe -Dopenapi.spec com o caminho da OpenAPI oficial");
        assertFalse(configuredPath.isBlank(), "O caminho da OpenAPI oficial não pode estar vazio");

        Path sourcePath = Path.of(configuredPath).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(sourcePath), "OpenAPI oficial não encontrada: " + sourcePath);
        return sourcePath;
    }

    private ObjectNode createScopedSpec(JsonNode source) {
        ObjectNode scopedSpec = objectMapper.createObjectNode();
        copyRequiredField(source, scopedSpec, "openapi");
        copyRequiredField(source, scopedSpec, "info");
        copyRequiredField(source, scopedSpec, "servers");
        scopedSpec.set("components", createScopedComponents(source));

        JsonNode sourcePaths = source.path("paths");
        ObjectNode scopedPaths = objectMapper.createObjectNode();

        ALLOWED_OPERATIONS.forEach((path, method) -> {
            JsonNode operation = sourcePaths.path(path).get(method);
            assertNotNull(operation, () -> "Contrato ausente na OpenAPI oficial: " + method.toUpperCase() + " " + path);

            ObjectNode pathNode = objectMapper.createObjectNode();
            pathNode.set(method, operation.deepCopy());
            scopedPaths.set(path, pathNode);
        });

        scopedSpec.set("paths", scopedPaths);
        return scopedSpec;
    }

    private ObjectNode createScopedComponents(JsonNode source) {
        JsonNode sourceComponents = source.path("components");
        JsonNode sourceSchemas = sourceComponents.path("schemas");
        JsonNode sourceSecuritySchemes = sourceComponents.path("securitySchemes");

        ObjectNode scopedSchemas = objectMapper.createObjectNode();
        copyRequiredField(sourceSchemas, scopedSchemas, "CRAPIResponse");
        copyRequiredField(sourceSchemas, scopedSchemas, "JwtResponse");

        ObjectNode scopedSecuritySchemes = objectMapper.createObjectNode();
        copyRequiredField(sourceSecuritySchemes, scopedSecuritySchemes, "bearerAuth");

        ObjectNode scopedComponents = objectMapper.createObjectNode();
        scopedComponents.set("schemas", scopedSchemas);
        scopedComponents.set("securitySchemes", scopedSecuritySchemes);
        return scopedComponents;
    }

    private void copyRequiredField(JsonNode source, ObjectNode target, String fieldName) {
        JsonNode value = source.get(fieldName);
        assertNotNull(value, () -> "Campo obrigatório ausente na OpenAPI oficial: " + fieldName);
        target.set(fieldName, value.deepCopy());
    }

    private void validateScope(JsonNode scopedSpec) {
        JsonNode paths = scopedSpec.path("paths");
        assertEquals(ALLOWED_OPERATIONS.size(), paths.size(), "A OpenAPI temporária deve conter três paths");
        assertEquals(ALLOWED_OPERATIONS.size(), countOperations(paths), "A OpenAPI temporária deve conter três operações");

        ALLOWED_OPERATIONS.forEach((path, method) -> {
            JsonNode pathNode = paths.get(path);
            assertNotNull(pathNode, () -> "Path não permitido ou ausente: " + path);
            assertEquals(1, pathNode.size(), () -> "O path deve conter somente uma operação: " + path);
            assertNotNull(pathNode.get(method), () -> "Operação ausente: " + method.toUpperCase() + " " + path);
        });

        assertEquals(2, scopedSpec.path("components").path("schemas").size());
        assertEquals(1, scopedSpec.path("components").path("securitySchemes").size());

        Set<String> references = new HashSet<>();
        collectReferences(scopedSpec, references);
        assertEquals(ALLOWED_REFERENCES, references, "A OpenAPI temporária contém referências fora do escopo");
    }

    private void collectReferences(JsonNode node, Set<String> references) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                if ("$ref".equals(entry.getKey())) {
                    references.add(entry.getValue().asText());
                } else {
                    collectReferences(entry.getValue(), references);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectReferences(child, references));
        }
    }

    private int countOperations(JsonNode paths) {
        int count = 0;
        for (JsonNode path : paths) {
            count += path.size();
        }
        return count;
    }

    private String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().withUpperCase().formatHex(digest.digest(Files.readAllBytes(path)));
    }

    private static Map<String, String> allowedOperations() {
        Map<String, String> operations = new LinkedHashMap<>();
        operations.put("/identity/api/auth/signup", "post");
        operations.put("/identity/api/auth/login", "post");
        operations.put("/identity/api/v2/user/dashboard", "get");
        return Collections.unmodifiableMap(operations);
    }
}
