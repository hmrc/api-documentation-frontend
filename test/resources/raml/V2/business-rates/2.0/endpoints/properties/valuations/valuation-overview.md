This endpoint returns both the summary valuation and the detailed valuation when projection is defined for the given property.  

The summary valuation is a reduced version of the valuation which is available to everyone.  The full valuation is available via the detailed valuation endpoint but requires you to link the property to your organisation.

The VOA gives a [rateable value](https://www.gov.uk/guidance/how-non-domestic-property-including-plant-and-machinery-is-valued#rateable-value) or valuation to each non-domestic property and this is used by local councils to calculate a property’s business rates.

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

**Licensed Properties are split into two types:**
  - Pubs
  - Hotels and Guesthouses
