package ee.guardtime.proov.api;

import com.guardtime.ksi.KSI;
import com.guardtime.ksi.KSIBuilder;
import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.hashing.UnknownHashAlgorithmException;
import com.guardtime.ksi.service.client.http.HttpClientSettings;
import com.guardtime.ksi.service.http.simple.SimpleHttpClient;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVParserException;
import com.guardtime.ksi.trust.X509CertificateSubjectRdnSelector;
import com.guardtime.ksi.unisignature.KSISignature;
import ee.guardtime.proov.ContainerServiceException;
import ee.guardtime.proov.tlv.DatafileStructure;
import ee.guardtime.proov.tlv.ManifestStructure;
import ee.guardtime.proov.tlv.TLVService;
import ee.guardtime.proov.zip.FileReference;
import ee.guardtime.proov.zip.ZipService;
import org.apache.commons.io.IOUtils;

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
public class ContainerService {


  private List<ManifestStructure> manifestStructures = new ArrayList<>();
  private ZipOutputStream zipFile;
  private int counter;
  private HttpClientSettings httpClientSettings;


  /**
   * initialize container and open streams for adding new files
   * suitable for large files if use FileOutputStream or something like that
   * using old container to add new signatures
   *
   */
  public void initializeFromExisting(HttpClientSettings httpClientSettings, File inputFile,
                                     File outputFile) throws IOException, TLVParserException, UnknownHashAlgorithmException {
    initializeFromExisting(httpClientSettings, new FileInputStream(inputFile), new FileOutputStream(outputFile));
  }


  private void initializeFromExisting(HttpClientSettings httpClientSettings, InputStream inputStream,
                                      OutputStream outputStream) throws IOException,
    TLVParserException, UnknownHashAlgorithmException {
    if(this.zipFile != null){
      throw  new ContainerServiceException("Already initialized");
    }

    ZipInputStream zis = new ZipInputStream(inputStream);
    ZipOutputStream zos = new ZipOutputStream(outputStream);

    ZipEntry entry;
    while((entry = zis.getNextEntry()) != null){

      if(entry.getName().startsWith("/META-INF/manifest")){

        ZipService zipService = new ZipService();
        FileReference fileReference = zipService.unzipOneEntry(zis, entry);
        ManifestStructure manifestStructure = new ManifestStructure(TLVElement.create(fileReference.getContent()));
        manifestStructures.add(manifestStructure);
        zos.putNextEntry(new ZipEntry(entry.getName()));
        IOUtils.copy(new ByteArrayInputStream(fileReference.getContent()), zos);

      } else {
        zos.putNextEntry(new ZipEntry(entry.getName()));
        IOUtils.copy(zis, zos);
      }

      zos.closeEntry();
    }

    this.counter = manifestStructures.size() + 1;
    this.zipFile = zos;
    this.httpClientSettings = httpClientSettings;
  }

  /**
   * output goes to file
   *
   */
  public void initialize(HttpClientSettings httpClientSettings, File outputFile) throws FileNotFoundException {
    initialize(httpClientSettings, new FileOutputStream(outputFile));
  }

  /**
   * initialize container and open streams for adding new files
   * suitable for large files if use FileOutputStream or something like that
   */
  void initialize(HttpClientSettings httpClientSettings, OutputStream outputStream) {
    if(this.zipFile != null){
      throw  new ContainerServiceException("Already initialized");
    }
    this.counter = 1;
    this.zipFile = new ZipOutputStream(outputStream);
    this.httpClientSettings = httpClientSettings;
  }

  /**
   * finish writing and close streams
   *
   * @throws IOException
   */
  public void finish() throws IOException {
    if(zipFile == null) {
      throw new ContainerServiceException("Container service not initialized");
    }

    zipFile.close();
  }


