start java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/assemble-0.8.0-SNAPSHOT.jar server src/main/resources/hz-ora-mongo.yml