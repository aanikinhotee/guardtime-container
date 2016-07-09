package ee.guardtime.proov.tlv;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.hashing.DataHasher;
import com.guardtime.ksi.hashing.HashAlgorithm;
import com.guardtime.ksi.hashing.HashException;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVParserException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Package: ee.guardtime.proov.tlv
 * User: anton
 */
public class TLVServiceTest {

  private DatafileStructure getDatafileStructureSHA2_256() throws HashException, TLVParserException {
    final String filename = "test.a";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
    TLVService tlvService = new TLVService();
    DataHash dataHash = tlvService.calculateHash(HashAlgorithm.SHA2_256, is);
    assertEquals("SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]", dataHash.toString());
    return tlvService.combineDatafileStructure(filename, HashAlgorithm.SHA2_256, dataHash);
  }

  private DatafileStructure getDatafileStructureSHA2_512() throws HashException, TLVParserException {
    final String filename = "test.a";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
    TLVService tlvService = new TLVService();
    DataHash dataHash = tlvService.calculateHash(HashAlgorithm.SHA2_512, is);
    assertEquals("SHA-512:[CF83E1357EEFB8BDF1542850D66D8007D620E4050B5715DC83F4A921D36" +
      "CE9CE47D0D13C5D85F2B0FF8318D2877EEC2F63B931BD47417A81A538327AF927DA3E]", dataHash.toString());
    return tlvService.combineDatafileStructure(filename, HashAlgorithm.SHA2_512, dataHash);
  }

  @Test
  public void testDataHash() throws HashException {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("test.a");
    TLVService tlvService = new TLVService();
    DataHash dataHash = tlvService.calculateHash(HashAlgorithm.SHA2_256, is);
    assertEquals("SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]", dataHash.toString());


    DataHash dataHash2 = tlvService.calculateHash(HashAlgorithm.SHA2_512, is);
    assertEquals("SHA-512:[CF83E1357EEFB8BDF1542850D66D8007D620E4050B5715DC83F4A921D36CE" +
      "9CE47D0D13C5D85F2B0FF8318D2877EEC2F63B931BD47417A81A538327AF927DA3E]", dataHash2.toString());
  }

  @Test
  public void testCreateDatafileStructure() throws HashException, TLVParserException {
    DatafileStructure datafileStructure = getDatafileStructureSHA2_256();
    assertEquals("DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}", datafileStructure.toString());
  }

  @Test
  public void testCreateDatafileStructure2() throws KSIException {
    DatafileStructure datafileStructure = getDatafileStructureSHA2_256();
    assertEquals("DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}", datafileStructure.toString());

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    datafileStructure.writeTo(out);

    byte[] datafileStructureData = out.toByteArray();
    DatafileStructure newStructure = new DatafileStructure(TLVElement.create(datafileStructureData));
    assertEquals("DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}", newStructure.toString());
  }

  @Test
  public void testCreateEmptyManifest() throws KSIException, FileNotFoundException {
    ManifestStructure manifestStructure = new ManifestStructure();
    assertEquals("ManifestStructure{datafiles=[], signatureUri='null'}", manifestStructure.toString());
  }

  @Test
  public void testCreateManifest() throws KSIException {
    TLVService tlvService = new TLVService();
    ManifestStructure manifestStructure =
      tlvService.createManifest(HashAlgorithm.SHA2_256, new ByteArrayInputStream("aaaa".getBytes()), "aaa.txt",
      "/META-INF/signature1.ksi");

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    manifestStructure.writeTo(out);

    byte[] manifestStructureData = out.toByteArray();
    ManifestStructure newManifestStructure = new ManifestStructure(TLVElement.create(manifestStructureData));

    assertEquals("/META-INF/signature1.ksi", newManifestStructure.getSignatureUri());
    assertEquals("aaa.txt", newManifestStructure.getDatafiles().get(0).getUri());

    DataHasher dataHasher = new DataHasher(HashAlgorithm.SHA2_256);
    dataHasher.addData("aaaa".getBytes());
    DataHash dataHash = dataHasher.getHash();

    assertEquals(dataHash, newManifestStructure.getDatafiles().get(0).getHash());

  }

  @Test
  public void testCreateManifest2() throws KSIException, FileNotFoundException {
    ManifestStructure manifestStructure = new ManifestStructure();
    assertEquals("ManifestStructure{datafiles=[], signatureUri='null'}", manifestStructure.toString());


    DatafileStructure datafileStructure = getDatafileStructureSHA2_256();
    manifestStructure.addDatafile(datafileStructure);
    assertEquals("ManifestStructure{datafiles=[DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}], " +
      "signatureUri='null'}", manifestStructure.toString());


    DatafileStructure datafileStructure2 = getDatafileStructureSHA2_256();
    manifestStructure.addDatafile(datafileStructure2);
    assertEquals("ManifestStructure{datafiles=[DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}, " +
      "DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}], " +
      "signatureUri='null'}", manifestStructure.toString());


    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    manifestStructure.writeTo(out);

    byte[] manifestStructureData = out.toByteArray();
    ManifestStructure newManifestStructure = new ManifestStructure(TLVElement.create(manifestStructureData));

    assertEquals("ManifestStructure{datafiles=[DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}, " +
      "DatafileStructure{uri='test.a', hashAlgorithm=SHA-256, " +
      "hash=SHA-256:[61BE55A8E2F6B4E172338BDDF184D6DBEE29C98853E0A0485ECEE7F27B9AF0B4]}], " +
      "signatureUri='null'}", newManifestStructure.toString());

  }
}
