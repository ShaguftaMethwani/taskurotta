start java -Xmx64m -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -DassetsMode=dev -Dts.node.custom-name="node1" -Ddw.http.port=8811 -Ddw.http.adminPort=8812 -Ddw.logging.file.currentLogFilename="./target/logs/service1.log" -jar target/assemble-0.5.0-SNAPSHOT.jar server src/main/resources/hz-ora-mongo.yml