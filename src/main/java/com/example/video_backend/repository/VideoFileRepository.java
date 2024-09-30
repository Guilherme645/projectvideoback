package com.example.video_backend.repository;

import com.example.video.model.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {
    // Métodos customizados, se necessário
}