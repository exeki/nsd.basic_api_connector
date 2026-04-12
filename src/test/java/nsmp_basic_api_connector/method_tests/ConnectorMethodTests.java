package nsmp_basic_api_connector.method_tests;

import nsmp_basic_api_connector.ApiTestUtils;
import org.junit.jupiter.api.Test;
import ru.kazantsev.nsmp.basic_api_connector.Connector;
import ru.kazantsev.nsmp.basic_api_connector.dto.nsmp.FileDto;
import ru.kazantsev.nsmp.basic_api_connector.dto.nsmp.ScriptChecksums;
import ru.kazantsev.nsmp.basic_api_connector.dto.nsmp.ServiceTimeExclusionDto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorMethodTests {

    private static final String METACLASS = "serviceCall$serviceCall";
    private static final String OBJECT_UUID = "serviceCall$15803";
    private static final String SERVICE_TIME_UUID = "servicetime$8901";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String FILE_UUID = "";

    private static Connector api() {
        return ApiTestUtils.getApi();
    }

    private static File resourceFile(String name) {
        var url = ConnectorMethodTests.class.getClassLoader().getResource(name);
        assertNotNull(url, "Missing test resource: " + name);
        try {
            return new File(url.toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String resourceText(String name) {
        try {
            return Files.readString(resourceFile(name).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> createServiceCallPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("metaClass", METACLASS);
        payload.put("description", "java-test");
        payload.put("clientEmployee", "employee$10501");
        payload.put("clientOU", "ou$10001");
        payload.put("agreement", "agreement$9101");
        payload.put("service", "slmService$9301");
        return payload;
    }

    @Test
    void create() {
        assertDoesNotThrow(() -> api().create(METACLASS, createServiceCallPayload()));
    }

    @Test
    void addFile() {
        assertDoesNotThrow(() -> api().addFile(OBJECT_UUID, resourceFile("test/testFile.txt")));
    }

    @Test
    void addFileFromBytes() {
        byte[] bytes = resourceText("test/testFile.txt").getBytes(StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> api().addFile(OBJECT_UUID, bytes, "get.txt"));
    }

    @Test
    void addFileFromFileList() {
        List<File> files = List.of(resourceFile("test/testFile.txt"));
        assertDoesNotThrow(() -> api().addFile(OBJECT_UUID, files));
    }

    @Test
    void createExcl() throws Exception {
        ServiceTimeExclusionDto excl = api().createExcl(
                SERVICE_TIME_UUID,
                new SimpleDateFormat(DATE_PATTERN).parse("2022-01-15")
        );
        assertNotNull(excl);
        assertNotNull(excl.uuid);
    }

    @Test
    void createM2M() {
        Connector api = api();
        Map<String, Object> payload = createServiceCallPayload();
        HashMap result = api.createM2M(METACLASS, payload);
        assertNotNull(result);
        assertNotNull(result.get("UUID"));
        api.delete(String.valueOf(result.get("UUID")));
    }

    @Test
    void createM2MMultiple() {
        Connector api = api();
        List<Map<String, Object>> objects = new ArrayList<>();
        objects.add(createServiceCallPayload());
        objects.add(createServiceCallPayload());
        objects.add(createServiceCallPayload());

        List<HashMap> result = api.createM2MMultiple(objects);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (HashMap item : result) {
            Object uuid = item.get("UUID");
            if (uuid != null) {
                api.delete(String.valueOf(uuid));
            }
        }
    }

    @Test
    void delete() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        assertDoesNotThrow(() -> api.delete(uuid));
    }

    @Test
    void edit() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        assertDoesNotThrow(() -> api.edit(uuid, Map.of("title", "TESETESTESTES")));
        api.delete(uuid);
    }

    @Test
    void editExcl() throws Exception {
        Connector api = api();
        ServiceTimeExclusionDto excl = api.createExcl(
                SERVICE_TIME_UUID,
                new SimpleDateFormat(DATE_PATTERN).parse("2023-01-17")
        );
        ServiceTimeExclusionDto edited = api.editExcl(excl.uuid, 28_800_000L, 53_000_000L);
        assertNotNull(edited);
        assertNotNull(edited.uuid);
    }

    @Test
    void editM2M() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        HashMap edited = api.editM2M(uuid, Map.of("title", "drthwrthwrh"), List.of("title", "UUID"));
        assertNotNull(edited);
        api.delete(uuid);
    }

    @Test
    void execFileFromFile() {
        assertDoesNotThrow(() -> api().execFile(resourceFile("test/testScript.groovy")));
    }

    @Test
    void execFileFromBytes() {
        byte[] bytes = resourceText("test/testScript.groovy").getBytes(StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> api().execFile(bytes));
    }

    @Test
    void execFileFromString() {
        assertDoesNotThrow(() -> api().execFile(resourceText("test/testScript.groovy")));
    }

    @Test
    void find() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        List<HashMap> result = api.find(METACLASS, Map.of("UUID", uuid), null, 0L, 1L);
        assertNotNull(result);
    }

    @Test
    void findWithoutPaging() {
        List<HashMap> result = api().find(METACLASS, Map.of());
        assertNotNull(result);
    }

    @Test
    void findWithReturnAttrs() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        List<HashMap> result = api.find(METACLASS, Map.of("UUID", uuid), List.of("title", "UUID"));
        assertNotNull(result);
    }

    @Test
    void get() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        HashMap result = api.get(uuid, List.of("title", "UUID"));
        assertNotNull(result);
    }

    @Test
    void getWithoutReturnAttrs() {
        Connector api = api();
        HashMap created = api.createM2M(METACLASS, createServiceCallPayload());
        String uuid = String.valueOf(created.get("UUID"));
        HashMap result = api.get(uuid);
        assertNotNull(result);
    }

    @Test
    void getAccessKey() {
        String key = api().getAccessKey("system", "manager", 1);
        assertNotNull(key);
        assertFalse(key.isBlank());
    }

    @Test
    void getFile() {
        FileDto file = api().getFile(FILE_UUID);
        assertNotNull(file);
        assertNotNull(file.bytes);
        assertTrue(file.bytes.length > 0);
        assertNotNull(file.title);
    }

    @Test
    void getScripts() {
        String archive = api().getScripts();
        assertNotNull(archive);
        assertFalse(archive.isBlank());
    }

    @Test
    void getScriptsStatus() {
        ScriptChecksums checksums = api().getScriptsStatus();
        assertNotNull(checksums);
    }

    @Test
    void groovyVersion() {
        String value = api().groovyVersion();
        assertNotNull(value);
        assertFalse(value.isBlank());
    }

    @Test
    void jpdaInfo() {
        String value = api().jpdaInfo();
        assertNotNull(value);
    }

    @Test
    void metainfoWithTimeout() {
        String value = api().metainfo(15_000);
        assertNotNull(value);
        assertFalse(value.isBlank());
    }

    @Test
    void metainfoDefaultTimeout() {
        String value = api().metainfo();
        assertNotNull(value);
    }

    @Test
    void pushScripts() {
        Connector api = api();
        ScriptChecksums checksums = api.pushScripts(api.getScripts().getBytes(StandardCharsets.UTF_8));
        assertNotNull(checksums);
    }

    /*
    @Test
    void uploadMetainfoWithTimeout() {
        Connector api = api();
        String content = api.metainfo(15_000);
        assertDoesNotThrow(() -> api.uploadMetainfo(content, 15_000));
    }

    @Test
    void uploadMetainfoDefaultTimeout() {
        Connector api = api();
        String content = api.metainfo(15_000);
        assertDoesNotThrow(() -> api.uploadMetainfo(content));
    }
     */

    @Test
    void version() {
        String value = api().version();
        assertNotNull(value);
        assertFalse(value.isBlank());
    }
}
