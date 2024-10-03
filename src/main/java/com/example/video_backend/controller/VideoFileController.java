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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoFileController {

    @Autowired

    private VideoService videoService;
    private final String UPLOAD_DIR = "C:/pastavideos";
    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subFolderName") String subFolderName) {
        try {
            // Cria a subpasta se ela não existir
            Path folderPath = Paths.get(UPLOAD_DIR, subFolderName);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath); // Cria a subpasta
            }

            // Salva o arquivo na subpasta
            Path filePath = folderPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("Vídeo enviado com sucesso para: " + filePath.toString());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao salvar o vídeo: " + e.getMessage());
        }
    }

    // Endpoint para listar todos os vídeos na pasta C:/pastavideos
    @GetMapping("/list/{folderName}")
    public ResponseEntity<List<String>> listVideosFromFolder(@PathVariable String folderName) {
        try {
            List<String> videos = videoService.listVideosFromFolder(folderName);
            if (videos.isEmpty()) {
                return ResponseEntity.noContent().build();  // 204 se não houver vídeos
            }
            return ResponseEntity.ok(videos);  // 200 OK com a lista de vídeos
        } catch (IOException e) {
            return ResponseEntity.status(500).body(List.of("Erro ao listar vídeos na pasta: " + folderName));
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<List<String>> listAllFolders() {
        try {
            List<String> folders = videoService.listAllFolders();
            if (folders.isEmpty()) {
                return ResponseEntity.noContent().build();  // 204 se não houver pastas
            }
            return ResponseEntity.ok(folders);  // 200 OK com a lista de pastas
        } catch (IOException e) {
            return ResponseEntity.status(500).body(List.of("Erro ao listar pastas: " + e.getMessage()));
        }
    }

    // Endpoint para cortar um vídeo da pasta C:/pastavideos
    @PostMapping("/cut")
    public ResponseEntity<?> cutVideo(@RequestParam String folderName,
                                      @RequestParam String fileName,
                                      @RequestParam double startSeconds,
                                      @RequestParam double durationSeconds) {
        try {
            // Passe o nome da pasta e o nome do arquivo para o serviço de corte de vídeo
            String outputFileName = videoService.cutVideoFileFromDirectory(folderName, fileName, startSeconds, durationSeconds);
            return ResponseEntity.ok("Vídeo cortado e salvo em: " + outputFileName);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao cortar o vídeo: " + e.getMessage());
        }
    }

    @GetMapping("/play/{folderName}/{fileName}")
    public ResponseEntity<Resource> playVideo(@PathVariable String folderName, @PathVariable String fileName) {
        try {
            Resource video = videoService.loadVideoFromFolderAsResource(folderName, fileName);
            if (video.exists() && video.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(video);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/cuts/folders")
    public ResponseEntity<List<String>> listCutFolders() {
        try {
            List<String> folders = videoService.listCutFolders();  // Certifique-se que este método existe
            if (folders.isEmpty()) {
                return ResponseEntity.noContent().build();  // 204 se não houver pastas
            }
            return ResponseEntity.ok(folders);  // 200 OK com a lista de pastas
        } catch (IOException e) {
            return ResponseEntity.status(500).body(List.of("Erro ao listar pastas: " + e.getMessage()));
        }
    }
    @GetMapping("/cuts/list/{folderName}")
    public ResponseEntity<List<String>> listVideosFromCutFolder(@PathVariable String folderName) {
        try {
            List<String> videos = videoService.listVideosFromCutFolder(folderName);
            if (videos.isEmpty()) {
                return ResponseEntity.noContent().build();  // 204 se não houver vídeos
            }
            return ResponseEntity.ok(videos);  // 200 OK com a lista de vídeos
        } catch (IOException e) {
            return ResponseEntity.status(500).body(List.of("Erro ao listar vídeos na pasta de cortes: " + folderName));
        }
    }
    @GetMapping("/cuts/play/{folderName}/{fileName}")
    public ResponseEntity<Resource> playCutVideo(@PathVariable String folderName, @PathVariable String fileName) {
        try {
            // Monta o caminho para a pasta de cortes
            Path cutsLocation = Paths.get("C:/cortesvideos").resolve(folderName).normalize();
            Path filePath = cutsLocation.resolve(fileName).normalize();

            // Carrega o vídeo como um recurso
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Retorna o vídeo como uma resposta
                return ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(404).body(null);  // Retorna 404 se o vídeo não for encontrado
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);  // Retorna 500 em caso de erro no servidor
        }
    }
    @GetMapping("/video/cut/progress/{folderName}/{fileName}")
    public Double getCutProgress(@PathVariable String folderName, @PathVariable String fileName) {
        return videoService.getVideoCutProgress(folderName, fileName);
    }
}
