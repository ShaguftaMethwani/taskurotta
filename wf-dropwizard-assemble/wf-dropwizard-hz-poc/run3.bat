start java -Dts.hz.port=7778 -Ddw.http.port=8831 -Ddw.http.adminPort=8832 -Ddw.logging.file.currentLogFilename="./target/logs/service3.log" -jar target/wf-dropwizard-hz-poc-0.1.0-SNAPSHOT.jar server src/main/resources/conf.yml