package com.ethereal.fileservice.service;

import com.ethereal.fileservice.entity.ImageEntity;
import com.ethereal.fileservice.exceptions.FtpConnectionException;
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
public class FtpService {
    @Value("${ftp.server}")
    private String FTP_SERVER;
    @Value("${ftp.username}")
    private String FTP_USERNAME;
    @Value("${ftp.password}")
    private String FTP_PASSWORD;
    @Value("${ftp.origin}")
    private String FTP_ORIGIN_DIRECTORY;
    @Value("${ftp.port}")
    private int FTP_PORT;


    private FTPClient getFtpConnection() throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(FTP_SERVER, FTP_PORT);
        ftpClient.login(FTP_USERNAME, FTP_PASSWORD);

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        return ftpClient;
    }

    public ImageEntity uploadFileToFtp(MultipartFile file) throws FtpConnectionException, IOException {
        try {
            FTPClient ftpClient = getFtpConnection();
            String remoteFilePath = FTP_ORIGIN_DIRECTORY + "/" + LocalDate.now() + "/" + file.getOriginalFilename();
            boolean uploaded = streamFile(file, ftpClient, remoteFilePath);
            if (!uploaded) {
                createFolder(ftpClient);
                if (!streamFile(file, ftpClient, remoteFilePath)) {
                    throw new FtpConnectionException("Cannot connect to server");
                }
            }
            ftpClient.logout();
            ftpClient.disconnect();
            return ImageEntity.builder()
                    .path(remoteFilePath)
                    .uuid(UUID.randomUUID().toString())
                    .createAt(LocalDate.now())
                    .isUsed(false).build();
        } catch (IOException e) {
            throw new FtpConnectionException(e);
        }
    }

    private void createFolder(FTPClient client) throws IOException {
        client.makeDirectory(FTP_ORIGIN_DIRECTORY + "/" + LocalDate.now());
    }

    private boolean streamFile(MultipartFile file, FTPClient ftpClient, String remoteFilePath) throws IOException {
        InputStream inputStream = file.getInputStream();
        System.out.println("Trying to upload to: " + remoteFilePath);
        System.out.println("FTP Client reply: " + ftpClient.getReplyString());
        //ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        boolean uploaded = ftpClient.storeFile(remoteFilePath, inputStream);
        inputStream.close();
        return uploaded;
    }

    public boolean deleteFile(String path) throws IOException {
        FTPClient ftpClient = getFtpConnection();
        boolean deleted = ftpClient.deleteFile(path);
        ftpClient.logout();
        ftpClient.disconnect();
        return deleted;
    }

    public ByteArrayOutputStream getFile(ImageEntity imageEntity) throws IOException {
        FTPClient ftpClient = getFtpConnection();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean downloaded = ftpClient.retrieveFile(imageEntity.getPath(), outputStream);
        ftpClient.logout();
        ftpClient.disconnect();
        if (downloaded)
            return outputStream;
        throw new FtpConnectionException("Cannot download file");
    }
}
