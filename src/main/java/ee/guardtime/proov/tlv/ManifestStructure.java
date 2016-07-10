package ee.guardtime.proov.tlv;

import com.guardtime.ksi.hashing.UnknownHashAlgorithmException;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVParserException;
import com.guardtime.ksi.tlv.TLVStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * This is class for storing metadata about signature and related datafiles.
 * For storing information used TLV format. In current implementation size of Manifest cannot be more
 * than 64KB. It mean that you have to split datafiles between different manifests if this size exceeded.
 *
 */
public class ManifestStructure extends TLVStructure {

  private static final int MANIFEST_TYPE = 0x0;
  private static final int DATAFILE_TYPE = 0x1;
  private static final int SIGNATURE_URI_TYPE = 0x10;

  List<DatafileStructure> datafiles = new ArrayList<>();
  String signatureUri;

  public int getContentLength() {
    return rootElement.getContentLength();
  }

  public ManifestStructure(TLVElement rootElement) throws TLVParserException, UnknownHashAlgorithmException {
    super(rootElement);

    List<TLVElement> childs = rootElement.getChildElements();

    for (TLVElement element : childs) {
      switch (element.getType()) {
        case (SIGNATURE_URI_TYPE):
          this.signatureUri = this.readOnce(element).getDecodedString();
          break;
        case (DATAFILE_TYPE):
          DatafileStructure datafileStructure = new DatafileStructure(element);
          this.datafiles.add(datafileStructure);
          break;
      }
    }
  }

  public ManifestStructure() {
    this.rootElement = new TLVElement(false, false, MANIFEST_TYPE);
  }

  public void addDatafile(DatafileStructure datafileStructure) throws TLVParserException {
    this.datafiles.add(datafileStructure);
    this.rootElement.addChildElement(datafileStructure.getRootElement());
  }

  public void addSignatureUri(String signatureUri) throws TLVParserException {
    this.signatureUri = signatureUri;
    this.rootElement.addChildElement(TLVElement.create(SIGNATURE_URI_TYPE, signatureUri));
  }

  @Override
  public int getElementType() {
    return 0;
  }

  public List<DatafileStructure> getDatafiles() {
    return datafiles;
  }

  public String getSignatureUri() {
    return signatureUri;
  }

  @Override
  public String toString() {
    return "ManifestStructure{" +
      "datafiles=" + datafiles +
      ", signatureUri='" + signatureUri + '\'' +
      '}';
  }
}
