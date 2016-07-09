package ee.guardtime.proov.zip;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class ZipService {

  private static final int DEFAULT_BUFFER_SIZE = 8192;




  /**
   * Unzip all files
   *
   * @param inputStream stream of zip file
   * @return list of file data + filename
   * @throws IOException
   */
  public List<FileReference> unzipFileMultiple(InputStream inputStream) throws IOException {
    ZipInputStream zis =
      new ZipInputStream(inputStream);

    ZipEntry entry;

    List<FileReference> fileReferences = new ArrayList<>();
    while((entry = zis.getNextEntry()) != null){
      fileReferences.add(unzipOneEntry(zis, entry));
    }
    return fileReferences;
  }

  /**
   * Add new file to existing zip file
   * zipOutputStream should be created first
   *
   * @param zos zip file output stream
   * @param inputStream new file input stream
   * @param filename new filename
   * @throws IOException
   */
  public void addFileToZip(ZipOutputStream zos, InputStream inputStream, String filename) throws IOException {
    // create byte buffer
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    // begin writing a new ZIP entry, positions the stream to the start of the entry data
    zos.putNextEntry(new ZipEntry(filename));

    int length;

    while ((length = inputStream.read(buffer)) > 0) {
      zos.write(buffer, 0, length);
    }

    zos.closeEntry();
  }

  public FileReference unzipOneEntry(ZipInputStream zis, ZipEntry entry) throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    ByteArrayOutputStream output = null;
    try
    {
      output = new ByteArrayOutputStream();
      int len;
      while ((len = zis.read(buffer)) > 0)
      {
        output.write(buffer, 0, len);
      }

      return new FileReference(output.toByteArray(), entry.getName());
    }
    finally
    {
      // we must always close the output file
      if(output!=null) output.close();
    }
  }

}
