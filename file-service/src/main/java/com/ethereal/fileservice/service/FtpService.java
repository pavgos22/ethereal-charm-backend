package com.ethereal.fileservice.service;

import com.ethereal.fileservice.entity.ImageEntity;
import com.ethereal.fileservice.exceptions.FtpConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class FtpService {

    @Value("${ftp.server}")     private String FTP_SERVER;
    @Value("${ftp.port}")       private int    FTP_PORT;
    @Value("${ftp.username}")   private String FTP_USERNAME;
    @Value("${ftp.password}")   private String FTP_PASSWORD;
    @Value("${ftp.origin}")     private String FTP_ORIGIN_DIRECTORY;


    public ImageEntity uploadFileToFtp(MultipartFile file)
            throws FtpConnectionException, IOException {

        FTPClient ftp = null;

        try {
            ftp = openConnection();

            String dateFolder   = LocalDate.now().toString();
            String remoteDir    = FTP_ORIGIN_DIRECTORY + "/" + dateFolder;
            ensureDirExists(ftp, remoteDir);

            String remotePath = remoteDir + "/" + file.getOriginalFilename();
            try (InputStream in = file.getInputStream()) {
                log.info("Uploading to {}", remotePath);
                if (!ftp.storeFile(remotePath, in)) {
                    throw new FtpConnectionException("FTP storeFile() returned false: " + ftp.getReplyString());
                }
            }

            return ImageEntity.builder()
                    .uuid(UUID.randomUUID().toString())
                    .path(remotePath)
                    .createAt(LocalDate.now())
                    .isUsed(false)
                    .build();

        } catch (IOException ex) {
            throw new FtpConnectionException(ex);
        } finally {
            closeQuietly(ftp);
        }
    }

    public boolean deleteFile(String path) throws IOException {
        FTPClient ftp = null;
        try {
            ftp = openConnection();
            return ftp.deleteFile(path);
        } finally {
            closeQuietly(ftp);
        }
    }

    public ByteArrayOutputStream getFile(ImageEntity img) throws IOException {
        FTPClient ftp = null;
        try {
            ftp = openConnection();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (ftp.retrieveFile(img.getPath(), out)) {
                return out;
            }
            throw new FtpConnectionException("Cannot download file: " + ftp.getReplyString());
        } finally {
            closeQuietly(ftp);
        }
    }


    private FTPClient openConnection() throws IOException {
        FTPClient ftp = new FTPClient();
        ftp.connect(FTP_SERVER, FTP_PORT);
        if (!ftp.login(FTP_USERNAME, FTP_PASSWORD)) {
            throw new FtpConnectionException("FTP login failed: " + ftp.getReplyString());
        }
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        return ftp;
    }

    private void closeQuietly(FTPClient ftp) {
        if (ftp != null && ftp.isConnected()) {
            try { ftp.logout(); } catch (IOException ignored) {}
            try { ftp.disconnect(); } catch (IOException ignored) {}
        }
    }

    private void ensureDirExists(FTPClient ftp, String remotePath) throws IOException {
        String[] parts = remotePath.split("/");
        String path = "";
        for (String part : parts) {
            if (part.isBlank()) continue;
            path += "/" + part;
            if (!ftp.changeWorkingDirectory(path)) {
                log.debug("Creating missing dir {}", path);
                if (!ftp.makeDirectory(path)) {
                    throw new FtpConnectionException("Cannot create remote dir " + path + ": "
                            + ftp.getReplyString());
                }
                ftp.changeWorkingDirectory(path);
            }
        }
        ftp.changeWorkingDirectory("/");
    }
}