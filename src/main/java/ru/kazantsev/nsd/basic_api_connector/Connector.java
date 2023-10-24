package ru.kazantsev.nsd.basic_api_connector;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpEntity;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Коннектор, имплементирующий методы базового API NSD4.15
 */
@SuppressWarnings("rawtypes,unused")
public class Connector {

    protected static final String ACCESS_KEY_PARAM_NAME = "accessKey";
    protected static final String BASE_PATH = "/sd/services/rest";
    protected static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    protected static Logger logger = LoggerFactory.getLogger(Connector.class);

    protected String scheme;
    protected String host;
    protected String accessKey;
    protected Boolean infoLoggingIsEnabled = true;

    /**
     * Клиент, с заранее вложенными парсером, авторизацией, обработчиком ошибок
     */
    protected CloseableHttpClient client;
    /**
     * используемый при общении маппер
     */
    protected ObjectMapper objectMapper;

    public Connector(ConnectorParams params) {
        this.setParams(params);
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if(params.isIgnoringSSL()) clientBuilder.setSSLSocketFactory(getNoSslSocketFactory());
        this.client = clientBuilder.build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(new SimpleDateFormat(DATE_PATTERN));
    }

    /**
     * Возвращает SSLConnectionSocketFactory с выключенным ssl
     * @return SSLConnectionSocketFactory с выключенным ssl
     */
    protected static SSLConnectionSocketFactory getNoSslSocketFactory() {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    public void logInfo(Object... objects) {
        if (infoLoggingIsEnabled) {
            String message = Arrays.stream(objects).map(object -> Optional.ofNullable(object).orElse("null")).map(Object::toString).collect(Collectors.joining());
            logger.info(message);
        }
    }

    public void setInfoLogging(Boolean boo) {
        this.infoLoggingIsEnabled = boo;
    }

    public void setObjectMapper(ObjectMapper mapper) {
        this.objectMapper = mapper;
    }

    public void setParams(ConnectorParams params) {
        this.host = params.getHost();
        this.accessKey = params.getAccessKey();
        this.scheme = params.getScheme();
    }

    /**
     * Возвращает базовый конструктор URI
     * @return  базовый конструктор URI
     */
    protected URIBuilder getBasicUriBuilder() {
        return new URIBuilder().setScheme(scheme).setHost(host).addParameter(ACCESS_KEY_PARAM_NAME, accessKey);
    }

    /**
     * Создание объекта (метод rest api 'create')
     *
     * @param metaClassCode fqn создаваемого объекта, например, serviceCall.
     * @param attributes    атрибуты создаваемого объекта.
     */
    public void create(String metaClassCode, Map<String, Object> attributes) {
        String PATH_SEGMENT = "create";
        logInfo("create (" + metaClassCode + ", " + attributes + ")");
        String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(path));
        logInfo("create uri: " + uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, attributes));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        logInfo("create response status: " + response.getStatusLine().getStatusCode() + " , body: " + ConnectorUtilities.entityToString(response.getEntity()));
    }

    /**
     * Добавление файла к объекту (метод rest api 'add-file')
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param file             отправляемый файл
     */
    public void addFile(String targetObjectUuid, File file) {
        addFile(targetObjectUuid, Collections.singletonList(file), null);
    }

    /**
     * Добавление файла к объекту (метод rest api 'add-file')
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param file             отправляемый файл
     * @param attrCode         код атрибута типа "Файл". Если параметр указан, то файл добавляется в указанный атрибут, иначе файл добавляется к объекту.
     */
    public void addFile(String targetObjectUuid, File file, String attrCode) {
        addFile(targetObjectUuid, Collections.singletonList(file), attrCode);
    }

    /**
     * Добавление файлов к объекту (метод rest api 'add-file')
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param files            отправляемые файлы
     */
    public void addFile(String targetObjectUuid, List<File> files) {
        addFile(targetObjectUuid, files, null);
    }

    /**
     * Добавление файлов к объекту (метод rest api 'add-file')
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param files            отправляемые файлы
     * @param attrCode         код атрибута типа "Файл". Если параметр указан, то файл добавляется в указанный атрибут, иначе файл добавляется к объекту.
     */
    public void addFile(String targetObjectUuid, List<File> files, String attrCode) {
        String PATH_SEGMENT = "add-file";
        logInfo("addFile (" + targetObjectUuid + ", ", files);
        String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
        URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
        if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
        URI uri = ConnectorUtilities.buildUriBuilder(uriBuilder);
        logInfo("addFile uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        for (int i = 0; i < files.size(); i++) {
            entityBuilder.addBinaryBody(String.valueOf(i), files.get(i));
        }
        httpPost.setEntity(entityBuilder.build());
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        logInfo("addFile response status: " + response.getStatusLine().getStatusCode() + ". body: "
                + ConnectorUtilities.entityToString(response.getEntity()));
    }

    /**
     * Добавление файлов к объекту (метод rest api 'add-file')
     * contentType будет указан как plain text
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param fileBytes        байты добавляемого файла
     * @param fileName         имя добавляемого файла
     */
    public void addFile(String targetObjectUuid, byte[] fileBytes, String fileName) {
        addFile(targetObjectUuid, fileBytes, fileName, null);
    }

    /**
     * Добавление файлов к объекту (метод rest api 'add-file')
     *
     * @param targetObjectUuid идентификатор объекта, к которому будет приложен файл, например, serviceCall$1992;
     * @param fileBytes        байты добавляемого файла
     * @param fileName         имя добавляемого файла
     * @param attrCode         код атрибута типа "Файл". Если параметр указан, то файл добавляется в указанный атрибут, иначе файл добавляется к объекту.
     */
    public void addFile(String targetObjectUuid, byte[] fileBytes, String fileName, String attrCode) {
        String PATH_SEGMENT = "add-file";
        logInfo("addFile (" + targetObjectUuid + "/, byte[], ", fileName, ")");
        String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
        URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
        if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
        URI uri = ConnectorUtilities.buildUriBuilder(uriBuilder);
        logInfo("addFile uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", fileBytes, ContentType.TEXT_PLAIN, fileName)
                .build();
        httpPost.setEntity(entity);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        logInfo("addFile response status: " + response.getStatusLine().getStatusCode()
                + ". body: " + ConnectorUtilities.entityToString(response.getEntity()));
    }

    /**
     * Создание исключения в указанном классе обслуживания
     *
     * @param serviceTimeUuid uuid класса обслуживания, в который нужно добавить исключение, например, servicetime$2204
     * @param exclusionDate   дата исключения
     * @return Созданный объект (исключение). Черновик редактируемого класса обслуживания, в котором создается исключение, будет автоматически подтвержден.
     */
    public NsdDto.ServiceTimeExclusionDto createExcl(String serviceTimeUuid, Date exclusionDate) {
        return createExcl(serviceTimeUuid, exclusionDate, null, null);
    }

    /**
     * Создание исключения в указанном классе обслуживания
     *
     * @param serviceTimeUuid uuid класса обслуживания, в который нужно добавить исключение, например, servicetime$2204
     * @param exclusionDate   дата исключения
     * @param startTime       время начала исключения (необязательно)
     * @param endTime         время окончания исключения (необязательно)
     * @return Созданный объект (исключение). Черновик редактируемого класса обслуживания, в котором создается исключение, будет автоматически подтвержден.
     */
    public NsdDto.ServiceTimeExclusionDto createExcl(String serviceTimeUuid, Date exclusionDate, Long startTime, Long endTime) {
        String PATH_SEGMENT = "create-excl";
        logInfo("createExcl (" + serviceTimeUuid + ", ", exclusionDate, ")");
        HashMap<String, String> lastSegmentMap = new HashMap<>();
        lastSegmentMap.put("exclusionDate", new SimpleDateFormat(DATE_PATTERN).format(exclusionDate));
        if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
        if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
        String lastSegmentString = ConnectorUtilities.writeValueAsString(this.objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT), lastSegmentMap);
        String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeUuid + "/" + lastSegmentString;
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(path));
        logInfo("createExcl uri: " + uri);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("createExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return ConnectorUtilities.readValue(objectMapper, responseBody, NsdDto.ServiceTimeExclusionDto.class);
    }

    /**
     * Делает из Map JSON строку, которую потом можно затолкать в url
     *
     * @param map из этого будет создан JSON
     * @return JSON
     */
    protected static String createJsonForUrl(HashMap<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        int size = map.size();
        int index = 1;
        stringBuilder.append("{");
        map.forEach(
                (key, value) ->
                {
                    stringBuilder.append("\"").append(key).append("\":\"").append(value).append("\"");
                    if (index != size) stringBuilder.append(",");
                }
        );
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * Создание объекта для машинного взаимодействия
     *
     * @param metaClassCode fqn создаваемого объекта, например, serviceCall
     * @param attributes    атрибуты создаваемого объекта
     * @return Созданный объект или только указанные атрибуты созданного объекта, если установлен returnAttrs;
     */
    public HashMap createM2M(String metaClassCode, Map<String, Object> attributes) {
        return createM2M(metaClassCode, attributes, null);
    }

    /**
     * Создание объекта для машинного взаимодействия
     *
     * @param metaClassCode fqn создаваемого объекта, например, serviceCall
     * @param attributes    атрибуты создаваемого объекта
     * @param returnAttrs   коды атрибутов (через запятую, без пробелов), которые необходимо вернуть в ответе. Если параметр будет пустой, то вернется весь объект.
     * @return Созданный объект или только указанные атрибуты созданного объекта, если установлен returnAttrs;
     */
    public HashMap createM2M(String metaClassCode, Map<String, Object> attributes, List<String> returnAttrs) {
        String PATH_SEGMENT = "create-m2m";
        logInfo("createM2M (", metaClassCode, ", ", attributes, ", ", returnAttrs, ")");
        String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
        URIBuilder builder = getBasicUriBuilder().setPath(path);
        if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
        URI uri = ConnectorUtilities.buildUriBuilder(builder);
        logInfo("createM2M uri: " + uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, attributes));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("createM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return ConnectorUtilities.readValue(objectMapper, responseBody, HashMap.class);
    }

    /**
     * Создание множества объектов для машинного взаимодействия
     *
     * @param objects лист с атрибутами создаваемых объектов
     * @return Массив объектов с UUID для созданных и переданную информацию для создания объекта с сообщением об ошибке в поле error для не созданных.
     */
    public List<HashMap> createM2MMultiple(List<Map<String, Object>> objects) {
        String PATH_SEGMENT = "create-m2m-multiple";
        logInfo("createM2MMultiple (", objects, ")");
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT));
        logInfo("createM2MMultiple uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, objects));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("createM2MMultiple response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return Arrays.stream(ConnectorUtilities.readValue(objectMapper, responseBody, HashMap[].class)).collect(Collectors.toList());
    }

    /**
     * Удаление объекта
     *
     * @param objectUuid uuid удаляемого объекта, например, serviceCall$501.
     */
    public void delete(String objectUuid) {
        String PATH_SEGMENT = "delete";
        logInfo("delete (" + objectUuid + ")");
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid));
        logInfo("delete uri: ", uri);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        logInfo("delete response status: "
                + response.getStatusLine().getStatusCode() + ". body: "
                + ConnectorUtilities.entityToString(response.getEntity()));
    }

    /**
     * Редактирование объекта
     *
     * @param objectUuid uuid изменяемого объекта, например, serviceCall$501.
     * @param attributes изменяемые атрибуты.
     */
    public void edit(String objectUuid, Map<String, Object> attributes) {
        String PATH_SEGMENT = "edit";
        logInfo("edit (" + objectUuid + ", ", attributes, ")");
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid));
        logInfo("edit uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, attributes));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        logInfo("edit response status: " + response.getStatusLine().getStatusCode()
                + ". body: " + ConnectorUtilities.entityToString(response.getEntity()));
    }

    /**
     * Редактирование периода исключения для заданного исключения класса обслуживания
     *
     * @param serviceTimeExclusion uuid изменяемого объекта, например, srvTimeExcl$10502;
     * @param startTime            время начала исключения
     * @param endTime              время окончания исключения
     * @return измененный объект. Черновик редактируемого класса обслуживания, в котором создается исключение, будет автоматически подтвержден.
     */
    public NsdDto.ServiceTimeExclusionDto editExcl(String serviceTimeExclusion, Long startTime, Long endTime) {
        String PATH_SEGMENT = "edit-excl";
        logInfo("editExcl ( " + serviceTimeExclusion + ", ", startTime, ", ", endTime, ")");
        HashMap<String, String> lastSegmentMap = new HashMap<>();
        lastSegmentMap.put("exclusionDate", serviceTimeExclusion);
        if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
        if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
        String lastSegmentString = ConnectorUtilities.writeValueAsString(this.objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT), lastSegmentMap);
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeExclusion + "/" + lastSegmentString));
        logInfo("editExcl uri: ${uri}");
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("editExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return ConnectorUtilities.readValue(objectMapper, responseBody, NsdDto.ServiceTimeExclusionDto.class);
    }

    /**
     * Редактирование (для машинного взаимодействия)
     *
     * @param objectUuid uuid изменяемого объекта, например, srvTimeExcl$10502;
     * @param attributes изменяемые атрибуты
     * @return измененный объект или только указанные атрибуты, если установлен returnAttrs.
     */
    public HashMap editM2M(String objectUuid, Map<String, Object> attributes) {
        return editM2M(objectUuid, attributes, null);
    }

    /**
     * Редактирование (для машинного взаимодействия)
     *
     * @param objectUuid  uuid изменяемого объекта, например, srvTimeExcl$10502;
     * @param attributes  изменяемые атрибуты
     * @param returnAttrs коды атрибутов (через запятую, без пробелов), которые необходимо вернуть в ответе. Если параметр будет пустой, то вернется весь объект
     * @return измененный объект или только указанные атрибуты, если установлен returnAttrs.
     */
    public HashMap editM2M(String objectUuid, Map<String, Object> attributes, List<String> returnAttrs) {
        String PATH_SEGMENT = "edit-m2m";
        logInfo("editM2M (", objectUuid, ", ", attributes, ", ", returnAttrs, ")");
        URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
        if (returnAttrs != null) builder.addParameter("attrs", String.join(",", returnAttrs));
        URI uri = ConnectorUtilities.buildUriBuilder(builder);
        logInfo("editM2M uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, attributes));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("editM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return ConnectorUtilities.readValue(objectMapper, responseBody, HashMap.class);
    }

    /**
     * Выполнение скрипта из файла
     *
     * @param file файл скрипта для выполнения
     * @return Результат выполнения скрипта в виде строки (без какого либо формата)
     */
    public String execFile(File file) {
        try {
            return execFile(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполнение скрипта из файла
     *
     * @param byteArray байты скрипта
     * @return Результат выполнения скрипта в виде строки (без какого либо формата)
     */
    public String execFile(byte[] byteArray) {
        String PATH_SEGMENT = "exec";
        logInfo("execFile (byte[])");
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT));
        logInfo("execFile uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("script", byteArray, ContentType.TEXT_PLAIN, "script.groovy")
                .build();
        httpPost.setEntity(entity);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("execFile response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return responseBody;
    }

    /**
     * Выполнение скрипта из файла
     *
     * @param scriptText текст скрипта в UTF_8 кодировке
     * @return Результат выполнения скрипта в виде строки (без какого либо формата)
     */
    public String execFile(String scriptText) {
        return execFile(scriptText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Получение информации об объекте
     *
     * @param objectUuid uuid интересующего объекта
     * @return объект или только указанные атрибуты, если установлен returnAttrs.
     */
    public HashMap get(String objectUuid) {
        return get(objectUuid, null);
    }

    /**
     * Получение информации об объекте
     *
     * @param objectUuid  uuid интересующего объекта
     * @param returnAttrs коды атрибутов (через запятую, без пробелов), которые необходимо вернуть в ответе. Если параметр будет пустой, то вернется весь объект.
     * @return объект или только указанные атрибуты, если установлен returnAttrs.
     */
    public HashMap get(String objectUuid, List<String> returnAttrs) {
        String PATH_SEGMENT = "get";
        logInfo("get (" + objectUuid + ", ", returnAttrs, ")");
        URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
        if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
        URI uri = ConnectorUtilities.buildUriBuilder(builder);
        logInfo("get uri: ", uri);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("get response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return ConnectorUtilities.readValue(objectMapper, responseBody, HashMap.class);
    }

    /**
     * Получение контента файла по его UUID
     *
     * @param fileUuid uuid файла
     * @return DTO содержащий информацию о файле
     */
    public NsdDto.FileDto getFile(String fileUuid) {
        String PATH_SEGMENT = "get-file";
        logInfo("getFile (" + fileUuid + ")");
        URI uri = ConnectorUtilities.buildUriBuilder(getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + fileUuid));
        logInfo("getFile uri: ", uri);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        String contentDisposition = response.getFirstHeader("Content-Disposition").getValue();
        int index = contentDisposition.indexOf('=');
        NsdDto.FileDto file = new NsdDto.FileDto(
                ConnectorUtilities.entityToByre(response.getEntity()),
                contentDisposition.substring(index + 2, contentDisposition.length() - 1),
                response.getFirstHeader("Content-Type").getValue()
        );
        logInfo("getFile response status: " + response.getStatusLine().getStatusCode() + ". body: byte[]");
        return file;
    }

    /**
     * Поиск бизнес объектов в системе
     *
     * @param metaClassCode fqn типа (класса) объекта
     * @param searchAttrs   атрибуты и их значения, по которым осуществляется поиск
     * @return список найденных объектов
     */
    public List<HashMap> find(
            String metaClassCode,
            Map<String, Object> searchAttrs
    ) {
        return find(metaClassCode, searchAttrs, null, null, null);
    }

    /**
     * Поиск бизнес объектов в системе
     *
     * @param metaClassCode fqn типа (класса) объекта
     * @param searchAttrs   атрибуты и их значения, по которым осуществляется поиск
     * @param returnAttrs   коды атрибутов, которые необходимо вернуть в ответе (через запятую, без пробелов). Если параметр будет пустой, то вернется весь объект
     * @return список найденных объектов
     */
    public List<HashMap> find(
            String metaClassCode,
            Map<String, Object> searchAttrs,
            List<String> returnAttrs
    ) {
        return find(metaClassCode, searchAttrs, returnAttrs, null, null);
    }

    /**
     * Поиск бизнес объектов в системе
     *
     * @param metaClassCode fqn типа (класса) объекта
     * @param searchAttrs   атрибуты и их значения, по которым осуществляется поиск
     * @param returnAttrs   коды атрибутов, которые необходимо вернуть в ответе (через запятую, без пробелов). Если параметр будет пустой, то вернется весь объект
     * @param offset        количество строк (число), которые будут пропускаться перед выводом результатов запроса
     * @param limit         максимальное количество элементов для поиска (число)
     * @return список найденных объектов
     */
    public List<HashMap> find(
            String metaClassCode,
            Map<String, Object> searchAttrs,
            List<String> returnAttrs,
            Long offset,
            Long limit
    ) {
        String PATH_SEGMENT = "find";
        logInfo("find (", metaClassCode, ", ", searchAttrs, ", ", returnAttrs, ", ", offset, ", ", limit, ")");
        URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode);
        if (returnAttrs != null) uriBuilder.setParameter("attrs", String.join(",", returnAttrs));
        if (offset != null) uriBuilder.setParameter("offset", offset.toString());
        if (limit != null) uriBuilder.setParameter("limit", limit.toString());
        URI uri = ConnectorUtilities.buildUriBuilder(uriBuilder);
        logInfo("find uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(ConnectorUtilities.newStringEntity(objectMapper, searchAttrs));
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        String responseBody = ConnectorUtilities.entityToString(response.getEntity());
        logInfo("find response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
        return Arrays.stream(ConnectorUtilities.readValue(objectMapper, responseBody, HashMap[].class)).collect(Collectors.toList());
    }

    /**
     * Выполнение функции модуля через POST запрос
     *
     * @param httpEntity          http сущность, содержащая body запроса
     * @param methodName          название модуля и функции, вызываемой из модуля (func=modules.moduleCode.methodName)
     * @param params              параметры функции, указанной в параметре func.
     * @param additionalUrlParams дополнительные параметры url
     * @return возвращает весь ответ сервера, его обработка остается на усмотрение пользователя
     */
    public CloseableHttpResponse execPost(
            HttpEntity httpEntity,
            String methodName,
            String params,
            Map<String, String> additionalUrlParams
    ) {
        String PATH_SEGMENT = "exec-post";
        logInfo("execPost (httpEntity, ", methodName, ", ", params, ", ", additionalUrlParams, ")");
        URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
        additionalUrlParams.forEach(uriBuilder::setParameter);
        uriBuilder.setParameter("func", methodName);
        uriBuilder.setParameter("params", params);
        uriBuilder.setParameter("raw", "true");
        URI uri = ConnectorUtilities.buildUriBuilder(uriBuilder);
        logInfo("execPost uri: ", uri);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(httpEntity);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, httpPost);
        HttpException.throwIfNotOk(this, response);
        logInfo("execPost response status: " + response.getStatusLine().getStatusCode());
        return response;
    }

    /**
     * Выполнение функции модуля через GET запрос
     *
     * @param methodName          название модуля и функции, вызываемой из модуля (func=modules.moduleCode.methodName)
     * @param params              параметры функции, указанной в параметре func
     * @param additionalUrlParams дополнительные параметры url
     * @return возвращает весь ответ сервера, его обработка остается на усмотрение пользователя
     */
    public CloseableHttpResponse execGet(
            String methodName,
            String params,
            Map<String, String> additionalUrlParams
    ) {
        logInfo("exec (", methodName, ", ", params, ", ", additionalUrlParams, ")");
        String PATH_SEGMENT = "exec";
        URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
        additionalUrlParams.forEach(uriBuilder::setParameter);
        uriBuilder.setParameter("func", methodName);
        uriBuilder.setParameter("params", params);
        URI uri = ConnectorUtilities.buildUriBuilder(uriBuilder);
        logInfo("exec uri: ", uri);
        CloseableHttpResponse response = ConnectorUtilities.execute(client, new HttpGet(uri));
        HttpException.throwIfNotOk(this, response);
        logInfo("exec response status: " + response.getStatusLine().getStatusCode());
        return response;
    }
}