package com.ethereal.fileservice.service;

import com.ethereal.fileservice.entity.ImageEntity;
import com.ethereal.fileservice.exceptions.FtpConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class FtpService {

    @Value("${ftp.server}")
    private String FTP_SERVER;
    @Value("${ftp.port}")
    private int    FTP_PORT;
    @Value("${ftp.username}")
    private String FTP_USERNAME;
    @Value("${ftp.password}")
    private String FTP_PASSWORD;
    @Value("${ftp.origin}")
    private String FTP_ORIGIN_DIRECTORY;
    @Value("${ftp.pool.max-total}")
    private int    POOL_MAX_TOTAL;
    @Value("${ftp.pool.max-wait-millis}")
    private long POOL_MAX_WAIT;

    private GenericObjectPool<FTPClient> pool;

    @PostConstruct
    void initPool() {

        GenericObjectPoolConfig<FTPClient> cfg = new GenericObjectPoolConfig<>();

        cfg.setMaxTotal(POOL_MAX_TOTAL);
        cfg.setMaxIdle(POOL_MAX_TOTAL);
        cfg.setMinIdle(1);

        cfg.setBlockWhenExhausted(true);
        cfg.setMaxWait(java.time.Duration.ofMillis(POOL_MAX_WAIT));

        cfg.setTestOnBorrow(true);
        cfg.setTestWhileIdle(false);

        pool = new GenericObjectPool<>(new FtpClientFactory(), cfg);

        log.info("FTP pool initialised (maxTotal={}, maxWait={} ms)",
                POOL_MAX_TOTAL, POOL_MAX_WAIT);
    }

    @PreDestroy
    void closePool() {
        if (pool != null) {
            pool.close();
        }
    }

    public ImageEntity uploadFileToFtp(MultipartFile file)
            throws IOException {

        FTPClient ftp = borrow();
        try {
            String dateFolder = LocalDate.now().toString();
            String remoteDir  = FTP_ORIGIN_DIRECTORY + "/" + dateFolder;
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
                    .isUsed(true)
                    .build();

        } finally {
            returnClient(ftp);
        }
    }

    public boolean deleteFile(String path) throws IOException {
        FTPClient ftp = borrow();
        try {
            return ftp.deleteFile(path);
        } finally {
            returnClient(ftp);
        }
    }

    public ByteArrayOutputStream getFile(ImageEntity img) throws IOException {
        FTPClient ftp = borrow();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (ftp.retrieveFile(img.getPath(), out)) {
                return out;
            }
            throw new FtpConnectionException("Cannot download file: " + ftp.getReplyString());
        } finally {
            returnClient(ftp);
        }
    }

    private FTPClient borrow() throws IOException {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new FtpConnectionException("Cannot borrow FTP client from pool", e);
        }
    }

    private void returnClient(FTPClient client) {
        if (client != null) {
            pool.returnObject(client);
        }
    }

    private class FtpClientFactory implements PooledObjectFactory<FTPClient> {

        @Override
        public PooledObject<FTPClient> makeObject() throws Exception {
            FTPClient ftp = new FTPClient();
            ftp.connect(FTP_SERVER, FTP_PORT);
            if (!ftp.login(FTP_USERNAME, FTP_PASSWORD)) {
                throw new FtpConnectionException("FTP login failed: " + ftp.getReplyString());
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            return new DefaultPooledObject<>(ftp);
        }

        @Override
        public void destroyObject(PooledObject<FTPClient> p) {
            FTPClient ftp = p.getObject();
            if (ftp.isConnected()) {
                try { ftp.logout(); } catch (IOException ignored) {}
                try { ftp.disconnect(); } catch (IOException ignored) {}
            }
        }

        @Override
        public boolean validateObject(PooledObject<FTPClient> p) {
            FTPClient ftp = p.getObject();
            try {
                return ftp.isConnected() && ftp.sendNoOp();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void activateObject(PooledObject<FTPClient> p) { /* noop */ }

        @Override
        public void passivateObject(PooledObject<FTPClient> p) { /* noop */ }
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