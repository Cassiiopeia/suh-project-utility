package me.suhsaechan.study.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.security.bc.BCSecurityProvider;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Set;

/**
 * 파일 저장 방식에 따른 인터페이스 정의 및 구현체
 * 기본적으로 SMB를 통해 시놀로지 NAS에 저장
 */
@Slf4j
@Component
public class FileStorageProvider {

    @Value("${file.domain}")
    private String fileDomain;

    @Value("${file.dir}")
    private String fileDir;

    @Value("${file.host}")
    private String fileHost;

    @Value("${file.smb.port:44445}")
    private int smbPort;

    @Value("${file.username}")
    private String fileUsername;

    @Value("${file.password}")
    private String filePassword;

    @Value("${file.smb.workgroup:WORKGROUP}")
    private String smbWorkgroup;
    
    @Value("${file.ftp.port:21}")
    private int ftpPort;
    
    @Value("${file.root-dir:web}")
    private String fileRootDir;

    /**
     * 파일 저장 방식 선택
     */
    public enum StorageType {
        LOCAL,      // 로컬 파일 시스템
        SMB,        // Samba (SMB) 네트워크 공유
        FTP         // FTP 서버
    }

    /**
     * 파일 저장 구현 - 기본적으로 SMB 사용
     * @param file 저장할 파일
     * @param storedFilename 저장될 파일명
     * @throws IOException 파일 저장 실패 시
     */
    public void saveFile(MultipartFile file, String storedFilename) throws IOException {
        saveFile(file, storedFilename, StorageType.SMB);
    }

    /**
     * 파일 저장 구현
     * @param file 저장할 파일
     * @param storedFilename 저장될 파일명
     * @param storageType 저장 방식
     * @throws IOException 파일 저장 실패 시
     */
    public void saveFile(MultipartFile file, String storedFilename, StorageType storageType) throws IOException {
        switch (storageType) {
            case LOCAL:
                saveToLocalSystem(file, storedFilename);
                break;
            case SMB:
                try {
                    saveToSmbServer(file, storedFilename);
                } catch (Exception e) {
                    log.error("SMB 저장 실패: {}", e.getMessage(), e);
                    throw new IOException("SMB 저장 실패", e);
                }
                break;
            case FTP:
                try {
                    saveToFtpServer(file, storedFilename);
                } catch (Exception e) {
                    log.error("FTP 저장 실패: {}", e.getMessage(), e);
                    throw new IOException("FTP 저장 실패", e);
                }
                break;
            default:
                saveToSmbServer(file, storedFilename);
        }
    }
    
    /**
     * 로컬 파일 시스템에 파일 저장 (테스트용)
     */
    private void saveToLocalSystem(MultipartFile file, String storedFilename) throws IOException {
        // 로컬 경로 설정
        String uploadDir = System.getProperty("user.dir") + "/uploads";
        Path uploadPath = Paths.get(uploadDir);
        
        // 디렉토리 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 파일 저장
        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("로컬 시스템에 파일 저장: {}", filePath);
    }
    
