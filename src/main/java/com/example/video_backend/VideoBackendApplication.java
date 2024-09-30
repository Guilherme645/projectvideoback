package com.example.video_backend;

import com.example.video.model.VideoFile;
import com.example.video.repository.VideoFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VideoService {

	private final String UPLOAD_DIR = "C:/videos";
	private final String FFMPEG_PATH = "C:/ffmpeg/ffmpeg.exe";  // Localização do FFmpeg
	private final String OUTPUT_DIR = "C:/cortes_videos";       // Diretório de saída

	@Autowired
	private VideoFileRepository videoFileRepository;

	// Função para calcular a duração do arquivo de vídeo
	public long getVideoDuration(String filePath) throws IOException {
		String[] command = {FFMPEG_PATH, "-i", filePath};

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("Duration")) {
					String[] parts = line.split(",")[0].split(":");
					String[] timeParts = parts[1].trim().split(":");
					long hours = Long.parseLong(timeParts[0].trim());
					long minutes = Long.parseLong(timeParts[1].trim());
					float seconds = Float.parseFloat(timeParts[2].trim());

					long totalSeconds = (hours * 3600) + (minutes * 60) + (long) seconds;
					return totalSeconds;
				}
			}
		}

		try {
			process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("O processo foi interrompido", e);
		}
		return 0;
	}

	// Salva o arquivo de vídeo no servidor e no banco de dados
	public String saveVideoFile(MultipartFile file) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		String filePath = UPLOAD_DIR + "/" + file.getOriginalFilename();
		File dest = new File(filePath);
		file.transferTo(dest);

		VideoFile videoFile = new VideoFile();
		videoFile.setFileName(file.getOriginalFilename());
		videoFile.setFilePath(filePath);
		videoFile.setSize(file.getSize());

		long duration = getVideoDuration(filePath);
		videoFile.setDuration(duration);

		videoFile.setUploadTime(LocalDateTime.now());
		videoFileRepository.save(videoFile);

		return file.getOriginalFilename();
	}

	// Lista todos os arquivos de vídeo em um diretório específico
	public List<String> listVideoFilesFromDirectory(String directory) throws IOException {
		Path dirPath = Paths.get(directory);
		if (!Files.exists(dirPath)) {
			throw new IOException("Diretório não encontrado: " + directory);
		}
		return Files.walk(dirPath)
				.filter(Files::isRegularFile)
				.map(path -> path.getFileName().toString())
				.collect(Collectors.toList());
	}

	// Função para cortar um arquivo de vídeo e atualizar o progresso
	public String cutVideoFile(String videoName, double start, double duration) throws IOException {
		String inputFilePath = Paths.get(UPLOAD_DIR, videoName).toString();

		// Obter a data atual no formato yyyy-MM-dd
		String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		// Criar o diretório com a data dentro de OUTPUT_DIR (se ainda não existir)
		Path dateDirectory = Paths.get(OUTPUT_DIR, currentDate);
		if (!Files.exists(dateDirectory)) {
			Files.createDirectories(dateDirectory); // Cria o diretório
		}

		// Construa o caminho completo do arquivo de saída dentro do diretório de data
		String outputFilePath = Paths.get(dateDirectory.toString(), "corte_" + videoName).toString();

		// Comando FFmpeg para corte
		String[] command = {
				FFMPEG_PATH,
				"-ss", String.valueOf(start),
				"-i", inputFilePath,
				"-t", String.valueOf(duration),
				"-c", "copy",  // Cópia sem reprocessamento
				outputFilePath
		};

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);  // Log para acompanhamento
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IOException("Erro no processo FFmpeg, código de saída: " + exitCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Processo de corte interrompido", e);
		}

		return outputFilePath;
	}
}
