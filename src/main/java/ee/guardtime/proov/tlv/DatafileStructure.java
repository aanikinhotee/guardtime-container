package ee.guardtime.proov.tlv;

import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.hashing.UnknownHashAlgorithmException;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVParserException;
import com.guardtime.ksi.tlv.TLVStructure;

import java.util.List;

/**
 * Package: ee.guardtime.proov
 * User: anton
 */
public class DatafileStructure extends TLVStructure {

  private String uri;
  private HashAlgorithm hashAlgorithm;
  private DataHash hash;

  public static final int DATAFILE_TYPE = 0x1;
  private static final int DATAFILE_URI_TYPE = 0x2;
  private static final int DATAFILE_HASH_ALGORITHM_TYPE = 0x3;
  private static final int DATAFILE_HASH_TYPE = 0x4;

  public int getContentLength() {
    return rootElement.getContentLength();
  }

  public DatafileStructure(TLVElement rootElement) throws TLVParserException, UnknownHashAlgorithmException {
    super(rootElement);


    List<TLVElement> childs = rootElement.getChildElements();

    for (TLVElement element : childs) {
      switch (element.getType()) {
        case DATAFILE_URI_TYPE:
          this.uri = this.readOnce(element).getDecodedString();
          break;
        case DATAFILE_HASH_ALGORITHM_TYPE:
          this.hashAlgorithm = HashAlgorithm.getByName(this.readOnce(element).getDecodedString());
          break;
        case DATAFILE_HASH_TYPE:
          this.hash = this.readOnce(element).getDecodedDataHash();
          break;
      }
    }
  }

  public DatafileStructure(){
  }

  public void addData(String uri, HashAlgorithm hashAlgorithm, DataHash hash) throws TLVParserException {
    this.uri = uri;
    this.hashAlgorithm = hashAlgorithm;
    this.hash = hash;

    rootElement = new TLVElement(false, false, DATAFILE_TYPE);
    rootElement.addChildElement(TLVElement.create(DATAFILE_HASH_ALGORITHM_TYPE, hashAlgorithm.getName()));
    rootElement.addChildElement(TLVElement.create(DATAFILE_HASH_TYPE, hash));
    rootElement.addChildElement(TLVElement.create(DATAFILE_URI_TYPE, uri));
  }

  public String getUri() {
    return uri;
  }

  public HashAlgorithm getHashAlgorithm() {
    return hashAlgorithm;
  }

  public DataHash getHash() {
    return hash;
  }

  @Override
  public String toString() {
    return "DatafileStructure{" +
      "uri='" + uri + '\'' +
      ", hashAlgorithm=" + hashAlgorithm.getName() +
      ", hash=" + hash +
      '}';
  }

  @Override
  public int getElementType() {
    return 1;
  }
}
