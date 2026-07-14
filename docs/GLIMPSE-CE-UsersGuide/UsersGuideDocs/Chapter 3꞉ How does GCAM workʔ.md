# Chapter 3꞉ How does GCAM workʔ

<details open><summary>Sections</summary><br>

[3.1 General solution process](#31-general-solution-process)

[3.2 Markets in GCAM](#32-markets-in-gcam)

[3.3 Determining market share](#33-determining-market-share)

[3.4 Calibration](#34-calibration)

[3.5 Relaxing shareweights over time](#35-relaxing-shareweights-over-time)

[3.6 Illustrating the operation of the "Car" market](#36-illustrating-the-operation-of-the-car-market)

[3.7 Markets, Logits, Shareweights throughout GCAM](#37-markets-logits-shareweights-throughout-gcam)

[3.8 The logit function and market share constraints](#38-the-logit-function-and-market-share-constraints)


</details>

## 3.1 General solution process
GCAM is a dynamic-recursive, market-based simulation model. This description refers to the process by which GCAM steps through time. In each time period, GCAM solves for the vector of prices at which quantities supplied equals quantities demanded for all modeled markets. When all markets have been solved, GCAM transitions to the next time period, starting with the solution from the prior period. 

## 3.2 Markets in GCAM
To illustrate how markets work in GCAM, we will focus on one sector, passenger travel demand. The figure below illustrates the hierarchical structure of this sector. Horizontal boxes represent various subsectors and modes, while the vertical boxes represent technologies. 

<img src='..\UsersGuideGraphics\C3-1.png'/><br>
**Figure 3.1 Hierarchical representation of passenger travel in GCAM. Passenger travel is subdivided into various subsectors, modes, and technologies. Options at each level compete for market share.**

The quantity of passenger travel demand, represented in units of passenger-km, is a function of the size of the population and the cost of travel. As population increases, the quantity of demand for passenger travel increases. However, if travel costs increase, demand for passenger travel per person decreases; and, if travel costs decrease, per capita demand increases. 

The "market" for passenger travel is apportioned across a range of modes, including "Walking", "Cycling", "Road", "Domestic Air", "International Air", "High Speed Rail (HSR)", and "Passenger Rail". The relative costs of these options influence the mixture used to fulfill overall passenger travel demand; decreasing the travel costs in any of these modes will increase its share compared to competing modes. 

The "Road" mode is subdivided further, with "Bus" competing with "Light-duty". Within "Light-duty", "2&3W" (2 or 3-wheeled vehicles, such as motorcycles) compete with "4W" (passenger vehicles with 4 wheels, such as cars, SUVs, and trucks). For convenience, "4W" vehicles are lumped into the categories "Car" and "Large Car and Truck". 

At the bottom of the hierarchy are the technologies that are competing against each other. For example, for the "Car" category, "Liquids" (conventional internal combustion engines) are competing with "Hybrid", "CNG", "Fuel Cell", and "EV". This is the most granular level in GCAM's market-based structure. The mix of technologies selected at this level determines the levelized cost ($/passenger-km) of meeting travel demand by "Car". How this cost compares to that of "Large Car and Truck" determines how "4W" demand is apportioned as well as the overall cost of "4W". The relative costs of "4W" and "2&3W" determines apportionment of "Light-duty" across those two categories. In this manner, costs cascade up the hierarchy, determining how passenger travel demand is allocated to each branch of the tree. 

## 3.3 Determining market share
In determining the relative market share of new purchases of competing technologies, GCAM uses a logit function. For example, the share of new "Car" purchases assigned to technology i is calculated using the modified logit formula: 

<img src='..\UsersGuideGraphics\C3-2.png'/>

where: *N* is the total number of technologies competing for market share; *s~i~* is the market share of technology *i*; *p~i~* and *p~j~* are the prices of technologies *i* and *j*, respectively; *α~i~* and *α~j~* are the shareweights of technologies *i* and *j*; and *&gamma;* is the logit exponent. 

The price of each technology is its levelized cost, which is determined from the capital, operation and maintenance, and fuel costs and represented in units of $/pass-km. 

The technology-specific shareweight is a means of representing non-modeled issues, such as logistical barriers for the adoption of a technology or bias against a new technology. By convention, a value of 1 indicates that the technology is perfectly competitive, meaning there is no bias and only relative costs come into play when determining the technology's market share. In contrast, a value of 0 indicates that the technology will not be purchased regardless of cost. 

If a technology is assumed to be purchased at a lower rate than its cost would suggest, then its shareweight typically is a value between 0 and 1. For example, in the "Car" category, a 0.2 shareweight for the "Fuel Cell" technology could be used to reflect implicitly how the limited availability of hydrogen would dampen market share. Similarly, a reduced shareweight for "EV" could be used to reflect factors such as limited charging network and range anxiety. 

The logit exponent controls the degree to which cost differences among technologies determine their relative market shares. A value of 0 means that the logit function will ignore cost differences. Assuming every technology has the same shareweight, they would be assigned equal shares. Typical logit exponent values are -4, -6, and -8. The more negative the value, the greater the weight that is given to cost differences. For the "Car" market, the logit exponent is   -8, indicating that consumer decisions on which technology to purchase are highly influenced by cost. In contrast, the logit exponent that is used to allocate passenger travel between "2&3W" and "4W" is -4, indicating that this market is less sensitive to price. 

## 3.4 Calibration 
The shareweights for most technologies in GCAM are estimated during the calibration process. For GCAM 9.1, the calibration years are 2015 and 2021. In the calibration years, GCAM starts with estimates of real-world market share for each technology. The technology with the greatest market share in the calibration year is assigned a shareweight of 1. Thus, in the "Car" market, "Liquids" would have a shareweight of 1. 

Since the market shares and costs for competing technologies (e.g., "Hybrid", "CNG", …) are known for the calibration year, and since a logit exponent has been assumed, GCAM can solve for the shareweights of each technology. 

For technologies that did not exist in a calibration year, shareweights are set to 0. In the "Car" market in 2015, the calculated "Hybrid" shareweight would be near 0, and the "Fuel Cell" and "EV" shareweight would effectively be 0. 

Note that the calculation of shareweight values is based upon overall stock of each technology in the market in the calibration year, not sales in that year. As a result, the shareweights can be biased toward historic factors as opposed to any new technology or developments that may have impacted more recently. 

## 3.5 Relaxing shareweights over time
To allow more flexibility over time, the shareweights for many technologies are set to follow a linear path from their calibrated value to 1 over time, implying that logistical barriers and biases in the calibration year would diminish over time. In many markets, technology shareweights are set to 1 in 2050, although for some technologies 2100 is used to indicate a very long technology development timeline is assumed until all barriers have been addressed. These decisions have been made by the GCAM developers using modelers' judgement but can be overridden by GCAM users. 

The best trajectory to use for a technology's shareweight may be technology- and scenario-dependent. For "Hybrid" cars, for example, there is no range anxiety or fueling infrastructure barriers. When introduced, there was some hesitancy to adopt hybrid vehicles because they represented a new technology. Arguably, this barrier is greatly diminished or no longer exists. It would be reasonable to have the "Hybrid" shareweight transition to 1 by 2025. In GLIMPSE, we use "2025", "2035", and "2040" as the years at which "Hybrid", "EV", and "Fuel Cell" technologies, respectively, achieve a shareweight of 1 for the "Car" and "Large Car and Truck" categories.

Shareweight trajectories may also be modified to reflect a particular scenario. For example, in a scenario involving deep decarbonization, there could be additional public investment in charging infrastructure and research and development in improving battery cost and range. In such a scenario, it could be reasonable to adjust the shareweight trajectory for EVs to reach 1 sooner. 


## 3.6 Illustrating the operation of the "Car" market
Next, we provide simplified description of the "Car" market to illustrate how the logit factors into technology choice in GCAM. 

The "Car" market in GCAM is depicted in the figure below. In the calibration year, 2021, "Car" travel demand is met by the initial stock of vehicles. This stock is assumed to retire over time, with that retirement curve represented by the red line and defined by the lifetime (e.g., 30 years), half-life (e.g., 7 years), and slope. Thus, the travel demand that is met by the initial stock declines over time while the overall demand for "Car" travel increases. 

<img src='..\UsersGuideGraphics\C3-3.png'/><br>
**Figure 3.2 Initial vehicle stock, retirement, and demand. The initial stock retires following an s-curve that is defined by its half-life, slope, and vehicle lifetime.**

At each modeled time period (typically 5 years), GCAM must purchase "Car" capacity to bridge the gap between demand and what can be met by the "Car" fleet remaining from the previous period. 

<img src='..\UsersGuideGraphics\C3-4.png'/><br>
**Figure 3.3 Vintaging and demand for new vehicles. Each vintage retires following an s-curve. The difference between demand and remaining stock is made up through the purchase of new stock in each modeled year.**

The logit function helps determine the technology composition and cost of "Car" travel for each vintage. 
To do this, GCAM takes an iterative approach to determine the "Cost" of car travel. An initial guess is made. At this price, GCAM estimates the amount of demand that would be allocated to "Car" in the passenger transportation hierarchy, as well as the supply of vehicles that would be brought to market at that price. This supply is informed by the logit function. 

In the illustration below, the guess for the initial price is low, resulting in demand for "Car" outstripping supply. 

<img src='..\UsersGuideGraphics\C3-5.png'/><br>
**Figure 3.4 An initial guess for a market-clearing price. At the initial guess, supply and demand values are determined.**

Next, a second guess is made. At this higher price, GCAM finds that supply would be greater than demand. 

<img src='..\UsersGuideGraphics\C3-6.png'/><br>
**Figure 3.5 A second guess for a market-clearing price. For the new price, quantities of supply and demand are again estimated.**

The guessing process continues via the bisection method until the difference between supply and demand is within a specified tolerance, such as 0.001. Once this criterion is met, the market is considered to have been solved.

<img src='..\UsersGuideGraphics\C3-7.png'/><br>
**Figure 3.6 Identification of a market clearing price that solves this market. Guessing continues until the difference between supply and demand is below the solution tolerance, indicating that the market is cleared.**

We can visualize how the logit apportions market share to each technology if we think of technology costs as distributions. 

<img src='..\UsersGuideGraphics\C3-8.png'/><br>
**Figure 3.7 Conceptual diagram indicating how the logit allocates market share. The price at which each technology can be brought to market is represented with a mean value and with a distribution.**

For example, at our initial guess for price, the composition of sales is determined based on the relative areas under the curves. In this illustrative example, at the initial price, 90% of sales are apportioned to "Liquids", 10% to "Hybrids". Because sales are represented as distributions, the amount assigned to "EV" would be non-0, but very small. 

The solution algorithm continues to search for a market clearing price, and, in this example, would result in sales shares of 50% for "Liquids", 38% for "Hybrid", and 12% for "EV". 

<img src='..\UsersGuideGraphics\C3-9.png'/><br>
**Figure 3.8 Market shares of sales at the market-clearing price. Shares are determined based upon the relative areas under the three distributions.**

This visualization also allows us to depict how reducing a technology's shareweight would impact apportionment of market share. For example, in the next figure, we illustrate market shares in which the shareweight for "EV" has been reduced. 

<img src='..\UsersGuideGraphics\C3-10.png'/><br>
**Figure 3.9 Impact of a reduced shareweight for EVs on market share allocation. The market share for EVs is reduced as a result of its lower shareweight.**

A policy such as a carbon tax directly influences market share decisions. By increasing the operating costs for fossil fuels, such a policy would modify the levelized costs of each technology. This would shift the distributions and result in a change in the sales share apportioned to each technology. 

<img src='..\UsersGuideGraphics\C3-11.png'/><br>
**Figure 3.10 Impact of a price on carbon on market share allocation. The distributions shift along the x-axis, reflecting changes in the costs associated with each technology that result from the price of carbon.**

The costs for technologies can also be shifted higher or lower based on user-specified technology-specific taxes or subsidies. A tax or subsidy would directly affect the technology allocations in the logit. Alternatively, GCAM offers the option of specifying a market share constraint, and GCAM then solves to determine the subsidy necessary to achieve this share.

For example, in this illustration, GCAM solves for the subsidy that would shift the EV cost distribution lower such that a 45% market share is achieved. 

<img src='..\UsersGuideGraphics\C3-12.png'/><br>
**Figure 3.11 Impact of a technology subsidy on market share allocation. The subsidy shifts the distribution of the subsidized technology, increasing its market share allocation.**

## 3.7 Markets, Logits, Shareweights throughout GCAM
While this example has focused on the "Car" market, the logit functions are used throughout GCAM in the solution of thousands of markets in every modeled time period. In passenger transportation, logit functions are used to apportion market share within each horizontal box in the figure below. 

<img src='..\UsersGuideGraphics\C3-13.png'/><br>
**Figure 3.12 Shareweights and logit exponents across the passenger transportation sector. Each sector, subsector, and technology has a shareweight, and each market is assigned a logit exponent.**

These mechanisms govern shares in other components of the energy system, as well as in GCAM's representation of choice in allocating water and land use. 

<img src='..\UsersGuideGraphics\C3-14.png'/><br>
**Figure 3.13 Shareweights are present across GCAM's systems and sectors. The logit determines market share allocations throughout GCAM.**

## 3.8 The logit function and market share constraints
The logit-based estimation process will attempt to assign at least some market shares to each technology option, with some exceptions (e.g., technologies with integrated carbon capture and storage are not available in scenarios where there is no price on carbon). As a result, it may be difficult for GCAM to achieve market share targets in which a technology or set of technologies achieves 100% (or near 100%) market share. Users can implement high market share targets by complementing the policy with low or zero shareweights for competing technologies.  

For example, California has recently specified a target that 100% of light duty vehicle sales be electric by 2035. One way to implement this policy would be via the following combination: 
-	Using a market share constraint to increase the EV sales share to 33% in 2025, 66% in 2030, and 100% in 2035
-	Setting the shareweights for non-EVs to 0 from 2035 through the final model year  

Modifying technology and sectoral shareweights is a complicated process in GCAM-USA. Shareweights can be specified for specific years and regions. However, these values are overridden if an interpolation rule is in place, which can result in unexpected behavior. To assist with this challenge, GLIMPSE's "Tech Param" tab allows the user to specify new sector- or technology-specific shareweights. When creating the resulting policy file, GLIMPSE attempts to automatically delete relevant interpolation rules. Users are encouraged to view the affected technology's shareweights via the ModelInterface to verify that the values used within the model reflect what was expected.

A more detailed description of where shareweight rules are specified and how they are interpreted will be included in future versions of this Users' Guide.

