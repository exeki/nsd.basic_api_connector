package ru.kazantsev.nsmp.basic_api_connector.exception;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public final class ResponseSnapshot {

    final int status;
    final String reasonPhrase;
    final ProtocolVersion protocolVersion;
    final Header[] headers;
    final  String contentType;
    final long contentLength;
    final byte[] body;

    public String getBodyAsString() {
        return getBodyAsString(StandardCharsets.UTF_8);
    }

    public String getBodyAsString(Charset charset) {
        return new String(body, charset);
    }

    public ResponseSnapshot(ClassicHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        this.status = response.getCode();
        this.reasonPhrase = response.getReasonPhrase();
        this.protocolVersion = response.getVersion();
        this.headers = response.getHeaders();
        this.contentType = entity != null ? entity.getContentType() : null;
        this.contentLength = entity != null ? entity.getContentLength() : -1;
        this.body = entity != null ? EntityUtils.toByteArray(entity) : new byte[0];
    }

    public int getStatus() {
        return status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public byte[] getBody() {
        return body;
    }
}
