package com.example.video_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final Path rootLocation;
    private final Path cutsLocation;
    private final String ffmpegPath;
    private ConcurrentMap<String, Double> progressMap = new ConcurrentHashMap<>();

    // Injetando valores do application.properties
    public VideoService(
            @Value("${video.upload-dir}") String uploadDir,
            @Value("${video.cuts-dir}") String cutsDir,
            @Value("${video.ffmpeg-path}") String ffmpegPath) {
        this.rootLocation = Paths.get(uploadDir);
        this.cutsLocation = Paths.get(cutsDir);
        this.ffmpegPath = ffmpegPath;
    }

    // Método para carregar o vídeo como recurso
    public Resource loadVideoFromFolderAsResource(String folderName, String fileName) throws IOException {
        Path folderPath = rootLocation.resolve(folderName).normalize();
        Path filePath = folderPath.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Arquivo de vídeo não encontrado ou não legível: " + filePath);
        }
    }

    // Lista todas as subpastas no diretório de vídeos
    public List<String> listAllFolders() throws IOException {
        if (!Files.exists(rootLocation) || !Files.isDirectory(rootLocation)) {
            throw new IOException("Diretório de vídeos não encontrado: " + rootLocation);
        }

        return Files.walk(rootLocation, 1)
                .filter(Files::isDirectory)
                .filter(path -> !path.equals(rootLocation))
                .map(path -> rootLocation.relativize(path).toString())
                .collect(Collectors.toList());
    }

    // Lista todos os vídeos dentro de uma pasta específica
    public List<String> listVideosFromFolder(String folderName) throws IOException {
        Path folderPath = rootLocation.resolve(folderName);
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IOException("Pasta não encontrada: " + folderName);
        }

        return Files.walk(folderPath, 1)
                .filter(Files::isRegularFile)
                .map(path -> folderPath.relativize(path).toString())
                .filter(this::isVideoFile)
                .collect(Collectors.toList());
    }

    // Verifica se o arquivo é um vídeo com as extensões permitidas
    private boolean isVideoFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".mp4") ||
                lowerCaseFileName.endsWith(".avi") ||
                lowerCaseFileName.endsWith(".mkv");
    }

    // Método para cortar um vídeo e salvar na pasta de cortes
    public String cutVideoFileFromDirectory(String folderName, String fileName, double startSeconds, double durationSeconds) throws IOException {
        String videoId = folderName + "_" + fileName;
        Path inputFile = rootLocation.resolve(folderName).resolve(fileName);

        if (!Files.exists(inputFile)) {
            throw new IOException("Vídeo não encontrado: " + fileName);
        }

        // Cria o nome da subpasta com a data atual no formato yyyy-MM-dd
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path dateSubFolder = cutsLocation.resolve(currentDate);
        if (!Files.exists(dateSubFolder)) {
            Files.createDirectories(dateSubFolder);
        }

        // Gera o nome do arquivo cortado
        String outputFileName = "cut_" + System.currentTimeMillis() + "_" + fileName;
        Path outputFilePath = dateSubFolder.resolve(outputFileName).normalize().toAbsolutePath();

        // Comando do FFmpeg para cortar o vídeo
        String[] command = {
                ffmpegPath,
                "-ss", String.valueOf(startSeconds),
                "-i", inputFile.toString(),
                "-t", String.valueOf(durationSeconds),
                "-c", "copy",
                outputFilePath.toString()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Lê a saída do FFmpeg e atualiza o progresso
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            int frameCount = 0;
            while ((line = reader.readLine()) != null) {
                frameCount++;
                double progress = (double) frameCount / 100;
                progressMap.put(videoId, Math.min(progress, 1.0));
            }
        }

        // Espera o processo terminar
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("O processo de corte foi interrompido.", e);
        }

        if (exitCode != 0) {
            throw new IOException("Erro ao cortar o vídeo, código de saída do FFmpeg: " + exitCode);
        }

        progressMap.put(videoId, 1.0);

        return outputFilePath.toString();
    }

    // Método para obter o progresso do corte
    public Double getVideoCutProgress(String folderName, String fileName) {
        String videoId = folderName + "_" + fileName;
        return progressMap.getOrDefault(videoId, 0.0);
    }

    // Lista todas as subpastas no diretório de cortes
    public List<String> listAllCutFolders() throws IOException {
        if (!Files.exists(cutsLocation) || !Files.isDirectory(cutsLocation)) {
            throw new IOException("Diretório de cortes não encontrado: " + cutsLocation);
        }

        return Files.walk(cutsLocation, 1)
                .filter(Files::isDirectory)
                .filter(path -> !path.equals(cutsLocation))
                .map(path -> cutsLocation.relativize(path).toString())
                .collect(Collectors.toList());
    }

    // Lista todos os vídeos dentro de uma pasta de cortes específica
    public List<String> listVideosFromCutFolder(String folderName) throws IOException {
        Path cutsFolderPath = cutsLocation.resolve(folderName);
        if (!Files.exists(cutsFolderPath) || !Files.isDirectory(cutsFolderPath)) {
            throw new IOException("Pasta de cortes não encontrada: " + folderName);
        }

        return Files.walk(cutsFolderPath, 1)
                .filter(Files::isRegularFile)
                .map(path -> cutsFolderPath.relativize(path).toString())
                .filter(this::isVideoFile)
                .collect(Collectors.toList());
    }

public List<String> listCutFolders() throws IOException {
    Path cutsLocation = Paths.get("C:/cortesvideos");

    if (!Files.exists(cutsLocation) || !Files.isDirectory(cutsLocation)) {
        throw new IOException("Diretório de cortes não encontrado: " + cutsLocation);
    }

    return Files.walk(cutsLocation, 1)
            .filter(Files::isDirectory)  // Somente diretórios
            .filter(path -> !path.equals(cutsLocation))  // Não incluir o diretório raiz
            .map(path -> cutsLocation.relativize(path).toString())  // Nome relativo da pasta
            .collect(Collectors.toList());
}
}