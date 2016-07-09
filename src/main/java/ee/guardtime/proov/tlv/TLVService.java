package ee.guardtime.proov.tlv;

import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.DataHasher;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.hashing.HashException;
import com.guardtime.ksi.tlv.TLVParserException;

import java.io.InputStream;

/**
 * Package: ee.guardtime.proov.tlv
 * User: anton
 */
public class TLVService {


  /**
   * Calculate DataHas for inputstream
   *
   * @param hashAlgorithm hash algorithm used
   * @param inputStream stream
   * @return DataHas
   * @throws HashException
   */
  public DataHash calculateHash(HashAlgorithm hashAlgorithm, InputStream inputStream) throws HashException {
    DataHasher dataHasher = new DataHasher(hashAlgorithm);
    dataHasher = dataHasher.addData(inputStream);
    return dataHasher.getHash();
  }


  /**
   * add new datafile into existinf manifest
   *
   * @param manifestStructure manifest
   * @param is new file input stream
   * @param filename new filename
   * @return manifest
   * @throws HashException
   * @throws TLVParserException
   */
  public ManifestStructure addDatafile(HashAlgorithm fileHashingAlgorithm, ManifestStructure manifestStructure,
                                       InputStream is, String filename) throws HashException, TLVParserException {
    final DatafileStructure datafileStructure = getDatafileStructure(fileHashingAlgorithm, is, filename);
    manifestStructure.addDatafile(datafileStructure);
    return manifestStructure;
  }


  /**
   * Create manifest structure in TLV format
   *
   * @param is new file input stream
   * @param filename new filename
   * @param signatureUri signature uri in zip file
   * @return manifest structure
   * @throws HashException
   * @throws TLVParserException
   */
  public ManifestStructure createManifest(HashAlgorithm fileHashingAlgorithm, InputStream is, String filename,
                                          String signatureUri) throws HashException, TLVParserException {
    ManifestStructure manifestStructure = createEmptyManifest(signatureUri);
    final DatafileStructure datafileStructure = getDatafileStructure(fileHashingAlgorithm, is, filename);
    manifestStructure.addDatafile(datafileStructure);
    return manifestStructure;
  }

  /**
   * create empty manifest
   *
   * @param signatureUri signature uri in zip file
   * @return empty manifest structure
   * @throws TLVParserException
   */
  public ManifestStructure createEmptyManifest(String signatureUri) throws TLVParserException {
    ManifestStructure manifestStructure = new ManifestStructure();
    manifestStructure.addSignatureUri(signatureUri);
    return manifestStructure;
  }

  DatafileStructure combineDatafileStructure(String uri, HashAlgorithm hashAlgorithm,
                                             DataHash hash) throws TLVParserException {
    DatafileStructure datafileStructure = new DatafileStructure();
    datafileStructure.addData(uri, hashAlgorithm, hash);
    return datafileStructure;
  }


  /**
   * generate DatafileStructure object
   *
   * @param fileHashingAlgorithm Hashing algorithm
   * @param is new file input stream
   * @param filename new filename
   * @return DatafileStructure
   * @throws HashException
   * @throws TLVParserException
   */
  public DatafileStructure getDatafileStructure(HashAlgorithm fileHashingAlgorithm, InputStream is,
                                                 String filename) throws HashException, TLVParserException {
    final DatafileStructure datafileStructure = new DatafileStructure();
    TLVService tlvService = new TLVService();
    DataHash dataHash = tlvService.calculateHash(fileHashingAlgorithm, is);
    datafileStructure.addData(filename, fileHashingAlgorithm, dataHash);
    return datafileStructure;
  }

}
