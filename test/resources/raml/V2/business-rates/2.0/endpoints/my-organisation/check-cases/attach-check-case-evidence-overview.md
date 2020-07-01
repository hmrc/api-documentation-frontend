<p>This endpoint allows a user to attach evidence supporting a check case.</p>

<p>Attaching evidence is a two-step process:</p>
<ol class="list-number">
    <li class="font-xsmall">Call this endpoint and supply fileName, mimeType and redirect URLs in the body of the request</li>
    <li class="font-xsmall">Upload the evidence file as a multi-part form using the form fields contained in the response from Step 1</li>
</ol>

<p class="bold">Step 1</p>
<p>Request</p>

<pre>
POST /my-organisation/check-cases/{checkSubmissionId}/evidence HTTP/1.1
Authorization: Bearer ...
Content-Type: application/json
Accept: application/vnd.hmrc.2.0+json

{
  "fileName": "some_file.jpg",
  "mimeType": "image/jpeg",
  "successRedirect": "http://example.com/success",
  "errorRedirect": "http://example.com/error"
}
</pre>

<p>Response</p>

<pre>
{
    "reference": "533bedac-914a-474e-86d2-753d0b18f4bb",
    "uploadRequest": {
        "href": "https://www.upscan.tax.service.gov.uk",
        "fields": {
            "x-amz-meta-callback-url": "http://api.service.hmrc.gov.uk/business-rates-attachments/callback",
            "x-amz-date": "20180611T081926Z",
            "x-amz-credential": "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
            "x-amz-algorithm": "AWS4-HMAC-SHA256",
            "key": "533bedac-914a-474e-86d2-753d0b18f4bb",
            "acl": "private",
            "x-amz-signature": "xxxx",
            "Content-Type": "image/jpeg",
            "policy": "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMTAyNDAwLDIwOTcxNTIwXV19",
            "success_action_redirect": "http://example.com/success",
            "error_action_redirect": "http://example.com/error"
        }
    }
}
</pre>

<p class="bold">Step 2</p>
<p>In order to upload the file, the following multipart-form 
is sent as the body of a POST request which should contain
all the fields returned in "fields" map in the response above.</p>

<pre>
Request method:	POST
Request URI:	https://fus-inbound-8264ee52f589f4c0191aa94f87aa1aeb.s3.amazonaws.com
Headers:		Content-Type=multipart/form-data
Multiparts:		------------
				Content-Disposition: form-data; name = x-amz-meta-callback-url; filename = file
				Content-Type: text/plain
				https://business-rates-attachments.protected.mdtp/business-rates-attachments/callback
				------------
				Content-Disposition: form-data; name = x-amz-date; filename = file
				Content-Type: text/plain
				20180718T152714Z
				------------
				Content-Disposition: form-data; name = x-amz-credential; filename = file
				Content-Type: text/plain
				AKIAIFG3PRBHSAVV7BWA/20180718/eu-west-2/s3/aws4_request
				------------
				Content-Disposition: form-data; name = x-amz-meta-request-id; filename = file
				Content-Type: text/plain
				s-govuk-tax-6ad94337-3e6a-4260-baa3-b8a1d7eb8356
				------------
				Content-Disposition: form-data; name = x-amz-meta-original-filename; filename = file
				Content-Type: text/plain
				${filename}
				------------
				Content-Disposition: form-data; name = x-amz-algorithm; filename = file
				Content-Type: text/plain
				AWS4-HMAC-SHA256
				------------
				Content-Disposition: form-data; name = key; filename = file
				Content-Type: text/plain
				80880f84-a334-4b4a-98e5-2d351d2beae1
				------------
				Content-Disposition: form-data; name = acl; filename = file
				Content-Type: text/plain
				private
				------------
				Content-Disposition: form-data; name = x-amz-signature; filename = file
				Content-Type: text/plain
				98ee51d67f92c5584c5ec60a68e3267d651f99206375327abae0d319120a2e9e
				------------
				Content-Disposition: form-data; name = Content-Type; filename = file
				Content-Type: text/plain
				image/jpeg
				------------
				Content-Disposition: form-data; name = x-amz-meta-session-id; filename = file
				Content-Type: text/plain
				n/a
				------------
				Content-Disposition: form-data; name = x-amz-meta-consuming-service; filename = file
				Content-Type: text/plain
				business-rates-attachments
				------------
				Content-Disposition: form-data; name = policy; filename = file
				Content-Type: text/plain
				eyJleHBpcmF0aW9uIjoiMjAxOC0wNy0yNVQxNToyNzoxNC4yMjVaIiwiY29uZGl0aW9ucyI6W3siYnVja2V0IjoiZnVzLWluYm91bmQtODI2NGVlNTJmNTg5ZjRjMDE5MWFhOTRmODdhYTFhZWIifSx7ImFjbCI6InByaXZhdGUifSx7IngtYW16LWNyZWRlbnRpYWwiOiJBS0lBSUZHM1BSQkhTQVZWN0JXQS8yMDE4MDcxOC9ldS13ZXN0LTIvczMvYXdzNF9yZXF1ZXN0In0seyJ4LWFtei1hbGdvcml0aG0iOiJBV1M0LUhNQUMtU0hBMjU2In0seyJrZXkiOiI4MDg4MGY4NC1hMzM0LTRiNGEtOThlNS0yZDM1MWQyYmVhZTEifSx7IngtYW16LWRhdGUiOiIyMDE4MDcxOFQxNTI3MTRaIn0sWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMSwxMDQ4NTc2MF0seyJ4LWFtei1tZXRhLWNhbGxiYWNrLXVybCI6Imh0dHBzOi8vYnVzaW5lc3MtcmF0ZXMtYXR0YWNobWVudHMucHJvdGVjdGVkLm1kdHAvYnVzaW5lc3MtcmF0ZXMtYXR0YWNobWVudHMvY2FsbGJhY2sifSx7IngtYW16LW1ldGEtY29uc3VtaW5nLXNlcnZpY2UiOiJidXNpbmVzcy1yYXRlcy1hdHRhY2htZW50cyJ9LHsieC1hbXotbWV0YS1zZXNzaW9uLWlkIjoibi9hIn0seyJ4LWFtei1tZXRhLXJlcXVlc3QtaWQiOiJzLWdvdnVrLXRheC02YWQ5NDMzNy0zZTZhLTQyNjAtYmFhMy1iOGExZDdlYjgzNTYifSxbInN0YXJ0cy13aXRoIiwiJHgtYW16LW1ldGEtb3JpZ2luYWwtZmlsZW5hbWUiLCIiXSx7IkNvbnRlbnQtVHlwZSI6ImltYWdlL2pwZWcifV19
				------------
				Content-Disposition: form-data; name = file; filename = testJpg.jpg
				Content-Type: application/octet-stream
				src/test/resources/testJpg.jpg
