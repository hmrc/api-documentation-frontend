<p>This endpoint is designed for loading lookup values (reference data).</p>

<p>A lookup consists of a sequence of values. 
A lookup value is essentially a code-and-description pair and optionally a sort-order value. 
Where present, the sort-order provides the correct ordering of the lookup values.</p>
<p>There's a number of lookups of different types available in the system. 
This endpoint enables retrieval of the full set of lookup values of a provided lookup type.</p>

<strong>Lookup types grouped by area of use</strong>

Valuation of shop/warehouse/office/land/misc

 - `basisOfMeasurement`
 - `useCode`
 - `floor`
 - `year`
 - `adjCode`
 - `oaCode`
 - `otherAdditionsDfe`

Valuation of licensed premises

 - `lpAgeDfe`
 - `lpClass`
 - `lpExtension`
 - `lpGrade`
 - `lpLocation`
 - `lpType`


Check case submission

 - `codeOfGroundsDfe`

Challenge case submission

 - `challengeCodeOfGroundsDfe`
