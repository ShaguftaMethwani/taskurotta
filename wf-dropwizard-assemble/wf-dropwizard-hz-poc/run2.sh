java -Ddw.http.port=8821 -Ddw.http.adminPort=8822 -Ddw.logging.file.currentLogFilename="./target/logs/service2.log" -jar target/wf-dropwizard-hz-poc-0.1.0-SNAPSHOT.jar server src/main/resources/conf.yml