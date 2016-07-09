package ee.guardtime.proov.api;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.service.client.KSIServiceCredentials;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.service.client.http.HttpClientSettings;
import ee.guardtime.proov.ContainerServiceException;
import ee.guardtime.proov.GuardtimeTests;
import ee.guardtime.proov.zip.FileReference;
import ee.guardtime.proov.zip.ZipService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class ContainerServiceTest extends GuardtimeTests {

  HttpClientSettings httpClientSettings;

  @Before
  public void setUp() {

    String username = System.getProperty("ksi.gw.username");
    String password = System.getProperty("ksi.gw.password");
    String signingUri = System.getProperty("ksi.gw.signing.uri");
    String extendingUri = System.getProperty("ksi.gw.extending.uri");
    String publicationUri = System.getProperty("ksi.gw.publications.uri");


    ServiceCredentials credentials = new KSIServiceCredentials(username, password);
    httpClientSettings = new HttpClientSettings(signingUri,
      extendingUri,
      publicationUri,
      credentials);
  }

  /**
   * this test used to test really large binary file
   * this file will not be delivered with project
   * the output container will be the same big as original file
   *
   * @throws IOException
   * @throws KSIException
   */
  @Test
  @Ignore
  public void testLargeFile2() throws IOException, KSIException {
    final File file = new File("/home/anton/Downloads/ISO/tomtom.iso");
    FileInputStream fis = new FileInputStream(file);
    ContainerService containerService = new ContainerService();
    containerService.initialize(httpClientSettings, new File("/tmp/zzzz.zip"));
    containerService.addFileAndSign(HashAlgorithm.SHA2_256, fis, file.getName());
    containerService.finish();
  }



  @Test
  public void testLargeFile() throws IOException, KSIException {
    File file = null;
    File file2 = null;

    try {
      file = File.createTempFile("large_", ".tmp");
      RandomAccessFile f = new RandomAccessFile(file, "rw");
      f.setLength(1024 * 1024 * 1024);
      f.close();
      FileInputStream fis = new FileInputStream(file);

      file2 = File.createTempFile("large_", ".tmp");
      RandomAccessFile f2 = new RandomAccessFile(file2, "rw");
      f2.setLength(1024 * 1024 * 1024);
      f2.close();
      FileInputStream fis2 = new FileInputStream(file2);


      ContainerService containerService = new ContainerService();
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      containerService.initialize(httpClientSettings, outputStream);

      containerService.addFileAndSign(HashAlgorithm.SHA2_256, fis, file.getName());
      containerService.addFileAndSign(HashAlgorithm.SHA2_256, fis2, file2.getName());

      containerService.finish();

      //writeToTempFile(outputStream.toByteArray(), "4");

    } finally {
      // cleanup system
      if(file!= null && file.delete()) System.out.println("deleted " + file.getName());
      if(file2!= null && file2.delete()) System.out.println("deleted " + file2.getName());
    }

  }


  @Test
  public void testLotsOfFiles2() throws IOException, KSIException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<File> files = new ArrayList<>();

    try {
      for (int i = 0; i < 10000; i++) {
        final File file = getTempFileSmall();
        files.add(file);
      }

      ContainerService containerService = new ContainerService();
      containerService.initialize(httpClientSettings, outputStream);
      containerService.addFilesAndSign(HashAlgorithm.SHA2_256, files);
      containerService.finish();

      //writeToTempFile(outputStream.toByteArray(), "2");
    } finally {
      for(File file : files){
        // cleanup system
        if(file.delete()) System.out.println("deleted " + file.getName());
      }
    }


  }

  @Test
  public void testLotsOfFiles1() throws IOException, KSIException {
    ContainerService containerService = new ContainerService();
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    containerService.initialize(httpClientSettings, outputStream);
    for(int i = 0; i < 10; i ++) {
      final File file = getTempFileSmall();
      FileInputStream fis = new FileInputStream(file);

      containerService.addFileAndSign(HashAlgorithm.SHA2_256, fis, file.getName());
    }

    containerService.finish();

    //writeToTempFile(outputStream.toByteArray(), "1");
  }

  @Test
  public void testAddFilesAndSign() throws IOException, KSIException {

    ContainerService containerService = new ContainerService();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    containerService.initialize(httpClientSettings, outputStream);

    List<File> fileList = new ArrayList<>();
    fileList.add(getTempFileSmall());
    fileList.add(getTempFileSmall());

    containerService.addFilesAndSign(HashAlgorithm.SHA2_256, fileList);

    containerService.finish();

    final byte[] zipFileBytes = outputStream.toByteArray();

    //writeToTempFile(zipFileBytes, "3");

    ZipService zipService = new ZipService();
    List<FileReference> files = zipService.unzipFileMultiple(new ByteArrayInputStream(zipFileBytes));

    assertEquals(4, files.size());
  }

  @Test(expected = ContainerServiceException.class)
  public void testInit(){
    ContainerService containerService = new ContainerService();
    containerService.initialize(httpClientSettings, new ByteArrayOutputStream());
    containerService.initialize(httpClientSettings, new ByteArrayOutputStream());
  }


  @Test(expected = ContainerServiceException.class)
  public void testFinish() throws IOException {
    ContainerService containerService = new ContainerService();
    containerService.finish();
  }

  @Test(expected = ContainerServiceException.class)
  public void testAddFileException() throws Exception {
    ContainerService containerService = new ContainerService();
    final FileReference fileReference = getFileReference("test.a");
    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileReference.getContent());
    containerService.addFileAndSign(HashAlgorithm.SHA2_256, inputStream, fileReference.getFilename());
  }

  @Test
  public void testAddFile() throws Exception {
    ContainerService containerService = new ContainerService();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    containerService.initialize(httpClientSettings, outputStream);
    final String filename = "test.a";
    final FileReference fileReference = getFileReference(filename);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileReference.getContent());
    containerService.addFileAndSign(HashAlgorithm.SHA2_256, inputStream, fileReference.getFilename());


    final FileReference fileReference1 = getFileReference("test.b");
    ByteArrayInputStream inputStream1 = new ByteArrayInputStream(fileReference1.getContent());
    containerService.addFileAndSign(HashAlgorithm.SHA2_256, inputStream1, fileReference1.getFilename());

    containerService.finish();

    final byte[] zipFileBytes = outputStream.toByteArray();

    writeToTempFile(zipFileBytes, "0");

    ZipService zipService = new ZipService();
    List<FileReference> files = zipService.unzipFileMultiple(new ByteArrayInputStream(zipFileBytes));

    assertEquals(6, files.size());
    for(FileReference file : files){
      if(Objects.equals(filename, file.getFilename())) {
        assertEquals("aaaa", new String(file.getContent()));
      }
    }
  }

  @Test
  public void testAppendToZip() throws IOException, KSIException {

    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");

    final File oldzip = File.createTempFile("oldzip_", ".zip");
    Files.copy(inputStream, oldzip.toPath(), StandardCopyOption.REPLACE_EXISTING);

    ContainerService containerService = new ContainerService();

    final File newzip = File.createTempFile("zip_", ".zip");

    containerService.initializeFromExisting(httpClientSettings, oldzip, newzip);

    final String filename = "test.c";
    final FileReference fileReference = getFileReference(filename);
    ByteArrayInputStream is = new ByteArrayInputStream(fileReference.getContent());

    containerService.addFileAndSign(HashAlgorithm.SHA2_256, is, filename);

    containerService.finish();

    if(newzip.renameTo(oldzip)) System.out.println("renamed");

  }

}
