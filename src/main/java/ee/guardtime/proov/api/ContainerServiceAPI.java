package ee.guardtime.proov.api;

import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.service.client.http.HttpClientSettings;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Container service for storing datafiles and signatures.
 * Should be possible to create container, add new signature.
 * Also should be possible to read existing containers, add and remove signatures from there.
 */
public interface ContainerServiceAPI {

  void initializeFromExisting(HttpClientSettings httpClientSettings, File inputFile);

  void initialize(HttpClientSettings httpClientSettings, File outputFile);

  void initialize(HttpClientSettings httpClientSettings, OutputStream outputStream);

  void finish();

  void addFilesAndSign(HashAlgorithm fileHashingAlgorithm, List<File> files);

  void addFileAndSign(HashAlgorithm fileHashingAlgorithm, InputStream inputStream, String filename);

  void removeSignature(String signatureUri);
}
