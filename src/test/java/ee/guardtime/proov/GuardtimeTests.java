package ee.guardtime.proov;

import ee.guardtime.proov.zip.FileReference;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class GuardtimeTests {

  public File getTempFileSmall() throws IOException {
    final File file = File.createTempFile("large_", ".tmp");
    RandomAccessFile f = new RandomAccessFile(file, "rw");
    f.setLength(1024);
    f.close();
    return file;
  }

  public void writeToTempFile(byte[] bytes, String prefix){
    Path path = Paths.get("/tmp/" + prefix + "zip.zip");
    try {
      Files.write(path, bytes);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  public FileReference getFileReference(String filename) throws IOException {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
    byte[] bytes = IOUtils.toByteArray(is);
    return new FileReference(bytes, filename);
  }
}
