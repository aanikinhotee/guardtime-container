package ee.guardtime.proov.zip;

import ee.guardtime.proov.GuardtimeTests;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class ZipServiceTest extends GuardtimeTests {
  @Test
  public void testZipFile() throws Exception {
    final String filename = "test.a";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);

    ZipService zipService = new ZipService();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(out);
    zipService.addFileToZip(zipOutputStream, is, filename);
    zipOutputStream.close();

    byte[] zipedFile = out.toByteArray();


    FileReference fileReference = zipService.unzipFileMultiple(new ByteArrayInputStream(zipedFile)).get(0);

    assertEquals("test.a", fileReference.getFilename());
    assertEquals("aaaa", new String(fileReference.getContent()));
  }
}
