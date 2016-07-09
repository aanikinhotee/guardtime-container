Guardtime signature container API
=================================
Create new container, add and sign files
----------------------------------------

Container is based on ZIP format and contains files with following structure:

  * datafile.xxx
  * META-INF/manifestX.tlv
  * META-INF/signatureX.ksi


### Initialization

API represented in **ConteinerService** class. First of all you need to initialize container
with method
````
initialize(HttpClientSettings httpClientSettings, File outputFile)
````

First parameter _httpClientSetting_ should contain Guardtime service information and credentials.
Second parameter _outputFile_ is a File where container will be stored after creation.

### Adding new signatures

To add new data file and sign it, you should use method
````
addFileAndSign(HashAlgorithm fileHashingAlgorithm, InputStream is, String filename)
````

First parameter _fileHashingAlgorithm_ define which hashing algorithm should be used.
Second parameter _is_ is an input stream of new file. And one more parameter _filename_ define what filename should be
assigned to data file in container.

### Adding multiple files

To add new multiple data files into container and sign them, you should use method
````
addFilesAndSign(HashAlgorithm fileHashingAlgorithm, List<File> files)
````

### Finalization

When all files added to container you should finalize process and call method
````
finish()
````

It will flush and close ZIP container's output stream.

### Open existing container and add more data files

To open and modify existing container you should initialize service with method
````
initializeFromExisting(HttpClientSettings httpClientSettings, File inputFile, File outputFile)
````

* httpClientSettings - parameter for Guardtime service parameters and credentials
* inputFile - existing container file
* outputFile - file for a new version of container

While standard ZIP format API does not allow make modifications in existing ZIP files, we should write down another
copy of container. After all modifications old version can be replaced with new version of file. It can be done outside
the service.

After initialization adding new data files and signatures can be done in normal way with methods
````
addFileAndSign(HashAlgorithm fileHashingAlgorithm, InputStream is, String filename)
````
or
````
addFilesAndSign(HashAlgorithm fileHashingAlgorithm, List<File> files)
````
and at the end of process finalized.


### Open existing container and remove signature

To remove signature you need initialized service, then you can execute method
````
removeSignature(String signatureUri)
````


### Example

Simplest example for creating container, adding and signing datafile is like this:

```java
     File file = new File("/tmp/datafile1.txt");
     FileInputStream fis = new FileInputStream(file);
     ContainerService containerService = new ContainerService();
     containerService.initialize(httpClientSettings, new File("/tmp/container.zip"));
     containerService.addFileAndSign(HashAlgorithm.SHA2_256, fis, file.getName());
     containerService.finish();
```

To execute unit test use following maven command:
-------------------------------------------------
```
mvn test -Dksi.gw.username= \
  -Dksi.gw.password= \
  -Dksi.gw.signing.uri=http://tryout.guardtime.net:8080/gt-signingservice \
  -Dksi.gw.extending.uri=http://tryout-extender.guardtime.net:8081/gt-extendingservice \
  -Dksi.gw.publications.uri=http://verify.guardtime.com/ksi-publications.bin
```
