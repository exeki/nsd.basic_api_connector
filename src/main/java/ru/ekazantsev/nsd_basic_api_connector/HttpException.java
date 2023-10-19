package ru.ekazantsev.nsd_basic_api_connector;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * Исключение, которое выбрасывается при получении не успешного http ответа
 */
public class HttpException extends RuntimeException {

    protected Integer serverResponseStatus;

    protected CloseableHttpResponse serverResponse;

    /**
     * @param message сообщение
     * @param status  HTTP статус
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
    public CloseableHttpResponse getServerResponse() {
        return this.serverResponse;
    }

    protected static String getErrorText(String host, String status, String body) {
        return "Error when accessing to " + host + ", response status: " + status + ", message:" + body;
    }


    /**
     * Выбрасывает исключение, если в переданном response код не успешный
     * иначе ничего не делает
     * @param connector коннектор
     * @param response ответ nsd
     */
    public static void throwIfNotOk(Connector connector, CloseableHttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 400 || status < 200) {
            String body = ConnectorUtilities.entityToString(response.getEntity());
            throw new HttpException(
                    getErrorText(connector.host, Integer.toString(status), body),
                    status,
                    response
            );
        }
    }

}