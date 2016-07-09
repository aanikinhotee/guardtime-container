package ee.guardtime.proov.api;

import com.guardtime.ksi.hashing.UnknownHashAlgorithmException;
import com.guardtime.ksi.service.client.KSIServiceCredentials;
import com.guardtime.ksi.service.client.ServiceCredentials;
import com.guardtime.ksi.service.client.http.HttpClientSettings;
import com.guardtime.ksi.tlv.TLVParserException;
import ee.guardtime.proov.GuardtimeTests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Package: ee.guardtime.proov.api
 * User: anton
 * Date: 7/10/16
 * Time: 12:15 AM
 */
public class ContainerServiceTest2 extends GuardtimeTests {

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
    ContainerService containerService = new ContainerService();
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");

    final File oldzip = File.createTempFile("oldzip_", ".zip");
    Files.copy(inputStream, oldzip.toPath(), StandardCopyOption.REPLACE_EXISTING);


    final File newzip = File.createTempFile("zip_", ".zip");
    containerService.initializeFromExisting(httpClientSettings, oldzip, newzip);

    containerService.removeSignature("META-INF/signature1.ksi");

    containerService.finish();

    if(newzip.renameTo(oldzip)) System.out.println("renamed");


  }
}


