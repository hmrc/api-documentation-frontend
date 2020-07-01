This endpoint allows a user to retrieve information about the evidence file supporting a challenge case.

You will need to include the challenge submission ID of the challenge case as well as the attachment reference in the URL of the GET request.

The attachment reference was included in the response from a previous POST to `/my-organisation/challenge-cases/{challengeSubmissionId}/evidence`   

Note that:

<ul class="list-bullet">
    <li class="font-xsmall">
        Individuals or organisations can only access their own data, and must be enrolled for the <a href="https://www.gov.uk/correct-your-business-rates" target="_blank">Find and check your business rates valuation</a>  online service.
    </li>
</ul>