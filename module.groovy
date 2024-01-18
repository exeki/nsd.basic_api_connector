package ru.kazantsev.nsd.basic_api_connector

import groovy.transform.Field;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.naming.ConfigurationException;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Field String MODULE_NAME = "nsdBasicApiConnector"

/** DTO со структурой файла конфига коннектора */
public class ConfigFileDto {
    /** Перечень конфигов инталляций */
    public List<InstallationConfig> installations;
    /** Конфиг конкретной инсталляции */
    public static class InstallationConfig {
        /** Идентификатор инсталляции */
        public String id;
        /** Схема, по которой система будет обращаться */
        public String scheme;
        /** Хост инсталляции */
        public String host;
        /** Ключ, по которому будет происходить обращение */
        public String accessKey;
        /** Признак необходимости игнорировать ssl */
        public Boolean ignoreSLL;
    }
}

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

    public void logInfo(Object... objects) {
        if (infoLoggingIsEnabled) {
            StringBuilder message = new StringBuilder();
            for (Object object : objects) {
                if (object == null) message.append("null");
                else message.append(object);
            }
            logger.info(message.toString());
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
            logInfo("create (" + metaClassCode + ", " + attributes + ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
            URI uri = getBasicUriBuilder().setPath(path).build();
            logInfo("create uri: " + uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logInfo("create response status: " + response.getStatusLine().getStatusCode() + " , body: " + EntityUtils.toString(response.getEntity()));
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
            logInfo("addFile (" + targetObjectUuid + ", ", files);
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
            if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
            URI uri = uriBuilder.build();
            logInfo("addFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            for (int i = 0; i < files.size(); i++) {
                entityBuilder.addBinaryBody(String.valueOf(i), files.get(i));
            }
            httpPost.setEntity(entityBuilder.build());
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logInfo("addFile response status: " + response.getStatusLine().getStatusCode() + ". body: "
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
            logInfo("addFile (" + targetObjectUuid + "/, byte[], ", fileName, ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + targetObjectUuid;
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(path);
            if (attrCode != null) uriBuilder.addParameter("attrCode", attrCode);
            URI uri = uriBuilder.build();
            logInfo("addFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", fileBytes, ContentType.TEXT_PLAIN, fileName)
                    .build();
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logInfo("addFile response status: " + response.getStatusLine().getStatusCode()
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
            logInfo("createExcl (" + serviceTimeUuid + ", ", exclusionDate, ")");
            HashMap<String, String> lastSegmentMap = new HashMap<>();
            lastSegmentMap.put("exclusionDate", new SimpleDateFormat(DATE_PATTERN).format(exclusionDate));
            if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
            if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
            String lastSegmentString = this.objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(lastSegmentMap);
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeUuid + "/" + lastSegmentString;
            URI uri = getBasicUriBuilder().setPath(path).build();
            logInfo("createExcl uri: " + uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("createExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
        for(Map.Entry<String, String> entry : entrySet) {
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
            logInfo("createM2M (", metaClassCode, ", ", attributes, ", ", returnAttrs, ")");
            String path = BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode;
            URIBuilder builder = getBasicUriBuilder().setPath(path);
            if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logInfo("createM2M uri: " + uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("createM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("createM2MMultiple (", objects, ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT).build();
            logInfo("createM2MMultiple uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(objects));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("createM2MMultiple response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("delete (" + objectUuid + ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid).build();
            logInfo("delete uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            logInfo("delete response status: "
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
            logInfo("edit (" + objectUuid + ", ", attributes, ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid).build();
            logInfo("edit uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logInfo("edit response status: " + response.getStatusLine().getStatusCode()
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
            logInfo("editExcl ( " + serviceTimeExclusion + ", ", startTime, ", ", endTime, ")");
            HashMap<String, String> lastSegmentMap = new HashMap<>();
            lastSegmentMap.put("exclusionDate", serviceTimeExclusion);
            if (startTime != null) lastSegmentMap.put("startTime", startTime.toString());
            if (endTime != null) lastSegmentMap.put("endTime", endTime.toString());
            String lastSegmentString = this.objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(lastSegmentMap);
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + serviceTimeExclusion + "/" + lastSegmentString).build();
            logInfo("editExcl uri: ${uri}");
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("editExcl response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("editM2M (", objectUuid, ", ", attributes, ", ", returnAttrs, ")");
            URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
            if (returnAttrs != null) builder.addParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logInfo("editM2M uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(attributes));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("editM2M response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("execFile (byte[])");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT).build();
            logInfo("execFile uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("script", byteArray, ContentType.TEXT_PLAIN, "script.groovy")
                    .build();
            httpPost.setEntity(entity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("execFile response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("get (" + objectUuid + ", ", returnAttrs, ")");
            URIBuilder builder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + objectUuid);
            if (returnAttrs != null) builder.setParameter("attrs", String.join(",", returnAttrs));
            URI uri = builder.build();
            logInfo("get uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("get response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("getFile (" + fileUuid + ")");
            URI uri = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + fileUuid).build();
            logInfo("getFile uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            String contentDisposition = response.getFirstHeader("Content-Disposition").getValue();
            int index = contentDisposition.indexOf('=');
            NsdDto.FileDto file = new NsdDto.FileDto(
                    EntityUtils.toByteArray(response.getEntity()),
                    contentDisposition.substring(index + 2, contentDisposition.length() - 1),
                    response.getFirstHeader("Content-Type").getValue()
            );
            logInfo("getFile response status: " + response.getStatusLine().getStatusCode() + ". body: byte[]");
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
            logInfo("find (", metaClassCode, ", ", searchAttrs, ", ", returnAttrs, ", ", offset, ", ", limit, ")");
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT + "/" + metaClassCode);
            if (returnAttrs != null) uriBuilder.setParameter("attrs", String.join(",", returnAttrs));
            if (offset != null) uriBuilder.setParameter("offset", offset.toString());
            if (limit != null) uriBuilder.setParameter("limit", limit.toString());
            URI uri = uriBuilder.build();
            logInfo("find uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(newStringEntity(searchAttrs));
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            String responseBody = EntityUtils.toString(response.getEntity());
            logInfo("find response status: " + response.getStatusLine().getStatusCode() + ". body: " + responseBody);
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
            logInfo("execPost (httpEntity, ", methodName, ", ", params, ", ", additionalUrlParams, ")");
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
            if (additionalUrlParams != null) {
                for(Map.Entry<String, String> entry : additionalUrlParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }
            uriBuilder.setParameter("func", methodName);
            uriBuilder.setParameter("params", params);
            uriBuilder.setParameter("raw", "true");
            URI uri = uriBuilder.build();
            logInfo("execPost uri: ", uri);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(httpEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            HttpException.throwIfNotOk(this, response);
            logInfo("execPost response status: " + response.getStatusLine().getStatusCode());
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
            logInfo("exec (", methodName, ", ", params, ", ", additionalUrlParams, ")");
            String PATH_SEGMENT = "exec";
            URIBuilder uriBuilder = getBasicUriBuilder().setPath(BASE_PATH + "/" + PATH_SEGMENT);
            if (additionalUrlParams != null) {
                for(Map.Entry<String, String> entry : additionalUrlParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }
            uriBuilder.setParameter("func", methodName);
            uriBuilder.setParameter("params", params);
            URI uri = uriBuilder.build();
            logInfo("exec uri: ", uri);
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            HttpException.throwIfNotOk(this, response);
            logInfo("exec response status: " + response.getStatusLine().getStatusCode());
            return response;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * DTO, содержит параметры для связи с NSD и методы по их упрощенному получению
 */
public class ConnectorParams {
    /**
     * Расположение конфигурационного файла по умолчанию
     */
    private static final String DEFAULT_PARAMS_FILE_PATH = System.getProperty("user.home") + "\\nsd_sdk\\conf\\nsd_connector_params.json";

    /**
     * Пользовательский идентификатор
     */

    private String userId;
    /**
     * Ключ доступа
     */
    private String accessKey;
    /**
     * Хост
     */
    private String host;
    /**
     * Схема (http/https)
     */
    private String scheme;
    /**
     * Признак необходимости игнорировать ssl
     */
    private Boolean ignoreSSL;

    /**
     * Конструктор для ручного сбора параметров
     * @param userId пользовательский идентификатор
     * @param scheme Схема (http/https)
     * @param host Хост
     * @param accessKey Ключ доступа
     * @param ignoreSSL Признак необходимости игнорировать ssl
     */

    public ConnectorParams(
            String userId,
            String scheme,
            String host,
            String accessKey,
            Boolean ignoreSSL
    ) {
        this.userId = userId;
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSSL = ignoreSSL;
    }

    protected ConnectorParams() {}

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/nsd_sdk/conf/nsd_connector_params.json
     * @param installationId ID инсталляции, указанный конфигурационном файле
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException выбрасывается если не удается считать json
     */
    public static ConnectorParams byConfigFile(String installationId) throws ConfigurationException, IOException {
        return byConfigFileInPath(installationId, DEFAULT_PARAMS_FILE_PATH);
    }

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/nsd_sdk/conf/nsd_connector_params.json
     * @param installationId ID инсталляции, указанный конфигурационном файле
     * @param pathToConfigFile путь до конфигурационного файла
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException выбрасывается если не удается считать json
     */
    public static ConnectorParams byConfigFileInPath(String installationId, String pathToConfigFile) throws IOException, ConfigurationException {
        File configFile = new File(pathToConfigFile);
        if (!configFile.exists())
            throw new FileNotFoundException("The configuration file was not found at " + pathToConfigFile);

        ConfigFileDto configFileDto;

        try {
            configFileDto = new ObjectMapper().readValue(configFile, ConfigFileDto.class);
        } catch (IOException e) {
            throw new IOException("Data could not be read from the configuration file at " + pathToConfigFile + ". Error text:" + e.getMessage());
        }

        List<ConfigFileDto.InstallationConfig> installationConfigs = new ArrayList<>();

        for(ConfigFileDto.InstallationConfig config : configFileDto.installations) {
            if(Objects.equals(config.id, installationId)) installationConfigs.add(config);
        }

        if (installationConfigs.size() != 1) {
            throw new ConfigurationException("Installation configuration "+  installationId + " could not be obtained " +
                    "in the configuration file at " + pathToConfigFile);
        }
        ConfigFileDto.InstallationConfig installationConfig = installationConfigs.get(0);
        if (installationConfig.host == null || installationConfig.host.trim().length() == 0) {
            throw new ConfigurationException("The host for installation is not specified" + installationId
                    + " in the configuration file at " + pathToConfigFile);
        }
        if (installationConfig.accessKey == null || installationConfig.accessKey.trim().length() == 0) {
            throw new ConfigurationException("accessKey for installation is not specified for " + installationId +
                    " in the configuration file at " + pathToConfigFile);
        }
        if (installationConfig.scheme == null || installationConfig.scheme.trim().length() == 0) {
            throw new ConfigurationException("Scheme is not specified for installation" + installationId +
                    " in the configuration file at " + pathToConfigFile);
        }
        return new ConnectorParams(
                installationId,
                installationConfig.scheme,
                installationConfig.host,
                installationConfig.accessKey,
                installationConfig.ignoreSLL != null ? installationConfig.ignoreSLL : false
        );
    }

    protected String getAccessKey() {
        return accessKey;
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public Boolean isIgnoringSSL(){
        return ignoreSSL;
    }

    public String getUserId() {return userId; }

}

/**
 * Исключение, которое выбрасывается при получении не успешного http ответа
 */
public class HttpException extends RuntimeException {

    protected Integer serverResponseStatus;

    protected CloseableHttpResponse serverResponse;

    /**
     * @param message  сообщение
     * @param status   HTTP статус
     * @param response полный ответ сервера
     */
    public HttpException(String message, Integer status, CloseableHttpResponse response) {
        super(message);
        this.serverResponseStatus = status;
        this.serverResponse = response;
    }

    /**
     * Получить статус ответа
     *
     * @return статус ответа
     */
    public Integer getServerResponseStatus() {
        return this.serverResponseStatus;
    }

    /**
     * Получить body ответа
     *
     * @return body ответа
     */
    public HttpResponse getServerResponse() {
        return this.serverResponse;
    }

    /**
     * Создает текст исключения по шаблону
     * @param host хост, к которому происходит обращение
     * @param status статус ответа
     * @param body тело ответа
     * @return текстовка исключения
     */
    public static String createErrorText(String host, String status, String body) {
        return "Error when accessing to " + host + ", response status: " + status + ", message:" + body;
    }

    /**
     * Выбрасывает исключение, если в переданном response код не успешный
     * иначе ничего не делает
     *
     * @param connector коннектор
     * @param response  ответ nsd
     */
    public static void throwIfNotOk(Connector connector, CloseableHttpResponse response) {
        try {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400 || status < 200) {
                String body = EntityUtils.toString(response.getEntity());
                throw new HttpException(
                        createErrorText(connector.host, Integer.toString(status), body),
                        status,
                        response
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

/**
 * Перечень DTO, которые фиксированно возвращаются из штатных методов REST API NSD
 */
public class NsdDto {

    static abstract class AbstractNsdDto {
        @Override
        public String toString() {
            try {
                return new ObjectMapper()
                        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                        .writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Исключение в классе обслуживания
     */
    public static class ServiceTimeExclusionDto extends AbstractNsdDto {
        @JsonAlias("UUID")
        public String uuid;
        public Long startTime;
        public Long endTime;
        @JsonFormat(pattern = "yyyy-MM-dd")
        public Date exclusionDate;
    }

    /**
     * Файл
     */
    public static class FileDto extends AbstractNsdDto {
        public byte[] bytes;
        public String title;
        public String contentType;

        public FileDto(byte[] bytes, String title, String contentType) {
            this.bytes = bytes;
            this.title = title;
            this.contentType = contentType;
        }

        @Override
        public String toString() {
            return this.title + " / " + this.contentType;
        }
    }
}