This endpoint returns a paginated list of properties which match the search criteria.

A typical use case would be to search for a property via this endpoint then use the HATEOAS links included in the results to view the summary valuation.

You must include at least one search query parameter to make a valid request:

 - postcode
 - billingAuthorityReference
 
The property search endpoint does not support pagination the same way as all the other paginated endpoints. After requesting a page of properties, you can query for the next page of results by specifying a combination of <code>afterAddress</code> and <code>afterUarn</code> - these will be the full address and UARN of the last property in your current page of search results. This is as if you were saying: "I've seen all properties up to here, give me what comes next."

The <code>afterUarn</code> parameter must only be used in addition to <code>afterAddress</code>. It's optional, but it's best to always include it, as some addresses might encompass multiple UARNs.
