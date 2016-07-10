package ee.guardtime.proov.zip;

import ee.guardtime.proov.GuardtimeTests;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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


  @Test
  public void copyEntryFromZip2Zip() throws IOException {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");

    ZipService zipService = new ZipService();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ZipOutputStream zos = new ZipOutputStream(out);
    ZipEntry entry;
    ZipEntry foundEntry = null;
    final ZipInputStream zipInputStream = new ZipInputStream(is);
    while((entry = zipInputStream.getNextEntry()) != null){
      if(Objects.equals("test.b", entry.getName())){
        foundEntry = entry;
        break;
      }
    }

    is.close();

    assertNotNull(foundEntry);

    InputStream is2 = this.getClass().getClassLoader().getResourceAsStream("0zip.zip");
    ZipInputStream zipInputStream2 = new ZipInputStream(is2);
    zipService.copyEntryFromZip2Zip(zipInputStream2, zos, Collections.singletonList(foundEntry.getName()));


    zos.close();
    byte[] zipedFile = out.toByteArray();


    final List<FileReference> fileReferences = zipService.unzipFileMultiple(new ByteArrayInputStream(zipedFile));
    assertEquals(1, fileReferences.size());
    FileReference fileReference = fileReferences.get(0);
    assertEquals("test.b", fileReference.getFilename());
    assertEquals("bbbbbb", new String(fileReference.getContent()));

  }
}
