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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final String UPLOAD_DIR = "C:/pastavideos"; // Altere para o seu caminho de diretório
    private final Path rootLocation = Paths.get(UPLOAD_DIR);
    private final Path cutsLocation = Paths.get("C:/cortesvideos");
    private final String FFMPEG_PATH = "C:/ffmpeg/ffmpeg-7.0.2-full_build/bin/ffmpeg.exe";
    private ConcurrentMap<String, Double> progressMap = new ConcurrentHashMap<>();

    // Método para carregar o vídeo como recurso
    public Resource loadVideoFromFolderAsResource(String folderName, String fileName) throws IOException {
        // Monta o caminho para a pasta do vídeo
        Path folderPath = Paths.get(UPLOAD_DIR).resolve(folderName).normalize();
        Path filePath = folderPath.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        // Verifica se o recurso existe e é legível
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

    // Método para cortar um vídeo e salvar na pasta C:/cortesvideos
    public String cutVideoFileFromDirectory(String folderName, String fileName, double startSeconds, double durationSeconds) throws IOException {
        String videoId = folderName + "_" + fileName; // Identificador único
        Path inputFile = Paths.get(UPLOAD_DIR, folderName, fileName);

        // Verifica se o arquivo de entrada existe
        if (!Files.exists(inputFile)) {
            throw new IOException("Vídeo não encontrado: " + fileName);
        }

        // Cria o nome da subpasta com a data atual no formato yyyy-MM-dd
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path dateSubFolder = Paths.get(cutsLocation.toString(), currentDate);
        if (!Files.exists(dateSubFolder)) {
            Files.createDirectories(dateSubFolder); // Cria a subpasta de cortes do dia
        }

        // Gera o nome do arquivo cortado com um timestamp para evitar conflitos de nome
        String outputFileName = "cut_" + System.currentTimeMillis() + "_" + fileName;
        Path outputFilePath = dateSubFolder.resolve(outputFileName).normalize().toAbsolutePath();

        // Comando do FFmpeg para cortar o vídeo
        String[] command = {
                FFMPEG_PATH,
                "-ss", String.valueOf(startSeconds),   // Início do corte
                "-i", inputFile.toString(),           // Arquivo de entrada
                "-t", String.valueOf(durationSeconds), // Duração do corte
                "-c", "copy",                         // Mantém o codec original (sem reprocessamento)
                outputFilePath.toString()             // Arquivo de saída
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
                // Simulação de progresso (pode melhorar se FFmpeg retornar frames processados)
                double progress = (double) frameCount / 100; // Progresso fictício de 0 a 100%
                progressMap.put(videoId, Math.min(progress, 1.0)); // Atualiza o progresso, não passa de 100%
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

        // Verifica se o FFmpeg teve sucesso
        if (exitCode != 0) {
            throw new IOException("Erro ao cortar o vídeo, código de saída do FFmpeg: " + exitCode);
        }

        // Após finalizar, define progresso como 100%
        progressMap.put(videoId, 1.0);

        return outputFilePath.toString(); // Retorna o caminho completo do arquivo cortado
    }

    // Método para obter o progresso do corte
    public Double getVideoCutProgress(String folderName, String fileName) {
        String videoId = folderName + "_" + fileName;
        return progressMap.getOrDefault(videoId, 0.0);
    }

    public List<String> listAllCutFolders() throws IOException {
        Path cutsRootLocation = Paths.get("C:/cortesvideos");
        if (!Files.exists(cutsRootLocation) || !Files.isDirectory(cutsRootLocation)) {
            throw new IOException("Diretório de cortes não encontrado: " + cutsRootLocation);
        }

        // Caminha por todas as subpastas no diretório de cortes
        return Files.walk(cutsRootLocation, 1)
                .filter(Files::isDirectory)  // Somente diretórios
                .filter(path -> !path.equals(cutsRootLocation))  // Não incluir o diretório raiz
                .map(path -> cutsRootLocation.relativize(path).toString())  // Nome relativo da pasta
                .collect(Collectors.toList());
    }
    public List<String> listVideosFromCutFolder(String folderName) throws IOException {
        Path cutsFolderPath = Paths.get("C:/cortesvideos").resolve(folderName);
        if (!Files.exists(cutsFolderPath) || !Files.isDirectory(cutsFolderPath)) {
            throw new IOException("Pasta de cortes não encontrada: " + folderName);
        }

        return Files.walk(cutsFolderPath, 1)
                .filter(Files::isRegularFile)  // Somente arquivos
                .map(path -> cutsFolderPath.relativize(path).toString())  // Nome relativo do arquivo
                .filter(this::isVideoFile)  // Filtrar somente arquivos de vídeo
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
