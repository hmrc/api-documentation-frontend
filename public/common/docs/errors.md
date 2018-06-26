We use standard [HTTP status codes](/api-documentation/docs/reference-guide#http-status-codes) to show whether an API request succeeded or not. They're usually:
* in the 200 to 299 range if it succeeded; including code 202 if it was accepted by an API that needs to wait for further action
* in the 400 to 499 range if it didn't succeed because of a client error by your application
* in the 500 to 599 range if it didn't succeed because of an error on our server

Errors specific to each API are shown in its own Resources section, under Response. 
See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.

