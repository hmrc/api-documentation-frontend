# api-documentation-frontend

Frontend for API documentation - part of the Developer Hub

## Steps to run acceptance test

[**Note**: Acceptance and Unit tests will fail if they are running at the same time as api-documentation-frontend.]

1. Run Assets Frontend using command ```sm --start ASSETS_FRONTEND``` where HMRC projects have been checked out.

2. Install chrome driver if you have not done already .

   2a. If using MAC OS this can be done using command ```brew install chromedriver```
       You might have to un-quarantine the chromedriver ```xattr -d com.apple.quarantine /usr/local/bin/chromedriver```
   
   2b. If not using MAC chromedriver can be downloaded from **https://chromedriver.storage.googleapis.com/index.html?path=2.29/**
   
3. Go to API Documentation Frontend project and Run ```sbt clean test```. This will create some artifacts in the target directory for the acceptance
   test to pick up.
   
4. To execute test
   
   4a  On chrome use command ```sbt acceptance:test -Dbrowser=chrome-local``` or just ```sbt acceptance:test```.
   
   4b. On firefox use command ```sbt acceptance:test -Dbrowser=firefox-local```
   
   4c. To run all the tests run the shell file **run_all_test.sh**
       A report will also be generated identifying any dependencies that need upgrading. This requires that
       you have defined CATALOGUE_DEPENDENCIES_URL as an environment variable pointing to the dependencies
       endpoint on the Tax Platform Catalogue's API.   

## Current Known Issues

1. If you are planning to run test on Firefox use firefox version 46 or lower as selenium version 2.53.0 is not compatible to work with Firefox version 47. Old firefox
   releases can be downloaded from **https://ftp.mozilla.org/pub/firefox/releases/**

## Testing approach

* Unit tests should make up the majority of tests so that test coverage should drop marginally when run against only unit tests.
* Acceptance tests should be a thin layer of coverage on happy paths only to ensure that journeys hang together.

## Running locally with dependencies

To run locally use `./run_local_with_dependencies.sh` as well as starting any apis you want to see the documentation for with service manager. 

Note: API definitions (`mongo -d api-definition -c api`) will need their `serviceBaseUrl` pointing to `http://localhost:<port>`:
To update:

```
db.api.updateOne({ serviceName: “<your service name>” }, { $set: { serviceBaseUrl: “http://localhost:<port>” } })
```