</pre>


<p>Or, the POST request could be made via a web form:</p>
<pre>
&lt;form method=&quot;POST&quot; href=&quot;...value of the href from the response above...&quot;&gt;
    &lt;input type=&quot;hidden&quot; name=&quot;x-amz-algorithm&quot; value=&quot;AWS4-HMAC-SHA256&quot;&gt;
    ... all the fields returned in &quot;fields&quot; map in the response above ...
    &lt;input type=&quot;file&quot; name=&quot;file&quot;/&gt; &lt;- form field representing the file to upload
    &lt;input type=&quot;submit&quot; value=&quot;OK&quot;/&gt;
&lt;/form&gt;
</pre>

<p class="bold">Notes</p>

<p>The response to your file upload will be a HTTP 303 redirect to one of the URLs you provided in Step 1.</p>
<p>If the upload was successful, you will be redirected to the <code>successRedirect</code> URL.</p>  
<p>If the upload failed, you will be redirected to the <code>errorRedirect</code> URL. Details about the nature of the error will be appended to the <code>errorRedirect</code> URL in the form of query parameters. (These additional parameters will be: <code>RequestId</code>, <code>Resource</code>, <code>Message</code>, and <code>Code</code>.)</p>  

<p>Whichever way the form is sent:</p>
<ul class="list-bullet font-xsmall">
  <li class="font-xsmall">Use multipart encoding (multipart/form-data) NOT application/x-www-form-urlencoded. 
  If you use application/x-www-form-urlencoded, 
  AWS will return a response from which this error is not clear.</li>
  <li class="font-xsmall">The 'file' field must be the last field in the submitted form.</li>
</ul>

<p>File names must not be blank and must not include certain forbidden characters, such as em-dashes, asterisks, question marks, colons, etc (for actual regex refer to the published JSON schema of the request)</p>

<p>Currently, the only MIME types supported are:</p>
<ul class="list-bullet">
    <li class="font-xsmall">application/msword</li>
    <li class="font-xsmall">application/vnd.openxmlformats-officedocument.wordprocessingml.document</li>
    <li class="font-xsmall">application/vnd.ms-excel</li>
    <li class="font-xsmall">application/vnd.ms-excel.sheet.binary.macroEnabled.12</li>
    <li class="font-xsmall">application/vnd.openxmlformats-officedocument.spreadsheetml.sheet</li>
    <li class="font-xsmall">application/vnd.oasis.opendocument.formula</li>
    <li class="font-xsmall">application/pdf</li>
    <li class="font-xsmall">image/jpeg</li>
</ul>

<p>The following file size restrictions are in place:</p>
<ul class="list-bullet">
  <li class="font-xsmall">Minimum file size: 100KB</li>
  <li class="font-xsmall">Maximum file size: 10MB</li>
</ul>
