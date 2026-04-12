package ru.kazantsev.nsmp.basic_api_connector.dto.nsmp;

/**
 * Файл
 */
@SuppressWarnings("unused")
public class FileDto   {
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