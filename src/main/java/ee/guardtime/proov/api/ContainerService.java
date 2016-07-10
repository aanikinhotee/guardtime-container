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

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class ContainerService {


  private List<ManifestStructure> manifestStructures = new ArrayList<>();
  private Map<String, List<String>> signedFiles = new HashMap<>();
  private Map<String, String> signedManifests = new HashMap<>();
  private List<String> signatureUris = new ArrayList<>();
  private ZipOutputStream newZipOutputStream;
  private int counter;
  private HttpClientSettings httpClientSettings;
  private File originalZipFile = null;
  private File tempZipFile = null;


  private List<String> zipEntryNames = new ArrayList<>();

  /**
   *
   * @return collection of signatures in container
   */
  public List<String> getSignatureUris() {
    return signatureUris;
  }

  /**
   *
   * @return collection of signed files in container
   */
  public Map<String, List<String>> getSignedFiles() {
    return signedFiles;
  }

  /**
   *
   * @return collection of signed manifests
   */
  public Map<String, String> getSignedManifests() {
    return signedManifests;
  }

  /**
   * Initialize container and open streams for adding new files
   * suitable for large files if use FileOutputStream or something like that
   * using old container to add new signatures
   *
   */
  public void initializeFromExisting(HttpClientSettings httpClientSettings, File inputFile) throws IOException, TLVParserException, UnknownHashAlgorithmException {
    if(this.newZipOutputStream != null){
      throw  new ContainerServiceException("Already initialized");
    }

    this.tempZipFile = File.createTempFile("zip_", ".zip");
    this.originalZipFile = inputFile;

    ZipInputStream zis = new ZipInputStream(new FileInputStream(originalZipFile));
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile));

    ZipEntry entry;
    while((entry = zis.getNextEntry()) != null){

      if(entry.getName().startsWith("/META-INF/manifest")){

        ZipService zipService = new ZipService();
        FileReference fileReference = zipService.unzipOneEntry(zis, entry);
        ManifestStructure manifestStructure = new ManifestStructure(TLVElement.create(fileReference.getContent()));

        List<String> datafileUris = getSignedFilesList(manifestStructure);

        signedManifests.put(manifestStructure.getSignatureUri(), entry.getName());
        signedFiles.put(manifestStructure.getSignatureUri(), datafileUris);
        signatureUris.add(manifestStructure.getSignatureUri());

        manifestStructures.add(manifestStructure);
      }

      zipEntryNames.add(entry.getName());
    }

    zis.close();
    this.counter = manifestStructures.size() + 1;
    this.newZipOutputStream = zos;
    this.httpClientSettings = httpClientSettings;
  }

  /**
   * Initialize container and open streams for adding new files
   * output goes directly to file in filesystem
   *
   */
  public void initialize(HttpClientSettings httpClientSettings, File outputFile) throws FileNotFoundException {
    initialize(httpClientSettings, new FileOutputStream(outputFile));
  }

  /**
   * Initialize container and open streams for adding new files
   * suitable for large files if use FileOutputStream or something like that
   */
  void initialize(HttpClientSettings httpClientSettings, OutputStream outputStream) {
    if(this.newZipOutputStream != null){
      throw  new ContainerServiceException("Already initialized");
    }
    this.counter = 1;
    this.newZipOutputStream = new ZipOutputStream(outputStream);
    this.httpClientSettings = httpClientSettings;
  }

  /**
   * Finish writing process and close streams
   *
   * @throws IOException
   */
  public void finish() throws IOException {
    if(newZipOutputStream == null) {
      throw new ContainerServiceException("Container service not initialized");
    }

    if(originalZipFile != null) {
      ZipInputStream originalInputStream = new ZipInputStream(new FileInputStream(originalZipFile));
      ZipService zipService = new ZipService();
      zipService.copyEntryFromZip2Zip(originalInputStream, newZipOutputStream, zipEntryNames);
    }
    newZipOutputStream.close();

    if(originalZipFile != null) {
      Path source = tempZipFile.toPath();
      Path target = originalZipFile.toPath();
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }


  /**
   * Add and sign list of files
   *
   * @param files list of files
   * @throws KSIException
   * @throws IOException
   */
  public void addFilesAndSign(HashAlgorithm fileHashingAlgorithm, List<File> files) throws KSIException, IOException {
    if(newZipOutputStream == null) {
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
        ZipEntry entry = zipService.addFileToZip(newZipOutputStream, is, file.getName());
        zipEntryNames.add(entry.getName());
      }
    }


    signAndAddManifest(signatureUri, manifestStructure);
  }

  /**
   * Add new file for signing
   *
   * @param inputStream new file input stream
   * @param filename new filename
   * @throws IOException
   * @throws KSIException
   */
  public void addFileAndSign(HashAlgorithm fileHashingAlgorithm, InputStream inputStream, String filename) throws IOException, KSIException {
    if(newZipOutputStream == null) {
      throw new ContainerServiceException("Container service not initialized");
    }

    ZipService zipService = new ZipService();

    ZipEntry entry = zipService.addFileToZip(newZipOutputStream, inputStream, filename);
    zipEntryNames.add(entry.getName());

    TLVService tlvService = new TLVService();
    final String signatureUri = "/META-INF/signature" + counter + ".ksi";
    ManifestStructure manifestStructure = tlvService.createEmptyManifest(signatureUri);
    manifestStructure = tlvService.addDatafile(fileHashingAlgorithm, manifestStructure, inputStream, filename);

    signAndAddManifest(signatureUri, manifestStructure);
  }

  /**
   * Removes signature from container
   *
   * @param signatureUri URI of signature file in container
   */
  public void removeSignature(String signatureUri) {
    List<String> datafileUris = getSignedFiles().get(signatureUri);
    String manifestUri = getSignedManifests().get(signatureUri);

    zipEntryNames.remove(signatureUri);

    for(String datafileUri : datafileUris){
      zipEntryNames.remove(datafileUri);
    }

    zipEntryNames.remove(manifestUri);
  }


  private void signAndAddManifest(String signatureUri, ManifestStructure manifestStructure) throws KSIException, IOException {
    TLVService tlvService = new TLVService();
    ZipService zipService = new ZipService();

    ByteArrayOutputStream manifestBaos = new ByteArrayOutputStream();
    manifestStructure.writeTo(manifestBaos);

    final byte[] manifestBytes = manifestBaos.toByteArray();

    final String manifestUri = "/META-INF/manifest" + counter + ".tlv";
    ZipEntry entry = zipService.addFileToZip(newZipOutputStream, new ByteArrayInputStream(manifestBytes), manifestUri);
    zipEntryNames.add(entry.getName());


    InputStream isFromManifest = new ByteArrayInputStream(manifestBytes);
    DataHash manifestDataHash = tlvService.calculateHash(HashAlgorithm.SHA2_256, isFromManifest);

    byte[] signature = signFile(manifestDataHash);

    ZipEntry entry1 = zipService.addFileToZip(newZipOutputStream, new ByteArrayInputStream(signature), signatureUri);
    zipEntryNames.add(entry1.getName());

    List<String> datafileUris = getSignedFilesList(manifestStructure);

    manifestStructures.add(manifestStructure);
    signedFiles.put(signatureUri, datafileUris);
    signedManifests.put(signatureUri, manifestUri);
    signatureUris.add(signatureUri);

    counter++;
  }


  private List<String> getSignedFilesList(ManifestStructure manifestStructure) {
    List<DatafileStructure> datafiles = manifestStructure.getDatafiles();
    List<String> datafileUris = new ArrayList<>();

    for(DatafileStructure datafileStructure: datafiles){
      datafileUris.add(datafileStructure.getUri());
    }
    return datafileUris;
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
