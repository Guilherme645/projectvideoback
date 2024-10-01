package com.example.video_backend.controller;
import com.example.video_backend.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaTypeFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/videos")
public class VideoFileController {

    @Autowired
    private VideoService videoService;

    // Endpoint para listar todos os vídeos na pasta C:/pastavideos
    @GetMapping("/list")
    public ResponseEntity<?> listVideos() {
        try {
            return ResponseEntity.ok(videoService.listAllVideosFromDirectory());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao listar vídeos.");
        }
    }

    // Endpoint para cortar um vídeo da pasta C:/pastavideos
    @PostMapping("/cut")
    public ResponseEntity<?> cutVideo(@RequestParam String fileName,
                                      @RequestParam double startSeconds,
                                      @RequestParam double durationSeconds) {
        try {
            String outputFileName = videoService.cutVideoFileFromDirectory(fileName, startSeconds, durationSeconds);
            return ResponseEntity.ok("Vídeo cortado e salvo como: " + outputFileName);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao cortar o vídeo.");
        }
    }

    // Método para reproduzir vídeos
    @GetMapping("/play/{fileName}")
    public ResponseEntity<Resource> playVideo(@PathVariable String fileName) {
        try {
            Resource video = videoService.loadVideoAsResource(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(video);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
