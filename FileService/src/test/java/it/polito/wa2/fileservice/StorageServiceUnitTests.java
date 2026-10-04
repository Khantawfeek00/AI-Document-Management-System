package it.polito.wa2.fileservice;

import io.minio.MinioClient;
import it.polito.wa2.fileservice.services.StorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.ByteArrayInputStream;
import io.minio.GetObjectResponse;

public class StorageServiceUnitTests {
    @Test
    public void testPutObjectCallsMinioClientPutObject() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        StorageService service = new StorageService(minioClient);
        ByteArrayInputStream data = new ByteArrayInputStream("data".getBytes());
        service.putObject("bucket", "key", data, 4);
        Mockito.verify(minioClient).putObject(Mockito.any());
    }

    @Test
    public void testGetObjectCallsMinioClientGetObject() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        StorageService service = new StorageService(minioClient);
        GetObjectResponse mockResponse = Mockito.mock(GetObjectResponse.class);
        Mockito.when(minioClient.getObject(Mockito.any())).thenReturn(mockResponse);
        var result = service.getObject("bucket", "key");
        Assertions.assertNotNull(result);
        Mockito.verify(minioClient).getObject(Mockito.any());
    }
}
