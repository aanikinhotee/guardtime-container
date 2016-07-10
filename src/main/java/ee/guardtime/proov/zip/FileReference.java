package ee.guardtime.proov.zip;

/**
 * This class used for storing files while unzip-ing some entries from container.
 * It should not be used for unzip-ing large data files, but only manifests and signatures.
 *
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
