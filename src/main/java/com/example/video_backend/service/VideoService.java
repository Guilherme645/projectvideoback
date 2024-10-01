package com.example.video_backend.service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@Service
public class VideoService {

    private final Path rootLocation = Paths.get("C:/pastavideos");
    private final Path cutsLocation = Paths.get("C:/cortesvideos");
    private final String FFMPEG_PATH = "C:/ffmpeg/ffmpeg-7.0.2-full_build/bin/ffmpeg.exe";

    // Lista todos os vídeos que estão na pasta C:/pastavideos
    public List<String> listAllVideosFromDirectory() throws IOException {
        if (!Files.exists(rootLocation)) {
            throw new IOException("Diretório de vídeos não encontrado.");
        }

        List<String> videoList = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(rootLocation, "*.{mp4,mkv,avi}");

        for (Path path : stream) {
            videoList.add(path.getFileName().toString());
        }

        return videoList;
    }


    // Corta um arquivo de vídeo e o salva na pasta C:/cortesvideos
    public String cutVideoFileFromDirectory(String fileName, double startSeconds, double durationSeconds) throws IOException {
        Path inputFile = rootLocation.resolve(fileName);

        // Verifica se o arquivo existe na pasta de vídeos
        if (!Files.exists(inputFile)) {
            throw new IOException("Vídeo não encontrado: " + fileName);
        }

        // Verifica se a pasta C:/cortesvideos existe, senão cria
        if (!Files.exists(cutsLocation)) {
            Files.createDirectories(cutsLocation);
        }

        // Gera um nome único para o arquivo de saída
        String outputFileName = "cut_" + System.currentTimeMillis() + "_" + fileName;
        Path outputPath = cutsLocation.resolve(outputFileName).normalize().toAbsolutePath();

        // Comando FFmpeg para cortar o vídeo
        String[] command = {
                FFMPEG_PATH,
                "-ss", String.valueOf(startSeconds),
                "-i", inputFile.toString(),
                "-t", String.valueOf(durationSeconds),
                "-c", "copy",
                outputPath.toString()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Lê a saída do FFmpeg
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // Aguarda o término do processo
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Erro ao cortar o vídeo, código de saída do FFmpeg: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("O processo de corte do vídeo foi interrompido", e);
        }

        return outputFileName;
    }


    public Resource loadVideoAsResource(String fileName) throws IOException {
        try {
            Path filePath = rootLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // Verifica se o recurso existe e é legível
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("Arquivo de vídeo não encontrado ou não legível: " + fileName);
            }
        } catch (IOException e) {
            throw new IOException("Erro ao carregar o vídeo: " + fileName, e);
        }
    }
}
