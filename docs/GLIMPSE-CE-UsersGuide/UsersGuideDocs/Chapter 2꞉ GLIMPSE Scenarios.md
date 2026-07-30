# Chapter 2꞉ GLIMPSE Reference Scenario (DRAFT)


## 2.1 Introduction

There are many pathways by which the U.S. energy system could evolve over the coming decades. GCAM-USA can be used to construct internally consistent scenarios that describe potential pathways. Users are also able to construct their own scenarios, introducing alternative assumptions about inputs such as population growth and migration, economic growth and transformation, technology change and adoption, climate change, human behavior and choice, and policies, including those targeting climate, environmental, or energy endpoints. The model’s projections reflect the underlying assumptions about the present and future, as well as the formulation of the GCAM-USA itself, including the aspects of the system the developers chose to include, how various phenomena are represented, and how those formulations are parameterized. Thus, understanding the underlying assumptions, formulation, and parameterization is important in interpreting the results. We encourage GCAM-USA users to become familiar with the [GCAM documentation](https://jgcri.github.io/gcam-doc/toc.html), and, in particular, the [section on economic choice](https://jgcri.github.io/gcam-doc/choice.html).

We include three scenarios with our GLIMPSE distribution, a reference scenario and two net-zero scenario, including one with (NZ-GCAM-USA-9.1) and one without direct air capture (DAC) technologies (NZ-GCAM-USA-9.1-DAC). The reference scenario (*Ref-GCAM-USA-9.1* in the *Scenario Library*) is based upon the reference case that is included by PNNL with GCAM-USA 9.1. We include several modifications, including: 

* calibrating state-level sales of onroad passenger light duty electric vehicles and hybrid vehicles to US sales estimates
* updating onroad vehicle emissions data to incorporate emission factors missing in the 9.1 release 

The primary goal of this chapter is to describe the reference scenario *Ref-GCAM-USA-9.1* and the ways in which the net-zero scenario *NZ-GCAM-USA-9.1* differs. We provide national-level results to convey fuel, technology, and emission trends, overall and for key energy sectors.

## 2.2 The GLIMPSE/GCAM-USA 9.1 Reference Scenario, *Ref-GCAM-USA-9.1*

### 2.2.1 Overview

Broadly, *Ref-GCAM-USA-9.1* is constructed with an underlying storyline that assumes that historical trends continue in the near term (e.g., through 2035). However, over the longer term (e.g., 2040-2050), parameters that are used to calibrate to historic trends are relaxed, and outcomes are increasingly driven by economic forces and , where applicable, by policies. 

## 2.3 The GLIMPSE Net Zero Scenario, *NZ-GCAM-USA-9.1*

> <span style='color:red'>The GLIMPSE Net Zero Scenario, NZ-GCAM-USA-9.1, is built upon the GCAM-USA 8.2 Reference Scenario that was distributed by PNNL with GCAM-USA 8.2 but includes adjustments to … </span>

> *More to come soon.*

## 2.4 Selected results for the GLIMPSE Reference Scenario

Below, national-level GLIMPSE Reference Scenario results are presented for several key outputs. The results are divided into three sections to reflect the three portions of the figure below: (1) primary energy, (2) processing and conversion of energy carriers, and (3) final energy. In addition, we provide results indicating the market shares for various technologies in selected end-use sectors, as well as emissions of CO2 and air pollutants.

<img src='..\UsersGuideGraphics\C2-1.png' /><br>
**Figure 2.1 Energy system schematic. The energy system extends from the import or extraction of primary energy, through its processing and conversion into useful forms, through its use in meeting final end-use energy demands.**

To obtain national totals, all states, DC, and the USA region were selected. The USA region includes a few sectors that have not yet been disaggregated to the state level, such as agriculture, hydrogen production, and oil, natural gas, and coal operations. 

All data were extracted from the output database using queries available in the “GLIMPSE” section of the *ModelInterface* query list. Except for the emissions graphics, all images shown were generated using the *ModelInterface*’s graphing tools.

### 2.4.1 Primary energy

Fig. 2.2 shows the *Ref-GCAM-USA-9.1* projection of the consumption of primary energy (e.g., in its “raw” form) through 2050. Renewables are shown as direct equivalent, indicating that the quantity shown is the energy produced from wind, solar, geothermal, and hydro.

Overall, primary energy consumption is stable from 2021 on. Coal and nuclear decrease over time, while solar, wind, and biomass increase. 
> <span style='color:red'>\[More comments on what is shown in the figure. Original: <br><br> Imported oil decreases over time but imported natural gas increases. The reduction in primary energy use in 2021 reflects factors such as improved vehicle efficiencies and the transition from coal-powered electricity production to higher efficiency gas combined-cycle turbines. The impacts of the COVID-19 pandemic are not included in this version of the model.\]</span>

<img src='..\UsersGuideGraphics\C2-2.png' /><br>
**Figure 2.2 Reference Scenario primary energy consumption by region (direct equivalent)**

### 2.4.2 Processing and conversion of energy carriers

In this section, we examine electricity production, refining, and hydrogen production by aggregated technology category. 

#### Electricity production

Figure 2.3 shows *Ref-GCAM-USA-9.1* electricity generation by aggregate subsector. Categories are fuel-based, but differentiate between onshore and offshore wind, as well as by type of solar power. PV stands for photovoltaic technologies, while CSP indicates concentrated solar power technologies. National electricity production grows by more than 50% from 2021 to 2050. Generation from coal and nuclear power diminish over the modeling horizon as existing plants retire and are replaced with increased generation from wind, solar, and natural gas. By 2050, renewables constitute approximately 50% of generation. This result is impacted by the use of updated electric sector technology costs that reflect reduced capital costs for wind and solar.

<img src='..\UsersGuideGraphics\C2-3.png' /><br>
**Figure 2.3 Reference Scenario electricity generation by aggregate subsector**

#### Refined liquids production

In GCAM and GCAM-USA, liquid fuel production technologies include those shown in Fig 2.4. The fuels that they produce are then lumped together and referred to as “refined liquids”. Refined liquids can be used by any sector. FT biofuels are those produced by the Fischer-Tropsch process. In *Ref-GCAM-USA-9.1*, domestic liquid fuel production decreases by 17% from 2021 to 2050, driven by vehicle efficiency improvements, fuel switching, and electrification of the transportation sector. The primary source of liquid fuels is oil refineries. Corn ethanol production is small in comparison and is steady over the modeling horizon until 2040, when it begins to decrease. Gas-to-liquid, cellulosic ethanol, BTL with hydrogen, and FT biofuels have small production shares.

<img src='..\UsersGuideGraphics\C2-4.png' /><br>
**Figure 2.4 Reference Scenario liquid fuel production by technology**

#### Hydrogen production

Technologies in *Ref-GCAM-USA-9.1* that produce H2 include natural gas steam reforming, electrolysis, and biomass technologies. Initially, the primary technology for H2 production is natural gas steam reforming, but from 2045 onwards the majority of H2 is produced by electrolysis.

<img src='..\UsersGuideGraphics\C2-5.png' /><br>
**Figure 2.5 Reference Scenario hydrogen production by technology**

### 2.4.3 Final energy

In this section, we show the total final energy consumption by sector over time, independent of fuel. We then examine this information further by inspecting the use of specific fuels by sector.

#### Total energy consumption by sector

Final energy is the energy used in various sectors to meet end-use demands, such as space heating, water heating, lighting, and transportation. Here, transport-LDV refers to passenger cars and trucks. Transport-HDV refers to trucks used for delivering freight, and these vehicles are further categorized as being small, medium, or large. Transport-ALM refers to the combination of air, locomotive (rail), and marine vehicles. In 2021, industry, buildings (the residential and commercial sectors), and transportation are each responsible for approximately one-third of final energy use. The industrial final energy usage grows, while most other sectors stay relatively constant. Energy use by transport-LDV decreases as those vehicles become more efficient. Fuel use in transport-HDV and in transport-ALM is steady over time. Overall, final energy is relatively constant through 2035. From 2035, however, the final energy consumption overtakes efficiency improvements, resulting in steady growth in consumption.

<img src='..\UsersGuideGraphics\C2-6.png' /><br>
**Figure 2.6 Reference Scenario final energy consumption by sector**

#### Electricity use by sector

The residential and commercial sectors are responsible for more than two-thirds of use electricity use in 2021. By 2050, this share has decreased as industry and transport grow in electricity use over the time horizon. Fuel production refers to fuel extraction and refining activities, which use very little electricity.

<img src='..\UsersGuideGraphics\C2-7.png' /><br>
**Figure 2.7 Reference Scenario electricity use by sector**

#### Coal use by sector

Coal use declines significantly over the modeling horizon. The largest decreases are prior to 2025, when coal loses market share to natural gas and, to a lesser extent, wind. Post 2025, coal decline is driven by retirements. Industrial coal use is steady over time, although there is a slight increase. Only a negligible amount of coal is used in the buildings sectors. Exported coal is not shown.

<img src='..\UsersGuideGraphics\C2-8.png' /><br>
**Figure 2.8 Reference Scenario coal use by aggregate sector**

#### Natural gas use by sector

Natural gas is used across many sectors of the economy. Use in electricity production grows steadily from 2021 to 2050. Use in industry, transportation, and fuel production increases over time as well. In contrast, commercial natural gas use is steady, while residential natural gas use decreases as more residential end-use services (e.g., space and water heating) are met with electricity.

<img src='..\UsersGuideGraphics\C2-9.png' /><br>
**Figure 2.9 Reference Scenario natural gas use by aggregate sector**

#### Refined liquids use by sector

GCAM does not differentiate whether the liquid fuels are gasoline, diesel, or biofuels. The transportation sector used more than three-quarters of liquid fuels in 2021. Use in light duty transportation declines over time as vehicles become more efficient and alternative fueled vehicles achieve greater market share. Use of liquid fuels in industry grows slowly but steadily. Much of this industrial refined liquid use is in construction, agriculture, and mining.

<img src='..\UsersGuideGraphics\C2-10.png' /><br>
**Figure 2.10 Reference Scenario refined liquids use by aggregate sector**

#### Biomass use by sector

The industrial sector is the greatest user of biomass in the early years. However, use in liquid fuel production jumps in 2050 as advanced biofuel technologies become available in the model.

<img src='..\UsersGuideGraphics\C2-11.png' /><br>
**Figure 2.11 Reference Scenario biomass use by sector**

#### H2 use by sector

The transportation sector is the greatest user of H2, and more than three-quarters of total H2 use occurs in on-road transportation. 

<img src='..\UsersGuideGraphics\C2-12.png' /><br>
**Figure 2.12 Reference Scenario hydrogen use by sector**

Queries are also available for examining specific sectors and their fuel use, as is shown in the following graphics. 

#### Energy use in the industrial sector

Overall, industrial energy use increases approximately 15% from 2021 to 2050. Use of all fuels increases over that time horizon, but the increase is dominated by electricity (0.67 EJ), refined liquids (1.14 EJ), and natural gas (0.90 EJ).

<img src='..\UsersGuideGraphics\C2-13.png' /><br>
**Figure 2.13 Reference Scenario industrial sector energy use by fuel**

#### Energy use in residential and commercial buildings

Overall, energy use in buildings increases approximately 13% from 2021 to 2050. Of the fuels used in buildings, only electricity use increases over that time horizon.

<img src='..\UsersGuideGraphics\C2-14.png' /><br>
**Figure 2.14 Reference Scenario buildings energy use by fuel**

#### Energy use in the on-road light-duty transportation sector

The onroad light-duty vehicle sector includes the “Car” and “Large Car and Truck” passenger vehicle categories. Overall, light-duty vehicle energy use decreases by 26% from 2021 to 2050. Use of refined liquids decreases by about 43%. By 2050, electricity and hydrogen account for 21% of fuel use.

<img src='..\UsersGuideGraphics\C2-15.png' /><br>
**Figure 2.15 Reference Scenario light-duty transportation energy use by fuel**

#### Energy use in the on-road freight transportation sector

The onroad heavy-duty vehicle (HDV) sector includes the “Small”, “Medium”, and “Large” freight truck categories. HDV energy use hits a maximum in 2030 but overall changes very little from 2021 to 2050. Use of refined liquids decreases by about 18% during this time. By 2050, electricity and hydrogen account for 16% of fuel use.

<img src='..\UsersGuideGraphics\C2-16.png' /><br>
**Figure 2.16 Reference Scenario heavy-duty transportation energy use by fuel**

#### Energy use across air, locomotive, and marine transportation sectors

The “ALM” sector includes air, locomotive, and marine passenger and freight vehicles. Overall, energy use in this sector increases by 18% from 2021 to 2050. Use of refined liquids increases by 12%, or 0.6 EJ. By 2050, electricity and hydrogen account for approximately 6% of this combined sector’s fuel use.

<img src='..\UsersGuideGraphics\C2-17.png' /><br>
**Figure 2.17 Reference Scenario energy use of fuel for the transport-ALM sector**

### 2.4.4 Technology market shares for specific end-uses

In this section, we examine the market shares (in terms of service demands met) of competing technologies for a selection of end-use markets. Here, we focus on the commercial, residential, and transportation sectors. 
> <span style='color:red;'>\[Original: <br><br>In GCAM-USA 5.4, the industrial sector has limited technological detail, reflecting “industrial energy use” by fuel, as well as fuel used in the cement and fertilizer industries. We do not present additional results for the industrial sector, but GLIMPSE users are encouraged to use the “Industry final energy by tech and fuel” to explore industrial fuel use further. Future versions of GCAM-USA are expected to have additional detail in the industrial sector.\]</span>

#### Commercial space cooling

Overall, commercial space cooling demands increase by 41% from 2021 to 2050. The greatest increase is in high-efficiency air conditioning (0.58 EJ), output of which nearly triples from 2021 to 2050.

<img src='..\UsersGuideGraphics\C2-18.png' /><br>
**Figure 2.18 Reference Scenario service output for commercial space cooling technologies**

#### Commercial space heating

Overall, commercial space heating demands increase by 33% from 2021 to 2050. The greatest increases are from high-efficiency gas furnaces (0.76 EJ) and from electric heat pumps (0.38 EJ).

<img src='..\UsersGuideGraphics\C2-19.png' /><br>
**Figure 2.19 Reference Scenario service output for commercial space heating technologies**

#### Commercial water heating

Overall, commercial water heating demands increase by 43% from 2021 to 2050. The greatest increase is from high-efficiency natural gas water heaters (0.35 EJ).

<img src='..\UsersGuideGraphics\C2-20.png' /><br>
**Figure 2.20 Reference Scenario service output for commercial water heating technologies**

#### Commercial lighting

Solid state technologies include Light-Emitting Diodes (LED) lighting. Fluorescent includes compact (CFL) and linear (LFL) bulbs. Overall, commercial lighting demands increase by 51% from 2021 to 2050. While some market share remains through 2050 for fluorescent and incandescent lighting, the only category that grows over that period is solid state lighting (12 petalumens-hours).

<img src='..\UsersGuideGraphics\C2-21.png' /><br>
**Figure 2.21 Reference Scenario service output for commercial lighting technologies**

#### Residential space cooling

Overall, residential cooling demands increases by 48% from 2021 to 2050. The high-efficiency version of electric air conditioning achieves only a very small market share.

<img src='..\UsersGuideGraphics\C2-22.png' /><br>
**Figure 2.22 Reference Scenario service output for residential space cooling technologies**

#### Residential space heating

Overall, residential heating demands increase by 33% from 2021 to 2050. Output from electric heat pumps increases by 2.3 EJ. Heating from high-efficiency natural gas furnaces grows in output as well, but only by 0.3 EJ. Overall, heating provided by natural gas decreases by 28% from 2021 to 2050.

<img src='..\UsersGuideGraphics\C2-23.png' /><br>
**Figure 2.23 Reference Scenario service output for residential space heating technologies**

#### Residential water heating

Overall, residential water heating demand increases by 38% from 2021 to 2050. Much of the growth over time is met by high-efficiency electric water heaters. However, starting in 2035, output from electric heat pump water heaters begins to grow.

<img src='..\UsersGuideGraphics\C2-24.png' /><br>
**Figure 2.24 Reference Scenario service output for residential water heating technologies**

#### Residential lighting

Solid state technologies include Light-Emitting Diodes (LED) lighting. Fluorescent includes compact (CFL) and linear (LFL) bulbs. Overall, residential lighting demand increases by 51% from 2021 to 2050. Output from fluorescent bulbs is relatively constant over time. Incandescent bulbs have been significantly displaced by solid state lighting by 2030.

<img src='..\UsersGuideGraphics\C2-25.png' /><br>
**Figure 2.25 Reference Scenario service output for residential lighting technologies**

#### On-road light-duty technologies

Conventional vehicles that operate on gasoline and diesel dominate the market through 2025, from which hybrid and electric vehicle market share increases steadily. Hydrogen fuel cell vehicles begin to penetrate the market in 2030, achieving a market share (based on pass-km) of approximately 7% in 2050.

<img src='..\UsersGuideGraphics\C2-26.png' /><br>
**Figure 2.26 Reference Scenario service output for light-duty passenger vehicle technologies**

#### On-road freight truck technologies

This graphic includes on-road freight demand met by technology across three size categories: “Light” and “Medium” and “Heavy”. The load factors for these three sizes differ, and are 0.27, 2.07, and 4.16 tonnes per vehicle. Adoption of alternative fuel vehicles across these size classes will differ. The “Transport service output by tech” query can be used to explore market shares for each class separately. Conventional vehicles that operate on gasoline and diesel dominate the market in 2021. From 2025, however, hybrid vehicle market share grows, eventually representing more than a third of overall ton-km. Electric and fuel cells begin to appear in 2025. By 2050, they have reached 13% and 9% of overall on-road ton-km, respectively.

<img src='..\UsersGuideGraphics\C2-27.png' /><br>
**Figure 2.27 Reference Scenario service output for heavy-duty truck technologies**

#### Bus technologies

Overall demand for bus travel declines slowly. Alternative fuels play an increasing role in this sector. By 2050, hybrids, electric, and fuel cell buses each represent approximately 25% of the category’s service demand.

<img src='..\UsersGuideGraphics\C2-28.png' /><br>
**Figure 2.28 Reference Scenario service output by bus technologies**

#### Domestic aviation technologies

Overall demand for domestic aviation nearly doubles from 2021 to 2050. Refined liquid technologies dominate the market, although there is significant growth in hydrogen and electric plants, after 2035. Note that biofuels are not represented as a separate end-use fuel, but that biofuels represent a portion of refined liquids.

<img src='..\UsersGuideGraphics\C2-29.png' /><br>
**Figure 2.29 Reference Scenario service output by domestic aviation technologies**

#### International aviation technologies

Overall demand for international passenger aviation increases by 59% from 2021 to 2050. Hydrogen and electric planes do not achieve a meaningful share of the market by 2050.

<img src='..\UsersGuideGraphics\C2-30.png' /><br>
**Figure 2.30 Reference Scenario service output by international aviation technologies**

#### Freight rail technologies

Freight rail service demand increases by 37% from 2021 to 2050. Hybrid and alternative fuel technologies begin to appear in the 2025 model year. By 2050, fuel cells, electric, hybrid, and convention rail all have similar market shares.

<img src='..\UsersGuideGraphics\C2-31.png' /><br>
**Figure 2.31 Reference Scenario service output by freight rail technologies**

#### Domestic marine technologies

Demand for domestic marine freight nearly doubles between 2021 and 2050. From 2025, hybrid technologies begin to appear and by 2040 have gained more than half the market. Hybrids remain dominant, but electric and fuel cell ships begin to achieve a growing market share in 2040.

<img src='..\UsersGuideGraphics\C2-32.png' /><br>
**Figure 2.32 Reference Scenario service output by domestic marine shipping technologies**

#### International marine technologies

Demand for international shipping grows at a smaller rate than domestic shipping, increasing 35% from 2021 to 2050. By 2040, hybrid technologies will provide more than half of the ton-km. By 2050, that has grown to two-thirds.

<img src='..\UsersGuideGraphics\C2-33.png' /><br>
**Figure 2.33 Reference Scenario service output by international marine shipping technologies**

### 2.4.5 Emissions of CO2 and air pollutants

#### CO2 emissions

CO2 emissions are produced at the state level, as well as from the “USA” region that includes several source categories that have not been disaggregated to the state level (e.g., oil and gas production, coal mining, and H2 production).

US CO2 emissions are estimated to decline steadily from 2021 to 2050, ultimately falling 18%. Note that emissions of CO2 are presented in units of Megatonnes of Carbon (MTC). To convert to MTCO2, multiply these values by 44/12.  

<img src='..\UsersGuideGraphics\C2-34.png' /><br>
**Figure 2.34 Reference Scenario CO2 emissions**

In Figure 2.35, CO2 emissions are aggregated by sector. Negative emissions from biomass growth reflect the CO2 that is removed from the atmosphere when growing the biomass used for bioenergy. In the scenario, CO2 emissions decline over time, driven by reductions from light duty transportation (transport-LDV) and the electric sector. Economic growth drives an increase in industrial CO2 emissions. Other sectors remain relatively constant despite increasing energy service demands.

<img src='..\UsersGuideGraphics\C2-35.png' /><br>
**Figure 2.35 Reference Scenario CO2 emissions by sector**

#### NOx emissions

NOx emissions decline from 2021 to 2050, and especiallly dramatically between 2030 and 2040. Onroad vehicles (transport-LDV and transport-HDV) and the electric sector are primarily responsible for this decline. For onroad vehicles, the Tier 3 engine and mobile vehicle fuel standards drive this trend. After 2035, additional reductions come from vehicle electrification. In the electric sector, fuel switching from coal to natural gas and renewables is driving emissions lower. Industry and air-locomotive-marine (transport-ALM) together emitted nearly 75% of total anthropogenic NOx in 2021. This holds steady through 2050 as emissions from other sectors largely decline.

<img src='..\UsersGuideGraphics\C2-36.png' /><br>
**Figure 2.36 Reference Scenario Nox emissions by sector**

#### SO2 emissions

Coal-fired power generation dominates SO2 emissions in 2021. However, emission limits combined with the retirement of coal plants reduces the power sector’s contribution by approximately one-third by 2020. Industrial SO2 grows slowly over the time horizon, while transport-ALM, which is driven by international shipping, decreases.

<img src='..\UsersGuideGraphics\C2-37.png' /><br>
**Figure 2.37 Reference Scenario SO2 emissions by sector**

#### PM2.5 emissions

Industrial sources, including combustion and processes, represent approximately half of PM2.5 emissions. Note that this category includes construction, mining, and agriculture. Residential wood combustion also accounts for a large share of PM2.5 emissions. Emissions from this category decrease over time, driven both by cleaner wood-burning technologies and by a transition to greater use of electric heating technologies.

<img src='..\UsersGuideGraphics\C2-38.png' /><br>
**Figure 2.38 Reference Scenario PM2.5 emissions by sector**


## 2.5 Comparing the Net Zero Scenario to the Reference Scenario

In this section, we look at a few ways that the *NZ-GCAM-USA-9.1* scenario differs from the *Ref-GCAM-USA-9.1* reference scenario.

### Primary energy consumption

Figure 2.39 shows the difference in primary energy consumption in the USA between the net-zero and reference scenarios. In the net-zero scenario, total primary energy consumption is fairly similar to reference scenario values (from 6% less than the reference case in 2030 to 14% more in 2050), but individual fuel usage changes quite a lot. 

The net-zero scenario results in substantially reduced fossil fuel consumption compared to the reference scenario. There is a more sudden drop in coal usage, though levels are similar by 2050. Also in 2050,  the net-zero scenario sees a 10 EJ (51%) reduction in primary energy from oil and an 11.4 EJ (37%) reduction in primary energy from natural gas relative to the reference scenario. There is a simultaneous increase in renewable usage, particularly in the case of biomass (by 2050, up 26 EJ (585%) over reference case levels), but also solar and wind (up about 2 EJ (40%)).

<img src='..\UsersGuideGraphics\C2-39.png' /><br>
**Figure 2.39 Difference in primary energy consumption by sector (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**


### Final energy consumption

The net-zero scenario sees lower final energy consumption in all sectors relative to the reference case (an overall 5-6% reduction from 2040 on) and a faster transition away from fossil fuels. 


#### By fuel

In the net-zero scenario, usage of refined liquids, natural gas, and coal is lower than reference-scenario values in every non-calibration year, while relative consumption of energy in the form of hydrogen, biomass, and electricity increases from 2030 on. In 2050, hydrogen is up 20%, biomass up 11%, and electricity up 5% over reference scenario values, while refined liquids are down 10%, natural gas down 15%, and coal down 28% from the reference scenario.

<img src='..\UsersGuideGraphics\C2-40.png' /><br>
**Figure 2.40 Difference in final energy consumption by fuel (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**


#### By aggregate sector

While final energy consumption is reduced in all sectors, the largest percent change is observed in the transport-ALM and transport-HDV sectors (12% and 11%, respectively, in 2050).

<img src='..\UsersGuideGraphics\C2-41.png' /><br>
**Figure 2.41 Difference in final energy consumption by sector (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**


### Electricity generation

Figure 2.42 shows the difference in electricity generation by aggregated subsector between the net-zero and reference scenarios. The net-zero scenario sees substantial reductions in natural gas usage in electricity production relative to the reference scenario, with increases in wind, solar, and biomass. The net effect is a 5% increase in electricity generation.

<img src='..\UsersGuideGraphics\C2-42.png' /><br>
**Figure 2.42 Difference in electricity generation by aggregated subsector with renewable detail (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**


### Emissions

Emissions in the net-zero scenario change in response to changes in fuel consumption. 
> <span style='color:red'>Biomass growth, for instance, reduces net CO2 emissions, while the greater use of biomass in electricity production contributes to increasing particulate matter levels. \[More here\]</span>


<img src='..\UsersGuideGraphics\C2-43.png' /><br>
**Figure 2.43 Difference in CO2 emissions by aggregate sector (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**


<img src='..\UsersGuideGraphics\C2-44.png' /><br>
**Figure 2.44 Difference in PM2.5 emissions by aggregate sector (*NZ-GCAM-USA-9.1* minus *Ref-GCAM-USA-9.1*)**