  /**
   * add and sign list of files
   *
   * @param files list of files
   * @throws KSIException
   * @throws IOException
   */
  public void addFilesAndSign(HashAlgorithm fileHashingAlgorithm, List<File> files) throws KSIException, IOException {
    if(zipFile == null) {
      throw new ContainerServiceException("Container service not initialized");
    }

    String signatureUri = "/META-INF/signature" + counter + ".ksi";
    TLVService tlvService = new TLVService();
    ManifestStructure manifestStructure = tlvService.createEmptyManifest(signatureUri);
    ZipService zipService = new ZipService();

    for(File file : files) {

      DatafileStructure datafileStructure;
      try (FileInputStream is = new FileInputStream(file)) {
        datafileStructure = tlvService.getDatafileStructure(fileHashingAlgorithm, is, file.getName());
      }


      if (manifestStructure.getContentLength() + datafileStructure.getContentLength() > TLVElement.MAX_TLV16_CONTENT_LENGTH) {
        signAndAddManifest(signatureUri, manifestStructure);
        signatureUri = "/META-INF/signature" + counter + ".ksi";
        manifestStructure = tlvService.createEmptyManifest(signatureUri);
      }

      try (FileInputStream is = new FileInputStream(file)) {
        manifestStructure = tlvService.addDatafile(fileHashingAlgorithm, manifestStructure, is, file.getName());
      }


      try (FileInputStream is = new FileInputStream(file)) {
        zipService.addFileToZip(zipFile, is, file.getName());
      }
    }


    signAndAddManifest(signatureUri, manifestStructure);
  }

  /**
   * add new file for signing
   *
   * @param is new file input stream
   * @param filename new filename
   * @throws IOException
   * @throws KSIException
   */
  public void addFileAndSign(HashAlgorithm fileHashingAlgorithm, InputStream is, String filename) throws IOException, KSIException {
    if(zipFile == null) {
      throw new ContainerServiceException("Container service not initialized");
    }

    ZipService zipService = new ZipService();

    zipService.addFileToZip(zipFile, is, filename);

    TLVService tlvService = new TLVService();
    final String signatureUri = "/META-INF/signature" + counter + ".ksi";
    ManifestStructure manifestStructure = tlvService.createEmptyManifest(signatureUri);
    manifestStructure = tlvService.addDatafile(fileHashingAlgorithm, manifestStructure, is, filename);

    signAndAddManifest(signatureUri, manifestStructure);
  }


  private void signAndAddManifest(String signatureUri, ManifestStructure manifestStructure) throws KSIException, IOException {
    TLVService tlvService = new TLVService();
    ZipService zipService = new ZipService();

    ByteArrayOutputStream manifestBaos = new ByteArrayOutputStream();
    manifestStructure.writeTo(manifestBaos);

    final byte[] manifestBytes = manifestBaos.toByteArray();

    zipService.addFileToZip(zipFile, new ByteArrayInputStream(manifestBytes), "/META-INF/manifest" + counter + ".tlv");


    InputStream isFromManifest = new ByteArrayInputStream(manifestBytes);
    DataHash manifestDataHash = tlvService.calculateHash(HashAlgorithm.SHA2_256, isFromManifest);

    byte[] signature = signFile(manifestDataHash);

    zipService.addFileToZip(zipFile, new ByteArrayInputStream(signature), signatureUri);
    manifestStructures.add(manifestStructure);
    counter++;
  }

  private byte[] signFile(DataHash manifestDataHash) throws KSIException, IOException {
    KSI ksi = getKsi();
    KSISignature sig1 = ksi.sign(manifestDataHash);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    sig1.writeTo(baos);
    return baos.toByteArray();
  }

  private KSI getKsi() throws KSIException {
    SimpleHttpClient simpleHttpClient = new SimpleHttpClient(httpClientSettings);

    return new KSIBuilder()
      .setKsiProtocolSignerClient(simpleHttpClient)
      .setKsiProtocolExtenderClient(simpleHttpClient)
      .setKsiProtocolPublicationsFileClient(simpleHttpClient)
      .setPublicationsFileTrustedCertSelector(new X509CertificateSubjectRdnSelector("E=test@test.com"))
      .build();
  }

}
