package ru.kazantsev.nsd.basic_api_connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    protected static final String CHARSET = "UTF-8";
    protected static Logger logger = LoggerFactory.getLogger(Connector.class);

    protected String scheme;
    protected String host;
    protected String accessKey;
    protected Boolean debugLoggingIsEnabled = true;

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
        if (params.isIgnoringSSL()) clientBuilder.setSSLSocketFactory(getNoSslSocketFactory());
        this.client = clientBuilder.build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(new SimpleDateFormat(DATE_PATTERN));
    }

    static class TrustAllStrategy implements TrustStrategy {

        public static final TrustAllStrategy INSTANCE = new TrustAllStrategy();

        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return true;
        }
    }

    /**
     * Возвращает SSLConnectionSocketFactory с выключенным ssl
     *
     * @return SSLConnectionSocketFactory с выключенным ssl
     */
    protected static SSLConnectionSocketFactory getNoSslSocketFactory() {
        TrustStrategy acceptingTrustStrategy = TrustAllStrategy.INSTANCE;
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    public void logDebug(Object... objects) {
        if (debugLoggingIsEnabled) {
            StringBuilder message = new StringBuilder();
            for (Object object : objects) {
                if (object == null) message.append("null");
                else message.append(object);
            }
            logger.debug(message.toString());
        }
    }

    public void setDebugLogging(Boolean boo) {
        this.debugLoggingIsEnabled = boo;
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
     *
     * @return базовый конструктор URI
     */
    protected URIBuilder getBasicUriBuilder() {
        return new URIBuilder().setScheme(scheme).setHost(host).addParameter(ACCESS_KEY_PARAM_NAME, accessKey);
    }

    protected StringEntity newStringEntity(Object value) {
        try {
            return new StringEntity(objectMapper.writeValueAsString(value), CHARSET);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создание объекта (метод rest api 'create')
     *
     * @param metaClassCode fqn создаваемого объекта, например, serviceCall.
     * @param attributes    атрибуты создаваемого объекта.
     */
    public void create(String metaClassCode, Map<String, Object> attributes) {
        try {
            String PATH_SEGMENT = "create";
            logDebug("create (" + metaClassCode + ", " + attributes + ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("create uri: " + uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("create response status: " + response.getStatusLine().getStatusCode() + " , body: " + EntityUtils.toString(response.getEntity()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "add-file";
            logDebug("addFile (" + targetObjectUuid + ", ", files);
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
            if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
            URI uri = uriBuilder.build();
            logDebug("addFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            for (int i = 0; i < files.size(); i++) {
                entityBuilder.addBinaryBody(String.valueOf(i), files.get(i));
            }
            httpPost.setEntity(entityBuilder.build());
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("addFile response status: " + response.getStatusLine().getStatusCode() + ". body: "
                    + EntityUtils.toString(response.getEntity()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "add-file";
            logDebug("addFile (" + targetObjectUuid + "/, byte[], ", fileName, ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
            if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
            URI uri = uriBuilder.build();
            logDebug("addFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", fileBytes, ContentType.TEXT_PLAIN, fileName)
                    .build();
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("addFile response status: " + response.getStatusLine().getStatusCode()
                    + ". body: " + EntityUtils.toString(response.getEntity()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "create-excl";
            logDebug("createExcl (" + serviceTimeUuid + ", ", exclusionDate, ")");
            HashMap<String, String> lastSegmentMap = new HashMap<>();
            lastSegmentMap.put("exclusionDate", new SimpleDateFormat(DATE_PATTERN).format(exclusionDate));
            if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
            if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
            String lastSegmentString = this.objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(lastSegmentMap);
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeUuid + "/" + lastSegmentString;
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("createExcl uri: " + uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("createExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return objectMapper.readValue(responseBody, NsdDto.ServiceTimeExclusionDto.class);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Делает из Map JSON строку, которую потом можно затолкать в url
     *
     * @param map из этого будет создан JSON
     * @return JSON
     */
    protected static String createJsonForUrl(HashMap<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        int size = entrySet.size();
        int index = 0;
        for (Map.Entry<String, String> entry : entrySet) {
            index++;
            stringBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            if (index != size) stringBuilder.append(",");
        }
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
        try {
            String PATH_SEGMENT = "create-m2m";
            logDebug("createM2M (", metaClassCode, ", ", attributes, ", ", returnAttrs, ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
            URIBuilder builder = getBasicUriBuilder().setPath(path);
            if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logDebug("createM2M uri: " + uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("createM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return objectMapper.readValue(responseBody, HashMap.class);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создание множества объектов для машинного взаимодействия
     *
     * @param objects лист с атрибутами создаваемых объектов
     * @return Массив объектов с UUID для созданных и переданную информацию для создания объекта с сообщением об ошибке в поле error для не созданных.
     */
    public List<HashMap> createM2MMultiple(List<Map<String, Object>> objects) {
        try {
            String PATH_SEGMENT = "create-m2m-multiple";
            logDebug("createM2MMultiple (", objects, ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT).build();
            logDebug("createM2MMultiple uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(objects));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("createM2MMultiple response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return Arrays.stream(objectMapper.readValue(responseBody, HashMap[].class)).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Удаление объекта
     *
     * @param objectUuid uuid удаляемого объекта, например, serviceCall$501.
     */
    public void delete(String objectUuid) {
        try {
            String PATH_SEGMENT = "delete";
            logDebug("delete (" + objectUuid + ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid).build();
            logDebug("delete uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            logDebug("delete response status: "
                    + response.getStatusLine().getStatusCode() + ". body: "
                    + EntityUtils.toString(response.getEntity()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Редактирование объекта
     *
     * @param objectUuid uuid изменяемого объекта, например, serviceCall$501.
     * @param attributes изменяемые атрибуты.
     */
    public void edit(String objectUuid, Map<String, Object> attributes) {
        try {
            String PATH_SEGMENT = "edit";
            logDebug("edit (" + objectUuid + ", ", attributes, ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid).build();
            logDebug("edit uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("edit response status: " + response.getStatusLine().getStatusCode()
                    + ". body: " + EntityUtils.toString(response.getEntity()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "edit-excl";
            logDebug("editExcl ( " + serviceTimeExclusion + ", ", startTime, ", ", endTime, ")");
            HashMap<String, String> lastSegmentMap = new HashMap<>();
            lastSegmentMap.put("exclusionDate", serviceTimeExclusion);
            if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
            if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
            String lastSegmentString = this.objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(lastSegmentMap);
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeExclusion + "/" + lastSegmentString).build();
            logDebug("editExcl uri: ${uri}");
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("editExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return objectMapper.readValue(responseBody, NsdDto.ServiceTimeExclusionDto.class);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "edit-m2m";
            logDebug("editM2M (", objectUuid, ", ", attributes, ", ", returnAttrs, ")");
            URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
            if (returnAttrs != null) builder.addParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logDebug("editM2M uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("editM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return objectMapper.readValue(responseBody, HashMap.class);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "exec";
            logDebug("execFile (byte[])");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT).build();
            logDebug("execFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("script", byteArray, ContentType.TEXT_PLAIN, "script.groovy")
                    .build();
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("execFile response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return responseBody;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "get";
            logDebug("get (" + objectUuid + ", ", returnAttrs, ")");
            URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
            if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logDebug("get uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("get response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return objectMapper.readValue(responseBody, HashMap.class);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получение контента файла по его UUID
     *
     * @param fileUuid uuid файла
     * @return DTO содержащий информацию о файле
     */
    public NsdDto.FileDto getFile(String fileUuid) {
        try {
            String PATH_SEGMENT = "get-file";
            logDebug("getFile (" + fileUuid + ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + fileUuid).build();
            logDebug("getFile uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String contentDisposition = response.getFirstHeader("Content-Disposition").getValue();
            int index = contentDisposition.indexOf('=');
            NsdDto.FileDto file = new NsdDto.FileDto(
                    EntityUtils.toByteArray(response.getEntity()),
                    contentDisposition.substring(index + 2, contentDisposition.length() - 1),
                    response.getFirstHeader("Content-Type").getValue()
            );
            logDebug("getFile response status: " + response.getStatusLine().getStatusCode() + ". body: byte[]");
            return file;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "find";
            logDebug("find (", metaClassCode, ", ", searchAttrs, ", ", returnAttrs, ", ", offset, ", ", limit, ")");
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode);
            if (returnAttrs != null) uriBuilder.setParameter("attrs", String.join(",", returnAttrs));
            if (offset != null) uriBuilder.setParameter("offset", offset.toString());
            if (limit != null) uriBuilder.setParameter("limit", limit.toString());
            URI uri = uriBuilder.build();
            logDebug("find uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(searchAttrs));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logDebug("find response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
            return Arrays.stream(objectMapper.readValue(responseBody, HashMap[].class)).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            String PATH_SEGMENT = "exec-post";
            logDebug("execPost (httpEntity, ", methodName, ", ", params, ", ", additionalUrlParams, ")");
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
            if (additionalUrlParams != null) {
                for (Map.Entry<String, String> entry : additionalUrlParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }
            uriBuilder.setParameter("func", methodName);
            uriBuilder.setParameter("params", params);
            uriBuilder.setParameter("raw", "true");
            URI uri = uriBuilder.build();
            logDebug("execPost uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(httpEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("execPost response status: " + response.getStatusLine().getStatusCode());
            return response;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
        try {
            logDebug("exec (", methodName, ", ", params, ", ", additionalUrlParams, ")");
            String PATH_SEGMENT = "exec";
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
            if (additionalUrlParams != null) {
                for (Map.Entry<String, String> entry : additionalUrlParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }
            uriBuilder.setParameter("func", methodName);
            uriBuilder.setParameter("params", params);
            URI uri = uriBuilder.build();
            logDebug("exec uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            logDebug("exec response status: " + response.getStatusLine().getStatusCode());
            return response;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить версию приложения инсталляции
     *
     * @return строка с версией
     */
    public String version() {
        try {
            logDebug("version");
            String path = "/sd/services/smpsync/version";
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("version uri: " + uri);
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            HttpException.throwIfNotOk(this, response);
            String body = EntityUtils.toString(response.getEntity());
            logDebug("version response status: " + response.getStatusLine().getStatusCode() + " , body: " + body);
            return body;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить версию groovy инсталляции
     *
     * @return строка с версией
     */
    public String groovyVersion() {
        try {
            logDebug("groovy_version");
            String path = "/sd/services/smpsync/groovy_version";
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("groovy_version uri: " + uri);
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            HttpException.throwIfNotOk(this, response);
            String body = EntityUtils.toString(response.getEntity());
            logDebug("groovy_version response status: " + response.getStatusLine().getStatusCode() + " , body: " + body);
            return body;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить ip инсталляции (наверное)
     *
     * @return строка с ip
     */
    public String jpdaInfo() {
        try {
            logDebug("jpda_info");
            String path = "/sd/services/smpsync/jpda_info";
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("jpda_info uri: " + uri);
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            HttpException.throwIfNotOk(this, response);
            String body = EntityUtils.toString(response.getEntity());
            logDebug("jpda_info response status: " + response.getStatusLine().getStatusCode() + " , body: " + body);
            return body;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить метаинформацию с инсталляции
     *
     * @param timeoutInMillis ожидание ответа в миллисекундаз
     * @return строка с xml-ником метаинформации
     */
    public String metainfo(int timeoutInMillis) throws SocketException, SocketTimeoutException {
        try {
            logDebug("metainfo");
            String path = "/sd/services/smpsync/metainfo";
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("metainfo uri: " + uri);
            HttpGet httpGet = new HttpGet(uri);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeoutInMillis)
                    .setConnectTimeout(timeoutInMillis)
                    .setConnectionRequestTimeout(timeoutInMillis)
                    .build();
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = client.execute(httpGet);
            HttpException.throwIfNotOk(this, response);
            String body = EntityUtils.toString(response.getEntity());
            logDebug("metainfo response status: " + response.getStatusLine().getStatusCode());
            return body;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить метаинформацию с инсталляции со стандартным таймаутом в 100 000 миллисекунд
     *
     * @return строка с xml-ником метаинформации
     */
    public String metainfo() throws SocketException, SocketTimeoutException{
        return metainfo(100000);
    }

    /**
     * Загрузить метаинформацию
     *
     * @param xmlFileContent строка xml файла конфигурации
     * @param timeoutInMillis таймаут ответа
     */
    public void uploadMetainfo(String xmlFileContent, Integer timeoutInMillis) throws SocketException, SocketTimeoutException {
        try {
            logDebug("upload-metainfo");
            String path = "/sd/services/rest/upload-metainfo";
            URI uri = getBasicUriBuilder().setPath(path).build();
            logDebug("upload-metainfo uri: " + uri);
            HttpPost httpPost = new HttpPost(uri);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("metainfo", xmlFileContent.getBytes(), ContentType.APPLICATION_XML, "metainfo.xml")
                    .build();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeoutInMillis)
                    .setConnectTimeout(timeoutInMillis)
                    .setConnectionRequestTimeout(timeoutInMillis)
                    .build();
            httpPost.setConfig(requestConfig);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logDebug("upload-metainfo response status: " + response.getStatusLine().getStatusCode());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Загрузить метаинформацию со стандартным таймаутом в 15 минут
     *
     * @param xmlFileContent строка xml файла конфигурации
     */
    public void uploadMetainfo(String xmlFileContent) throws SocketException, SocketTimeoutException{
        int TIMEOUT = 15 * 60 * 1000;
        uploadMetainfo(xmlFileContent, TIMEOUT);
    }
}
