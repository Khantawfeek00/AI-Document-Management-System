package it.polito.wa2.fileservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import it.polito.wa2.fileservice.services.FileShareService;
import it.polito.wa2.fileservice.repository.FileShareRepository;
import it.polito.wa2.fileservice.repository.FileRepository;
import it.polito.wa2.fileservice.clients.UserServiceClient;
import it.polito.wa2.fileservice.entities.SharePermission;
import it.polito.wa2.fileservice.exception.FileShareException;
import it.polito.wa2.fileservice.exception.UploadNotFoundException;

import java.util.Optional;

public class FileShareServiceUnitTests {
    @Test
    public void createShareThrowsIfUserDoesNotExist() {
        FileShareRepository fileShareRepository = Mockito.mock(FileShareRepository.class);
        FileRepository fileRepository = Mockito.mock(FileRepository.class);
        UserServiceClient userServiceClient = Mockito.mock(UserServiceClient.class);
        FileShareService service = new FileShareService(fileShareRepository, fileRepository, userServiceClient);
        
        Mockito.when(fileRepository.findById("fileId"))
               .thenReturn(Optional.of(Mockito.mock(it.polito.wa2.fileservice.entities.File.class)));
        Mockito.when(userServiceClient.userExists("userId")).thenReturn(false);
        
        Assertions.assertThrows(FileShareException.class, () -> {
            service.createShare("fileId", "userId", SharePermission.READ);
        });
    }

    @Test
    public void createShareThrowsIfAlreadyShared() {
        FileShareRepository fileShareRepository = Mockito.mock(FileShareRepository.class);
        FileRepository fileRepository = Mockito.mock(FileRepository.class);
        UserServiceClient userServiceClient = Mockito.mock(UserServiceClient.class);
        FileShareService service = new FileShareService(fileShareRepository, fileRepository, userServiceClient);

        try (MockedStatic<it.polito.wa2.fileservice.utils.SecurityUtils> mockedSecurityUtils = Mockito.mockStatic(it.polito.wa2.fileservice.utils.SecurityUtils.class)) {
            mockedSecurityUtils.when(it.polito.wa2.fileservice.utils.SecurityUtils::getAuthenticatedUserId).thenReturn("callerId");
            mockedSecurityUtils.when(it.polito.wa2.fileservice.utils.SecurityUtils::isAdmin).thenReturn(false);

            Mockito.when(fileRepository.findById("fileId"))
                   .thenReturn(Optional.of(Mockito.mock(it.polito.wa2.fileservice.entities.File.class)));
            Mockito.when(userServiceClient.userExists("userId")).thenReturn(true);
            Mockito.when(fileShareRepository.existsByFile_IdAndSharedWithUserId("fileId", "userId")).thenReturn(true);

            Assertions.assertThrows(FileShareException.class, () -> {
                service.createShare("fileId", "userId", SharePermission.READ);
            });
        }
    }

    @Test
    public void getFileSharesThrowsIfFileDoesNotExist() {
        FileShareRepository fileShareRepository = Mockito.mock(FileShareRepository.class);
        FileRepository fileRepository = Mockito.mock(FileRepository.class);
        UserServiceClient userServiceClient = Mockito.mock(UserServiceClient.class);
        FileShareService service = new FileShareService(fileShareRepository, fileRepository, userServiceClient);
        
        Mockito.when(fileRepository.existsById("fileId")).thenReturn(false);
        
        Assertions.assertThrows(UploadNotFoundException.class, () -> {
            service.getFileShares("fileId");
        });
    }
}
