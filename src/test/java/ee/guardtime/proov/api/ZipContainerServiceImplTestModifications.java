package ee.guardtime.proov.api;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.hashing.UnknownHashAlgorithmException;
import com.guardtime.ksi.service.client.KSIServiceCredentials;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.service.client.http.HttpClientSettings;
import com.guardtime.ksi.tlv.TLVParserException;
import ee.guardtime.proov.GuardtimeTests;
import ee.guardtime.proov.zip.FileReference;
import ee.guardtime.proov.zip.ZipService;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Package: ee.guardtime.proov.api
 * User: anton
 * Date: 7/10/16
 * Time: 12:15 AM
 */
public class ZipContainerServiceImplTestModifications extends GuardtimeTests {

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

  @Test
  public void testRemoveSignature() throws IOException, TLVParserException, UnknownHashAlgorithmException {
    ContainerServiceAPI zipContainerServiceImpl = new ZipContainerServiceImpl();
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");

    final File oldzip = File.createTempFile("oldzip_", ".zip");
    Files.copy(inputStream, oldzip.toPath(), StandardCopyOption.REPLACE_EXISTING);

    System.out.println("Old filename = " + oldzip.getName());

    zipContainerServiceImpl.initializeFromExisting(httpClientSettings, oldzip);

    final String signatureUriToRemove = "/META-INF/signature1.ksi";
    zipContainerServiceImpl.removeSignature(signatureUriToRemove);

    zipContainerServiceImpl.finish();


    ZipService zipService = new ZipService();
    List<FileReference> fileReferences = zipService.unzipFileMultiple(new FileInputStream(oldzip));

    assertEquals(3, fileReferences.size());
  }


  @Test
  public void testAppendToZip() throws IOException, KSIException {
    InputStream originalInputStream = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");

    final File oldzip = File.createTempFile("oldzip_", ".zip");
    Files.copy(originalInputStream, oldzip.toPath(), StandardCopyOption.REPLACE_EXISTING);

    System.out.println("Old filename = " + oldzip.getName());

    ContainerServiceAPI zipContainerServiceImpl = new ZipContainerServiceImpl();

    zipContainerServiceImpl.initializeFromExisting(httpClientSettings, oldzip);

    final String filename = "test.c";
    final FileReference fileReference = getFileReference(filename);
    ByteArrayInputStream cFileInputStream = new ByteArrayInputStream(fileReference.getContent());
    zipContainerServiceImpl.addFileAndSign(HashAlgorithm.SHA2_256, cFileInputStream, filename);
    zipContainerServiceImpl.finish();


    ZipService zipService = new ZipService();
    List<FileReference> fileReferences = zipService.unzipFileMultiple(new FileInputStream(oldzip));

    assertEquals(9, fileReferences.size());
  }
}


