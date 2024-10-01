package com.example.video_backend.model;


import javax.persistence.*;

@Entity
@Table(name = "video_file")
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome doa arquivo de vídeo
    private String fileName;

    // Caminho completo onde o arquivo está armazenado no sistema de arquivos
    private String filePath;

    // Tamanho do arquivo em bytes
    private long size;

    // Duração do vídeo em segundos
    private long duration;

    // Construtor padrão (necessário para o JPA)
    public VideoFile() {
    }

    // Construtor com parâmetros
    public VideoFile(String fileName, String filePath, long size, long duration) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.size = size;
        this.duration = duration;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    // O ID é gerado automaticamente, então geralmente não precisamos de um setter para ele

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
