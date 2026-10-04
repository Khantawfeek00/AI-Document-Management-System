package it.polito.wa2.fileservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import it.polito.wa2.fileservice.controllers.FileController;
import it.polito.wa2.fileservice.dtos.FileDetailDTO;
import it.polito.wa2.fileservice.dtos.FileSummaryDTO;
import it.polito.wa2.fileservice.dtos.UpdateFileRequest;
import it.polito.wa2.fileservice.services.FileService;
import it.polito.wa2.fileservice.services.UploadService;
import it.polito.wa2.fileservice.services.FileShareService;

import java.time.Instant;
import java.util.List;

public class FileControllerUnitTests {

    @Test
    public void listFilesReturnsPaginatedResponseWithFiles() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        PageRequest pageable = PageRequest.of(0, 2);
        Instant now = Instant.now();
        FileSummaryDTO dto1 = new FileSummaryDTO("id1", "file1.png", "image/png", 100L, now, now, "COMPLETED", "owner1");
        FileSummaryDTO dto2 = new FileSummaryDTO("id2", "file2.png", "image/png", 200L, now, now, "COMPLETED", "owner2");
        PageImpl<FileSummaryDTO> page = new PageImpl<>(List.of(dto1, dto2), pageable, 2);
        
        Mockito.when(fileService.getFiles(null, null, null, pageable)).thenReturn(page);
        
        var response = controller.listFiles(null, null, null, pageable);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(2, response.getBody().getContent().size());
        
        // Use record accessors or getters
        String filename1 = "";
        String filename2 = "";
        try {
            filename1 = (String) response.getBody().getContent().get(0).getClass().getMethod("filename").invoke(response.getBody().getContent().get(0));
            filename2 = (String) response.getBody().getContent().get(1).getClass().getMethod("filename").invoke(response.getBody().getContent().get(1));
        } catch (Exception e) {
            try {
                filename1 = (String) response.getBody().getContent().get(0).getClass().getMethod("getFilename").invoke(response.getBody().getContent().get(0));
                filename2 = (String) response.getBody().getContent().get(1).getClass().getMethod("getFilename").invoke(response.getBody().getContent().get(1));
            } catch (Exception ex) { }
        }
        Assertions.assertEquals("file1.png", filename1);
        Assertions.assertEquals("file2.png", filename2);
    }

    @Test
    public void getFileReturnsFileDetailsIfExists() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        FileDetailDTO detailDTO = Mockito.mock(FileDetailDTO.class);
        Mockito.when(fileService.getFileById("fileId")).thenReturn(detailDTO);
        
        var response = controller.getFile("fileId");
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(detailDTO, response.getBody());
    }

    @Test
    public void updateFileReturnsUpdatedFileDetails() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        FileDetailDTO detailDTO = Mockito.mock(FileDetailDTO.class);
        
        // Assuming UpdateFileRequest has a constructor with filename or builder.
        // Let's instantiate it empty and set it if possible, or use constructor.
        // Since it's a record or DTO translated from data class `UpdateFileRequest(filename = "updated.png")`,
        // it probably has a 1-arg constructor.
        UpdateFileRequest request = new UpdateFileRequest("updated.png");
        
        Mockito.when(fileService.updateFileMetadata("fileId", request)).thenReturn(detailDTO);
        
        var response = controller.updateFile("fileId", request);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(detailDTO, response.getBody());
    }

    @Test
    public void deleteFileReturnsNoContent() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        
        var response = controller.deleteFile("fileId");
        Assertions.assertEquals(204, response.getStatusCode().value());
        Assertions.assertNull(response.getBody());
        Mockito.verify(fileService).deleteFile("fileId");
    }

    @Test
    public void listFilesReturnsEmptyPageIfNoFilesExist() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<FileSummaryDTO> page = new PageImpl<>(List.of(), pageable, 0);
        
        Mockito.when(fileService.getFiles(null, null, null, pageable)).thenReturn(page);
        var response = controller.listFiles(null, null, null, pageable);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(0, response.getBody().getContent().size());
    }

    @Test
    public void listFilesReturnsSingleFileIfOnlyOneExists() {
        FileService fileService = Mockito.mock(FileService.class);
        UploadService uploadService = Mockito.mock(UploadService.class);
        FileShareService fileShareService = Mockito.mock(FileShareService.class);
        FileController controller = new FileController(fileService, uploadService, fileShareService);
        PageRequest pageable = PageRequest.of(0, 10);
        Instant now = Instant.now();
        FileSummaryDTO dto = new FileSummaryDTO("id1", "single.png", "image/png", 123L, now, now, "COMPLETED", "owner1");
        PageImpl<FileSummaryDTO> page = new PageImpl<>(List.of(dto), pageable, 1);
        
        Mockito.when(fileService.getFiles(null, null, null, pageable)).thenReturn(page);
        var response = controller.listFiles(null, null, null, pageable);
        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(1, response.getBody().getContent().size());
        
        String filename = "";
        try {
            filename = (String) response.getBody().getContent().get(0).getClass().getMethod("filename").invoke(response.getBody().getContent().get(0));
        } catch (Exception e) {
            try {
                filename = (String) response.getBody().getContent().get(0).getClass().getMethod("getFilename").invoke(response.getBody().getContent().get(0));
            } catch (Exception ex) { }
        }
        Assertions.assertEquals("single.png", filename);
    }
}
