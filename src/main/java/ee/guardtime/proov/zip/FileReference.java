package ee.guardtime.proov.zip;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class FileReference {

  byte[] content;
  String filename;

  public FileReference(byte[] content, String filename) {
    this.content = content;
    this.filename = filename;
  }

  public byte[] getContent() {
    return content;
  }

  public String getFilename() {
    return filename;
  }

}
