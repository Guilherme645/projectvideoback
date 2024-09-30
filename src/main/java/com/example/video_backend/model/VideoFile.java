package com.example.video_backend.model;

@Entity
@Table(name = "video_files")
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "size")
    private long size;

    @Column(name = "duration")
    private long duration;  // duração em segundos

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    public VideoFile() {
        // Construtor padrão
    }

    public VideoFile(String fileName, String filePath, long size, long duration, LocalDateTime uploadTime) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.size = size;
        this.duration = duration;
        this.uploadTime = uploadTime;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    @Override
    public String toString() {
        return "VideoFile{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", size=" + size +
                ", duration=" + duration +
                ", uploadTime=" + uploadTime +
                '}';
    }
}