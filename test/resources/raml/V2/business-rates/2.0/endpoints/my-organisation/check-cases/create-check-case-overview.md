<p>This endpoint allows a user to perform a ‘Check’ on a property owned by
their organisation.</p>

<p>Once you requested has been accepted there will a slight delay in processing the request (approximately a minute).</p>
<p>To confirm your request has completed you can poll Get Singular Check Case. to see if you can move onto the next step.</p>
<p>We recommend you don’t poll until a minute after the initial request was submitted.</p>
<p>Once you have confirmation your request has been processed you can then move onto uploading your evidence.</p>

<p>The ‘Check’ is a review by the IP of the information 
held by the VOA for their property. 
The IP confirms the accuracy of the facts on which 
the rating list entry is based, provides missing factual information 
and amends property details as necessary.</p>

<p>Note that:</p>

<p>To perform a ‘Check’ the property must have been claimed by the 
IP and the claim approved by the VOA.</p>

### Detailed Valuations and the ability to perform a digital Check via APIs

There are two distinct types of Valuation currently available via the APIs

- Properties with a 'Bulk Class' valuation classified as a Bulk Class (with 2 Survey Units or less) 
- Properties with a 'Licensed Properties' valuation based on Fair Maintainable Turnover

**Bulk Class properties are further broken down into:**
  - Shops (Bulk Class 'S')
  - Offices (Bulk Class 'O')
  - Factories (Bulk Class 'F')
  - Warehouses (Bulk Class 'W')
  - Land (Bulk Class 'L')
  - Miscellaneous Category 3 (Bulk Class 'M') - Garage, Car Showroom and others
    - Primary descriptions - CG2, CG3, CG4, CM1, CS7      
    - Primary descriptions - CX with a special category code of 413
  - Miscellaneous Category 4 (Bulk Class 'M') - Miscellaneous
    - Primary descriptions - CH, CH3, CL, CL1, CL2, CR, CR1, EL, EL1, EM, EM1, EN1, LI, LS, LT1, LT3, LT4, MP1, MR, MR1, CW2, IF4, LC, NX, IX
  - Miscellaneous Category 5 (Bulk Class 'M') - Holiday Accommodation and others
    - Primary descriptions - CH1, CH2, CG, CG1, CM
  - Miscellaneous Category 7 (Bulk Class 'M') - Car Parks
    - Primary descriptions - CP, CP1
  - Miscellaneous Category 11 (Bulk Class 'M') - Ad Rights, Beach Huts and others
    - Primary descriptions CA, CA1, CW1, LH1, LS2, LS3, MT2, NT1, TD2
    - Primary description of 'CX' and a special category code of either 018, 045, 146, 211, 419, 427 or 992
  - Miscellaneous Category 15 (Bulk Class 'M') - Club House
    - Primary descriptions - LC1
  - Miscellaneous Category 16 (Bulk Class 'M') - Leisure Centre/Hall
    - Primary descriptions - LC2, LC3, MH, MH1, NT3
  - Miscellaneous Category 18 (Bulk Class 'M') - Sports Grounds
    - Primary descriptions - LS4, LS5, LS6, LS7, MC, MC1
  - Car Parking
    - Bulk properties with Car Parking should always replay existing values in the submission if they remain unchanged. If they are omitted it will be seen as a request to remove the parking values.

**Licensed Properties are split into two types:**
  - Pubs
  - Hotels and Guesthouses


#### Hardship

You can inform the VOA that you are experiencing financial hardship. In order to do so, you may submit a 'experiencingHardship' flag in conjunction with a list of reliefs that you are currently receiving. The recognised reliefs are:

 - Small business rate relief (enum value SMALL_BUSINESS_RATE_RELIEF) 
 - Rural rate relief (enum value RURAL_RATE_RELIEF)
 - Business rates holiday for children's day nursery (enum value BUS_RATE_HOL_CHILD_DAY_NURSERY)
 - Expanded retail discount (enum value EXPANDED_RETAIL_DISCOUNT)
 - Retail, Hospitality and Leisure grant (enum value RETAIL_HOSPITALITY_LEISURE_GRANT)
 - COVID-19 business support grant (enum value COVID-19_BUSINESS_SUPPORT_GRANT)
 - Payments deferred (enum value PAYMENTS_DEFERRED)  
