package com.example.video_backend.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final String UPLOAD_DIR = "C:/pastavideos"; // Altere para o seu caminho de diretório
    private final Path rootLocation = Paths.get(UPLOAD_DIR);
    private final Path cutsLocation = Paths.get("C:/cortesvideos");
    private final String FFMPEG_PATH = "C:/ffmpeg/ffmpeg-7.0.2-full_build/bin/ffmpeg.exe";

    // Método para carregar o vídeo como recurso
    public Resource loadVideoAsResource(String fileName) throws IOException {
        Path filePath = rootLocation.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        // Verifica se o arquivo existe e é legível
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Arquivo de vídeo não encontrado ou não legível: " + fileName);
        }
    }

    // Lista todas as subpastas no diretório de vídeos
    public List<String> listAllFolders() throws IOException {
        if (!Files.exists(rootLocation) || !Files.isDirectory(rootLocation)) {
            throw new IOException("Diretório de vídeos não encontrado: " + rootLocation);
        }

        // Caminha por todas as subpastas no diretório raiz
        return Files.walk(rootLocation, 1)
                .filter(Files::isDirectory)  // Somente diretórios
                .filter(path -> !path.equals(rootLocation))  // Não incluir o diretório raiz
                .map(path -> rootLocation.relativize(path).toString())  // Nome relativo da pasta
                .collect(Collectors.toList());
    }

    // Lista todos os vídeos dentro de uma pasta específica
    public List<String> listVideosFromFolder(String folderName) throws IOException {
        Path folderPath = rootLocation.resolve(folderName);
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IOException("Pasta não encontrada: " + folderName);
        }

        return Files.walk(folderPath, 1)
                .filter(Files::isRegularFile)  // Somente arquivos
                .map(path -> folderPath.relativize(path).toString())  // Nome relativo do arquivo
                .filter(this::isVideoFile)  // Filtrar somente arquivos de vídeo
                .collect(Collectors.toList());
    }

    // Verifica se o arquivo é um vídeo com as extensões permitidas
    private boolean isVideoFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".mp4") ||
                lowerCaseFileName.endsWith(".avi") ||
                lowerCaseFileName.endsWith(".mkv");  // Adicione mais extensões conforme necessário
    }

    // Corta um arquivo de vídeo e o salva na pasta C:/cortesvideos
    public String cutVideoFileFromDirectory(String folderName, String fileName, double startSeconds, double durationSeconds) throws IOException {
        // Combine o caminho da pasta com o nome do arquivo
        Path folderPath = rootLocation.resolve(folderName);
        Path inputFile = folderPath.resolve(fileName);

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

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Erro ao cortar o vídeo, código de saída do FFmpeg: " + exitCode);
            }
        } catch (InterruptedException e) {
            // Interromper a thread atual e registrar o erro
            Thread.currentThread().interrupt();  // Restaura o estado de interrupção da thread
            System.out.println("Processo foi interrompido: " + e.getMessage());
            throw new IOException("O processo de corte do vídeo foi interrompido", e);
        }

        return outputFileName;
    }

    public Resource loadVideoFromFolderAsResource(String folderName, String fileName) throws IOException {
        // Monta o caminho para a pasta do vídeo
        Path folderPath = Paths.get(UPLOAD_DIR).resolve(folderName).normalize();
        Path filePath = folderPath.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        // Verifica se o recurso existe e é legível
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Arquivo de vídeo não encontrado: " + filePath);
        }
    }
}
