Guardtime signature container API
=================================
Create new container, add and sign files
----------------------------------------


### To execute unit test use following maven command:

mvn test -Dksi.gw.username= \
  -Dksi.gw.password= \
  -Dksi.gw.signing.uri=http://tryout.guardtime.net:8080/gt-signingservice \
  -Dksi.gw.extending.uri=http://tryout-extender.guardtime.net:8081/gt-extendingservice \
  -Dksi.gw.publications.uri=http://verify.guardtime.com/ksi-publications.bin