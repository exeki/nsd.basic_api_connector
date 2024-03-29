package ru.kazantsev.nsd.basic_api_connector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

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