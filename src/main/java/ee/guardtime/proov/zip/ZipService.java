package ee.guardtime.proov.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZipService class used for manipulations with ZIP files.
 *
 */
public class ZipService {

  private static final int DEFAULT_BUFFER_SIZE = 8192;


  /**
   * Method for coping entries from one to another
   *
   * @param destinationOutputStream ZIP file output stream , place where we write to
   * @param originInputStream file inputStream read from
   * @param entriesToCopy List of entry names from old ZIP archive we need to copy
   * @throws IOException
   */
  public void copyEntryFromZip2Zip(ZipInputStream originInputStream, ZipOutputStream destinationOutputStream, List<String> entriesToCopy) throws IOException {
    ZipEntry entry;
    while((entry = originInputStream.getNextEntry()) != null) {
      if(entriesToCopy.contains(entry.getName())){
        destinationOutputStream.putNextEntry(new ZipEntry(entry));
        copyLarge(originInputStream, destinationOutputStream, new byte[DEFAULT_BUFFER_SIZE]);
        destinationOutputStream.closeEntry();
      }
    }
  }


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
  public ZipEntry addFileToZip(ZipOutputStream zos, InputStream inputStream, String filename) throws IOException {
    // create byte buffer
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    // begin writing a new ZIP entry, positions the stream to the start of the entry data
    final ZipEntry zipEntry = new ZipEntry(filename);
    zos.putNextEntry(zipEntry);

    int length;

    while ((length = inputStream.read(buffer)) > 0) {
      zos.write(buffer, 0, length);
    }

    zos.closeEntry();
    return zipEntry;
  }



  /**
   * Method for unziping one entry
   *
   * @param zis ZIP file input stream
   * @param entry selected entry from ZIP file
   * @return FileReference = file data + filename
   * @throws IOException
   */
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
      if(output!=null) output.close();
    }
  }


  private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
    long count;
    int n;
    for(count = 0L; -1 != (n = input.read(buffer)); count += (long)n) {
      output.write(buffer, 0, n);
    }

    return count;
  }

}