    /**
     * SMB 서버에 파일 저장
     */
    private void saveToSmbServer(MultipartFile file, String storedFilename) throws IOException {
        // SMB 클라이언트 설정
        SmbConfig config = SmbConfig.builder()
                .withSecurityProvider(new BCSecurityProvider())
                .build();
        
        SMBClient client = new SMBClient(config);
        
        try (Connection connection = client.connect(fileHost, smbPort)) {
            AuthenticationContext authContext = new AuthenticationContext(
                    fileUsername, 
                    filePassword.toCharArray(), 
                    smbWorkgroup);
            
            Session session = connection.authenticate(authContext);
            
            // SMB 공유 접근
            try (DiskShare share = (DiskShare) session.connectShare(fileRootDir)) {
                // 업로드 디렉토리 경로 설정
                String uploadPath = fileDir;
                
                // 디렉토리 존재 확인 및 생성
                String[] pathParts = uploadPath.split("/");
                StringBuilder currentPath = new StringBuilder();
                
                for (String part : pathParts) {
                    if (!part.isEmpty()) {
                        if (currentPath.length() > 0) {
                            currentPath.append("/");
                        }
                        currentPath.append(part);
                        
                        if (!share.folderExists(currentPath.toString())) {
                            try {
                                share.mkdir(currentPath.toString());
                                log.info("SMB 디렉토리 생성: {}", currentPath);
                            } catch (Exception e) {
                                log.warn("SMB 디렉토리 생성 실패: {}", currentPath, e);
                            }
                        }
                    }
                }
                
                // 파일 저장
                String fullPath = uploadPath + "/" + storedFilename;
                
                // 파일에 쓰기 권한으로 열기
                Set<SMB2ShareAccess> shareAccess = EnumSet.of(
                        SMB2ShareAccess.FILE_SHARE_READ,
                        SMB2ShareAccess.FILE_SHARE_WRITE,
                        SMB2ShareAccess.FILE_SHARE_DELETE
                );
                
                Set<AccessMask> accessMask = EnumSet.of(
                        AccessMask.GENERIC_WRITE,
                        AccessMask.GENERIC_READ
                );
                
                // 파일 내용을 바이트 배열로 변환
                byte[] fileContent = IOUtils.toByteArray(file.getInputStream());
                
                try (com.hierynomus.smbj.share.File smbFile = share.openFile(
                        fullPath,
                        accessMask,
                        null,
                        shareAccess,
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        null)) {
                    
                    // 바이트 배열을 직접 쓰기
                    smbFile.write(fileContent, 0);
                    log.info("SMB 서버에 파일 저장: {}", fullPath);
                }
            }
        } catch (Exception e) {
            log.error("SMB 파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("SMB 파일 저장 실패", e);
        }
    }
    
    /**
     * FTP 서버에 파일 저장
     */
    private void saveToFtpServer(MultipartFile file, String storedFilename) throws IOException {
        FTPClient ftpClient = new FTPClient();
        
        try {
            // FTP 서버 연결
            ftpClient.connect(fileHost, ftpPort);
            boolean login = ftpClient.login(fileUsername, filePassword);
            
            if (!login) {
                throw new IOException("FTP 로그인 실패");
            }
            
            // 바이너리 모드 설정
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            
            // 업로드 디렉토리 설정
            String uploadPath = fileRootDir + "/" + fileDir;
            
            // 디렉토리 존재 확인 및 생성
            String[] pathParts = uploadPath.split("/");
            StringBuilder currentPath = new StringBuilder();
            
            for (String part : pathParts) {
                if (!part.isEmpty()) {
                    currentPath.append("/").append(part);
                    
                    boolean dirExists = ftpClient.changeWorkingDirectory(currentPath.toString());
                    if (!dirExists) {
                        boolean created = ftpClient.makeDirectory(currentPath.toString());
                        if (created) {
                            log.info("FTP 디렉토리 생성: {}", currentPath);
                        } else {
                            log.warn("FTP 디렉토리 생성 실패: {}", currentPath);
                        }
                    }
                }
            }
            
            // 파일 업로드 디렉토리로 이동
            ftpClient.changeWorkingDirectory(uploadPath);
            
            // 파일 저장
            boolean stored = ftpClient.storeFile(storedFilename, file.getInputStream());
            
            if (stored) {
                log.info("FTP 서버에 파일 저장: {}/{}", uploadPath, storedFilename);
            } else {
                log.error("FTP 파일 저장 실패: {}", ftpClient.getReplyString());
                throw new IOException("FTP 파일 저장 실패: " + ftpClient.getReplyString());
            }
            
            // 로그아웃
            ftpClient.logout();
            
        } catch (Exception e) {
            log.error("FTP 파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new IOException("FTP 파일 저장 실패", e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    log.error("FTP 연결 종료 실패", e);
                }
            }
        }
    }
    
    /**
     * 전체 파일 URL 생성
     * @param storedFilename 저장된 파일명
     * @return 완성된 URL
     */
    public String getFullFileUrl(String storedFilename) {
        // 시놀로지 접근용 URL
        return fileDomain + "/" + fileDir + "/" + storedFilename;
    }
    
    /**
     * 파일 삭제 - 기본적으로 SMB 사용
     * @param storedFilename 저장된 파일명
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String storedFilename) {
        return deleteFile(storedFilename, StorageType.SMB);
    }
    
    /**
     * 파일 삭제
     * @param storedFilename 저장된 파일명
     * @param storageType 저장 방식
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String storedFilename, StorageType storageType) {
        switch (storageType) {
            case LOCAL:
                return deleteFromLocalSystem(storedFilename);
            case SMB:
                try {
                    return deleteFromSmbServer(storedFilename);
                } catch (Exception e) {
                    log.error("SMB 삭제 실패: {}", e.getMessage(), e);
                    return false;
                }
            case FTP:
                try {
                    return deleteFromFtpServer(storedFilename);
                } catch (Exception e) {
                    log.error("FTP 삭제 실패: {}", e.getMessage(), e);
                    return false;
                }
            default:
                try {
                    return deleteFromSmbServer(storedFilename);
                } catch (Exception e) {
                    log.error("기본 SMB 삭제 실패: {}", e.getMessage(), e);
                    return false;
                }
        }
    }
    
    /**
     * 로컬 파일 시스템에서 파일 삭제 (테스트용)
     */
    private boolean deleteFromLocalSystem(String storedFilename) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            Path filePath = Paths.get(uploadDir, storedFilename);
            
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("로컬 시스템에서 파일 삭제 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * SMB 서버에서 파일 삭제
     */
    private boolean deleteFromSmbServer(String storedFilename) {
        SmbConfig config = SmbConfig.builder()
                .withSecurityProvider(new BCSecurityProvider())
                .build();
        
        SMBClient client = new SMBClient(config);
        
        try (Connection connection = client.connect(fileHost, smbPort)) {
            AuthenticationContext authContext = new AuthenticationContext(
                    fileUsername, 
                    filePassword.toCharArray(), 
                    smbWorkgroup);
            
            Session session = connection.authenticate(authContext);
            
            try (DiskShare share = (DiskShare) session.connectShare(fileRootDir)) {
                String filePath = fileDir + "/" + storedFilename;
                
                if (share.fileExists(filePath)) {
                    share.rm(filePath);
                    log.info("SMB 서버에서 파일 삭제: {}", filePath);
                    return true;
                } else {
                    log.warn("SMB 서버에서 파일이 존재하지 않음: {}", filePath);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("SMB 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * FTP 서버에서 파일 삭제
     */
    private boolean deleteFromFtpServer(String storedFilename) {
        FTPClient ftpClient = new FTPClient();
        
        try {
            // FTP 서버 연결
            ftpClient.connect(fileHost, ftpPort);
            boolean login = ftpClient.login(fileUsername, filePassword);
            
            if (!login) {
                throw new IOException("FTP 로그인 실패");
            }
            
            // 바이너리 모드 설정
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            
            // 파일 경로 설정
            String filePath = fileRootDir + "/" + fileDir + "/" + storedFilename;
            
            // 파일 삭제
            boolean deleted = ftpClient.deleteFile(filePath);
            
            if (deleted) {
                log.info("FTP 서버에서 파일 삭제: {}", filePath);
            } else {
                log.warn("FTP 서버에서 파일 삭제 실패: {}", ftpClient.getReplyString());
            }
            
            // 로그아웃
            ftpClient.logout();
            
            return deleted;
        } catch (Exception e) {
            log.error("FTP 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
            return false;
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    log.error("FTP 연결 종료 실패", e);
                }
            }
        }
    }
}