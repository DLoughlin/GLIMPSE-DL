# Chapter 1꞉ Overview

GLIMPSE-CE is a derivative of the [GLIMPSE](https://epa.gov/glimpse) decision support tool developed at the U.S. EPA to assist the EPA, states, and others with long-term environmental and energy planning. GLIMPSE-CE is not sponsored by the U.S. EPA.

GLIMPSE-CE is an acronym for the "GCAM Long-Term Interactive Multi-Pollutant Scenario Evaluator - Community Edition", where GCAM is the "Global Change Analysis Model". GCAM, a human-Earth systems model developed by Pacific Northwest National Laboratory (PNNL), simulates the co-evolution of the economy, energy system, land use, and climate systems, including how this co-evolution is shaped by policy and other external factors. The GLIMPSE-CE (Community Edition) software, referred to throughout as GLIMPSE, acts as a graphical user interface for GCAM. 

Using GLIMPSE, decision-makers and analysts at the national, regional, and state levels can examine potential policies, investigate the impacts of emerging technologies, develop cost-effective strategies for meeting air pollutant and greenhouse gas (GHG) mitigation targets, and explore the tradeoffs at the nexus of energy, water, and land use. Additionally, GLIMPSE can be used in a classroom setting, providing students with the ability to use a state-of-the-art human-Earth systems model to investigate alternative scenarios of the future.

<br>
<details open><summary><b>Sections</b></summary><br>

[1.1 Background](#11-background)

[1.2 Motivation](#12-motivation)

[1.3 GCAM-USA](#13-gcam-usa)

[1.4 Components of GLIMPSE](#14-components-of-glimpse)

[1.5 Design philosophy](#15-design-philosophy)

[1.6 Computer and software requirements](#16-computer-and-software-requirements)

[1.7 Organization of this Users' Guide](#17-organization-of-this-users-guide)

[1.8 GLIMPSE version](#18-glimpse-version)

[1.9 A note on units](#19-a-note-on-units)

[1.10 Interpreting GCAM results](#110-interpreting-gcam-results)

[1.11 Known bugs, limitations, and other considerations](#111-known-bugs-limitations-and-other-considerations)

[1.12 Where to learn more about GCAM and GLIMPSE](#112-where-to-learn-more-about-gcam-and-glimpse)

[1.13 How to get started using GLIMPSE](#113-how-to-get-started-using-glimpse)

[1.14 Where to get assistance or provide feedback](#114-where-to-get-assistance-or-provide-feedback)
</details>

<h2 id="11-background">1.1 Background</h2>

GLIMPSE can be used with GCAM or with variants of GCAM that have additional spatial resolution, such as GCAM-USA, which represents the U.S. energy system at the state level. 

Among the attributes of GCAM (and GCAM-USA) that led to its inclusion in GLIMPSE are:
-	spatial coverage and resolution (global, with available state-level resolution for the U.S.), allowing examination of national and state actions in a global context,
-	temporal range and resolution (2010-2100 in 5-year increments by default), supporting long-term air-climate-energy planning,
-	runtime of one to several hours, depending on which policies are included in the simulation, facilitating sensitivity and scenario analyses, 
-	input and output formats that are amenable to integration with a user interface,
-	emission outputs including both greenhouse gases (GHGs) (CO<sub>2</sub>, CH<sub>4</sub>, N<sub>2</sub>O, HFCs and CFCs) and traditional air pollutants (NO<sub>x</sub>, SO<sub>2</sub>, CO, PM<sub>2.5</sub>, VOCs, and NH<sub>3</sub>), covering major pollutants of concern in the US, 
-	characterization of water supply and demand across sectors, allowing investigation of water-energy-land-agricultural dynamics in the context of a changing climate,
-	no requirement for specialized hardware or proprietary software, thus lowering the barriers for adoption,
-	the GCAM source code and data, both of which are regularly updated, existing in the public domain and freely available, promoting transparency, and,
-	the model's primary developers, the Joint Global Change Research Institute (JGCRI) of PNNL, provide model documentation, have helped cultivate a broad user community, and hold an annual modelers' workshop.

JGCRI makes [versions of GCAM and GCAM-USA](https://github.com/JGCRI/gcam-core) available via GitHub. [Documentation](http://jgcri.github.io/gcam-doc/) is also available, and tutorials and training are provided at annual [GCAM Community Modeling Meetings](https://gcims.pnnl.gov/community). 

We have adopted a publicly available version of GCAM-USA 9.1 for use in GLIMPSE and have worked with PNNL researchers to modify that version by adding air pollutant emission factors, updating technology attributes in the transportation and power sectors, and incorporating key U.S. air quality and energy policies. 

<img src='..\UsersGuideGraphics\C1-1.png' width='100%'/><br>**Figure 1.1 GCAM inputs, outputs, and major components.** GCAM includes representations of energy, water, land use, agricultural, and climate systems, simulating their co-evolution.

<h2 id="12-motivation">1.2 Motivation</h2>

### 1.2.1 The energy system

Understanding energy system concepts and terminology is important for understanding how GLIMPSE and GCAM-USA can be used to explore GHG mitigation strategies, air pollution control strategies, and strategies for meeting climate and air quality goals simultaneously. 

In the context of GLIMPSE, the term "energy system" refers to all processes and fuels that extend from: 
-	importing or extracting raw forms of energy (e.g., crude oil, coal, natural gas, uranium, and wind),
-	converting (e.g., in refineries and power plants) those raw forms of energy into useful forms of energy (e.g., gasoline, diesel, and electricity), and
-	applying useful energy to meet end-use energy service demands (e.g., passenger and freight travel, space conditioning, water heating, and lighting). 

The figure below shows a depiction of these components: 

<img src='..\UsersGuideGraphics\C1-2.png' width='100%'/><br>**Figure 1.2 Schematic of the energy system.** The energy system extends from the import or extraction of primary energy, through its processing and conversion into useful forms, through its use in meeting final end-use energy demands.

Several important terms used when describing the energy system are defined below: 
-	Primary, secondary, and tertiary energy – Primary energy is in the raw form in which it is first accounted for in a statistical energy balance before any transformation to secondary or tertiary forms occurs. For example, coal can be converted to synthetic gas, which can be converted to electricity; coal is primary energy, synthetic gas is secondary energy, and electricity is tertiary energy. (Source: EIA)
-	Final energy – The energy that is consumed by end-users, including for transportation, residences, commercial buildings, and industry. Examples include electricity, gasoline, and natural gas. 
-	Useful energy – The portion of final energy that is used to meet energy services. This portion does not include energy that is wasted due to factors such as line loss, plug loss, leakage, and waste heat.
-	Rejected energy - Energy that is lost through inefficiencies such as line loss, plug loss, leakage, and waste heat. 
-	Energy services – Activities that require energy, including space conditioning, water heating, lighting, and passenger and freight transportation. Energy services can be expressed in units of energy (e.g., exajoules), but are also often expressed in physical units, such as lumens in lighting and passenger-km for travel. 

As energy is transformed through the energy system, there are inherent inefficiencies and losses. In the U.S., the quantity of useful energy is less than the quantity of rejected energy, as shown in the Sankey diagram below. 

<img src='..\UsersGuideGraphics\C1-3.png'/><br>**Figure 1.3 Energy flows and consumption in the U.S. in 2021.** This Sankey diagram tracks the flow of energy through the U.S. energy system, including useful and wasted energy. Source: LLNL

### 1.2.2 Energy and the environment

The energy system has many intersections with the environment. For example, based on the 2020 EPA Inventory of Greenhouse Gas Sources and Sinks, energy supply and use are responsible for more than 96% of U.S. anthropogenic CO<sub>2</sub> emissions and 82% of overall U.S. anthropogenic GHGs. As a result, the energy system is a major focus of climate action at the state and federal levels, including in the [climate action plans enacted by more than half of states](https://www.c2es.org/document/climate-action-plans/) and in federal regulations such as the [Corporate Average Fleet Efficiency standards for onroad vehicles](https://www.nhtsa.gov/laws-regulations/corporate-average-fuel-economy). 

Energy is also a source of air pollutants. According to the EPA's [National Emission Inventory](https://www.epa.gov/air-emissions-inventories/air-pollutant-emissions-trends-data) (NEI), the energy system contributes 91% of U.S. anthropogenic NO<sub>x</sub> emissions, 75% of SO<sub>2</sub>, 74% of CO, 45% of VOCs, and 22% of directly emitted fine PM in 2021.  Combustion of fossil fuels is the main contributor to most of these emissions, although natural gas leakage, evaporative processes, cement and fertilizer manufacturing contribute as well. 

Despite significant improvements in air quality over the past decades, [more than 100 million people in the U.S. are estimated to live within areas that exceed one or more National Ambient Air Quality Standards (NAAQS)](https://www3.epa.gov/airquality/greenbook/popexp.html). Thus, reducing the contribution of the energy system to air pollutant emissions should be a priority for agencies such as the U.S. EPA. 

<img src='..\UsersGuideGraphics\C1-4.png'/><br>**Figure 1.4 Map of U.S. non-attainment areas.** Shaded counties did not attain one or more National Ambient Air Quality Standards as of March 31, 2026. Source: [EPA Greenbook](https://www3.epa.gov/airquality/greenbook/popexp.html)

Environmental impacts associated with energy are not limited to climate and air quality. [In the U.S., freshwater withdrawals for thermoelectric power plant operations in 2015 were nearly as great as those of agriculture](https://pubs.usgs.gov/fs/2018/3035/fs20183035.pdf), 41% vs 42%. As a result, cooling water requirements make the energy system susceptible to droughts. Furthermore, used cooling water is typically discharged into rivers or lakes. Drought and high ambient temperatures can limit the ability of these bodies to absorb additional heat without damaging sensitive aquatic ecosystems. [These conditions can result in the temporary shutdown of thermal generating capacity if the Total Maximum Daily Load (TMDL) limits for a discharge body are exceeded](https://www.epa.gov/tmdl/overview-total-maximum-daily-loads-tmdls). Energy is also a major source of solid and liquid waste. [Electricity production, for example, was responsible for 450,000 tons of solid waste in 2019](https://www.epa.gov/trinationalanalysis/electric-utilities-waste-management-trend). 

### 1.2.3 Environmental management

There are many options available for addressing the environmental impacts of energy production and use. The traditional approach is to use control devices to capture pollutants from exhaust gases and liquid waste streams. While pollution controls have been used successfully and are responsible for much of the environmental progress that has been made to date, controls can result in important tradeoffs. For example, scrubbers that remove SO<sub>2</sub> from exhaust gases generate a liquid waste stream that must be treated. Also, carbon capture devices that remove CO<sub>2</sub> from an exhaust stream are energy intensive; by requiring more fuel, pollution associated with coal and gas production may increase. 

Non-traditional control options include reducing demands for energy through energy efficiency, switching to fuels with lower emissions intensity, and the combination of electrifying end-use technologies in transportation and buildings while simultaneously shifting electrification to clean sources. An important aspect of many of these management strategies is that they have the potential to benefit climate and environmental endpoints simultaneously. 

However, some non-traditional measures can result in changes in the types and locations of pollutants that are emitted. For example, while considered to be a low-carbon fuel, biomass can have high air pollutant emissions, and there are emissions associated with the manufacturing of batteries, solar panels, and wind turbines. There can also be cross-sector interactions. For example, depending on how electricity demands are met, electrification can increase electric sector emissions, as well as the emissions associated with natural gas production and transportation. Shifts in energy supply and demand can also result in price-induced fuel switching in other sectors.

In this complex landscape, it is important for policymakers to be able to:
-	quantify the benefits and co-benefits associated with various management options,
-	understand cross-sector interactions, tradeoffs, and potential unintended consequences, 
-	evaluate how uncertainty in future conditions may present opportunities or challenges for climate action and environmental management, and
-	identify management options that are cost-effective and robust through approaches such as scenario analysis.

Addressing these needs is the objective of the GLIMPSE project. 

<h2 id="13-gcam-usa">1.3 GCAM-USA</h2>

The computational engine for GLIMPSE is GCAM. Specifically, we are using GCAM-USA, a version of GCAM that includes state-level resolution of the U.S. energy system. GCAM-USA version 9.1 covers a time horizon of 2015 through 2100, calibrated to 2021 and simulated at the annual resolution in 5-year time steps. 

In GCAM-USA, the 50 U.S. states plus the District of Columbia are explicit regions that operate within the global GCAM model. Energy transformation (electricity generation and refined liquids production) and end-use demands (buildings, transportation, and industry) are modeled at the state resolution. Interstate trade of all energy goods is simulated, with state-specific consumer price mark-ups assigned for coal, natural gas, and refined liquids based on price data from [EIA 2017](https://www.eia.gov/state/seds/data.php?incfile=/state/seds/sep_sum/html/sum_pr_tot.html&sid=US). 

Note that several aspects of the energy system are not disaggregated to the state level. Most notably, this applies to primary production of fossil resources including oil, gas, and coal. The supply of biomass energy feedstocks, which include residues and dedicated energy crops, is modeled at the level of 22 water basins in the United States ([Calvin et al. 2019](https://doi.org/10.5194/gmd-12-677-2019), [Calvin et al. 2014](https://doi.org/10.1007/s10584-013-0897-y), [Wise et al. 2014](http://dx.doi.org/10.1016/j.apenergy.2013.08.042)). 

<img src='..\UsersGuideGraphics\C1-5.png'/><br>**Figure 1.5 GCAM-USA regions.** GCAM Global Regions and GCAM-USA States Map. Source: PNNL

Attributes of GCAM-USA are summarized in the table below: 

*Table 1.1 Summary of GCAM-USA attributes and links to documentation and source code. See the appendix for definition of abbreviations.*
| Attribute              | Description                                                                                                                                                                                                                                                                                                                                        |
| :--------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Type                   | Technology-rich, market-based human-Earth systems model                                                                                                                                                                                                                                                                                            |
| Solution approach      | Partial equilibrium (GDP is determined exogenously) <br>Nonlinear programming to identify market clearing prices for all markets <br>Myopic (no foresight to future time periods)  <br>Dynamic recursive (each time step starts with the prior time step's solution)                                                                                           |
| Economic choice        | Market shares of competing technologies are determined using a logit function, which considers the relative costs of technology options, technology-specific shareweights (representing bias), and a logit exponent (reflecting the degree to which cost-differences impact choice)                                                                |
| Temporal coverage      | 2015-2100 (although typically run to 2050 in GLIMPSE applications)                                                                                                                                                                                                                                                                                 |
| Temporal resolution    | 5-year timesteps (although alternative values, including annual are possible)                                                                                                                                                                                                                                                                      |
| Spatial coverage       | Global                                                                                                                                                                                                                                                                                                                                             |
| Spatial resolution     | Energy and economic: 32 global regions, with the U.S. disaggregated by state<br>U.S. electric grid: 15 regions, similar to the NERC regions, but following state boundaries <br>Water and land use: 235 regions, based on water basins                                                                                                                    |
| Sectoral coverage      | Resources (extraction and mining), refining (oil and biomass), electricity production (fossil and renewables), industry (manufacturing, non-manufacturing, and nonroad), commercial, residential, passenger travel (onroad, air, rail), freight travel (truck, rail, marine), and agriculture (livestock, poultry; biomass for energy, food, feed) |
| Pollutant coverage     | GHGs and SLCPs: CO<sub>2</sub>, CH<sub>4</sub>, N<sub>2</sub>O, BC, OC <br>Air pollutants: NO<sub>x</sub>, SO<sub>2</sub>, CO, NH<sub>3</sub>, VOC, PM<sub>10</sub>, PM<sub>2.5</sub>                                                                                                                                                                                                                                                          |
| Policy representations | Emission taxes and caps, technology and fuel taxes and subsidies, technology capacity and market share targets, clean energy and renewable portfolio standards                                                                                                                                                                                     |
| Implementation         | Model coding: Object-oriented C++ <br>Data system: CSV tables that are converted to XML input files using R                                                                                                                                                                                                                                            |
| Platforms              | Windows, Apple, Linux (including high performance clusters)                                                                                                                                                                                                                   |
| Computing requirements | At least 12 GB of RAM and 80 GB of free disk space are needed for core GCAM. 16 GB or more of RAM is recommended for GCAM-USA, and we have found that 20 GB or more is preferred when state-level renewable portfolio standards are included in scenarios.                                                                                              |
| Runtime                | 30 minutes to 4 hours on a typical desktop Windows computer, depending on policies and other modifications to the reference setup.                                                                                                                                                                                                                 |
| Source code and data   | [https://github.com/JGCRI/gcam-core](https://github.com/JGCRI/gcam-core) (Open source)                                                                                                                                                                                                                                                             |
| Documentation          | GCAM 9.1 documentation: [http://jgcri.github.io/gcam-doc/toc.html](http://jgcri.github.io/gcam-doc/toc.html) <br> GCAM Developer's Guide: [http://jgcri.github.io/gcam-doc/dev-guide.html](http://jgcri.github.io/gcam-doc/dev-guide.html)                                                                                                         |

<h2 id="14-components-of-glimpse">1.4 Components of GLIMPSE</h2>

GLIMPSE serves as a graphical interface to GCAM and GCAM-USA. We refer to both versions of the model as "GCAM" from here on unless clarification is necessary. The graphical interface consists of two primary components, the *Scenario Builder* and the *ModelInterface*. 

The *Scenario Builder* allows users to alter input assumptions and construct policy scenarios, as well as to manage the execution of GCAM. Through its *New Scenario Component Creator*, users can construct GHG targets, sectoral emission caps, technology subsidies, clean energy standards, electric vehicle market penetration targets, and different assumptions about the characteristics of technologies. The GLIMPSE *ModelInterface* builds upon the *ModelInterface* that PNNL distributes with GCAM, providing additional capabilities for filtering, visualizing, analyzing, comparing, and exporting model results. In this Users' Guide, we refer to the GLIMPSE *ModelInterface* as the *ModelInterface*.  

These components are described and demonstrated in the [Tutorial](UsersGuideDocs/Tutorial%201꞉%20Running%20GCAM%20through%20GLIMPSE.md) section of the Users' Guide. Attendance of GLIMPSE hands-on training sessions is highly recommended for those who are considering using GLIMPSE for their applications. 

<h2 id="15-design-philosophy">1.5 Design philosophy</h2>

GLIMPSE has been developed to meet the needs of both experienced GCAM users and those who are new to GCAM. For experienced users, the *Scenario Builder* will enhance their typical GCAM workflow by organizing a library of scenarios and scenario components, managing single and batch execution, providing quick access to logs, and archiving the files that are specific to a scenario. GLIMPSE also automates some activities that would be tedious even for an experienced user, such as developing policy "add-on" files that implement an emissions cap or clean energy standard over a group of states.

Those who are new to GCAM will benefit by being able to rapidly set up, execute, and examine the results of scenarios. Furthermore, conversations with prospective GCAM users have helped us identify and implement the scenario "levers" that address many of their modeling needs, and these levers have been added to GLIMPSE. While GCAM still has a substantial learning curve, GLIMPSE can address many of the barriers that new users face. 

When GCAM execution of a scenario begins, the model reads a configuration file which specifies many aspects of the run, such as the number of time periods to simulate, the name of the output database into which results will be placed, and which scenario components will be included. Scenario components are eXtensible Markup Language (XML)-formatted files that provide the data used by the model, including parameterizations of the electricity production, refining, industrial, commercial, residential, and transportation sectors. Scenario components can also include representations of policies or alternative assumptions about technologies, population, and GDP growth. 

To simulate an alternative scenario, references to different or additional scenario components can be included in the configuration file. The order in which these "add-on" scenario components are listed is important: if a parameter value occurs in several scenario components, the last value overrides prior values. Thus, policies or alternative assumptions about technologies typically can be specified in "add-on" files that are listed at the bottom of the scenario component list. 

GLIMPSE supports this workflow. GLIMPSE includes a template configuration file. Through the *Scenario Builder* the user can easily create a scenario based on this template, modified to reflect the user's choices of output database, years to simulate, etc. Furthermore, the user can add new scenario components by selecting them from a *Component Library*. The resulting scenario can then be saved to a *Scenario Library*. Scenarios in the library can be executed individually or in batches, and the status of each is displayed. 

<img src='..\UsersGuideGraphics\T1-2.png' /><br>**Figure 1.6 The *Scenario Builder*.** The *Scenario Builder* facilitates development and execution of scenarios.

GLIMPSE also supports exploration of model results through the *ModelInterface*. With this tool, users can extract, filter, rapidly visualize, and compare many outputs across regions or scenarios. 

The *ModelInterface* uses queries to extract data from the GCAM output database. We have organized the query list such that those that are anticipated to be of greatest utility to GLIMPSE users are grouped at the top. Hovering the mouse over these queries will produce a "tooltip" that includes a brief description of the query. As GLIMPSE develops, we plan to have the option of viewing results in units more meaningful for users in the energy and air quality management fields (e.g., GWh instead of EJ; short tons instead of metric tonnes). We also plan to support additional types of graphics, including maps and Sankey diagrams.  

While some graphical capabilities are provided by the *ModelInterface*, users can also readily export data for analysis using other tools. For example, open query results can be saved to comma-separated-value (CSV) files. Alternatively, users can drag a table into Excel by dragging the tab associated with the table into an open Excel workbook. 

<img src='..\UsersGuideGraphics\T3-13.png' /><br>**Figure 1.7 The GLIMPSE *ModelInterface*.** The *ModelInterface* supports exploratory investigation of GCAM results, including visualization and examining the differences in model outputs from one scenario to another.

GLIMPSE simplifies the process of creating add-on scenario components. GCAM modelers typically create add-on files in one of several ways. Simple scenario components are often created by a user in a text or XML editor. However, constructing complex scenario component files in this manner can be very tedious. Instead, GCAM modelers often use a two-step approach. First, a table of data is saved as a CSV file. A header file defines how the tabular data in the CSV file is to be converted to XML by the CSVtoXML.jar Java program that is integrated into the *ModelInterface*. Even this process can be time consuming if there are many technologies or regions that are being affected. For example, a state-level renewable portfolio standard CSV file may require hundreds of thousands of rows.

The *Scenario Builder* includes a *New Scenario Component Creator* that provides an alternative way to construct add-on files. Using this feature, users can implement a variety of policies and introduce alternative assumptions about technologies and fuels. These modifications can easily be applied to a single region or to a group of regions. Among the options available include: 
-	pollutant taxes or caps, introduced for a single sector or economy-wide,
-	technology market share constraints as a fraction of new sales or of total stock,
-	technology availability, including first and last year available,
-	technology-specific taxes and subsidies,
-	alternative values for technology costs and efficiencies,
-	adjustments to consumer preferences via the share weight parameter, and
-	fuel price adjustments.

GLIMPSE automates the process of generating the appropriate CSV file, selecting the matching header, then executing CSVtoXML.jar to produce the corresponding XML. 

<img src='..\UsersGuideGraphics\T5-2.png' /><br>**Figure 1.8 *New Scenario Component Creator*.** This dialog allows users to create their own Scenario Components, including implementations of policies and alternative assumptions about technologies.

<h2 id="16-computer-and-software-requirements">1.6 Computer and software requirements</h2>

The GLIMPSE software is written in the Java programming language and requires the 64-bit version of the Java Runtime Environment (JRE), version 8, which is also referred to as JRE 1.8. We include [Amazon's Corretto version of the JRE](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html) as part of the GLIMPSE package, although users are able to configure GLIMPSE to use other versions of Java JRE 1.8 that incorporate the JavaFX libraries. Note that OpenJDK JRE 1.8 is not packaged with JavaFX. 

GLIMPSE is available on Windows PCs and Macs only, although a Linux version is expected to be supported in the future.

The GCAM model itself is computationally and memory intensive. At least 12 GB of RAM and 80 GB of free disk space are needed for GCAM. 16 GB or more of RAM is recommended for GCAM-USA. We have found that 20 GB or more can be necessary when simulating complex policies such as state-level renewable portfolio standards.  

Each simulation generates more than 3 GB of results. Hard disk space is also used as virtual memory. We recommend that your computer has at least 80 GB of free hard disk space when GLIMPSE is installed, but more is preferable. 

<h2 id="17-organization-of-this-users-guide">1.7 Organization of this Users' Guide</h2>

This Users' Guide includes installation instructions, a multi-part tutorial, a description of the GLIMPSE Reference Scenario, and key results. The Users' Guide also includes a brief overview of how GCAM works, instructions on performing common tasks, additional information for advanced users, and descriptions of the components of the graphical user interface. A troubleshooting section helps with common problems, and a glossary defines key terms and acronyms. 

<h2 id="18-glimpse-version">1.8 GLIMPSE Version</h2>

This Users' Guide has been developed specifically for GLIMPSE-CE-2.2, though figures have been constructed using version 2.03 with GCAM 8.2. If you are using a more recent version of GLIMPSE, some of the information provided here may no longer be accurate. Furthermore, this User's Guide will evolve as users report their experiences.

GLIMPSE currently incorporates GCAM 9.1, which was released in the spring of 2026. GCAM releases typically occur at least once per year. See [https://github.com/JGCRI/gcam-core/releases](https://github.com/JGCRI/gcam-core/releases) for information on each GCAM release, including new features. 

<h2 id="19-a-note-on-units">1.9 A note on units</h2>

As a result of GCAM's origins in GHG emission projections and climate analyses, the units in GLIMPSE and GCAM may be different than those typical for applications in the air quality and energy fields. 

* Metric units are used in all instances. In this documentation, we refer to metric tons as "tonnes". However, in the GCAM outputs, "tons" is used.

* Energy values are typically provided in Exajoules, which are joules x 10^18. For reference, one EJ is 277,778 Gigawatt hours (GWh) or 277.778 Terawatt hours (TWh). For reference, the US power sector produced approximately 16 EJ of electricity in 2020, and the onroad transportation sector used approximately 21 EJ of gasoline, diesel, and ethanol in the same year. 

* Where CO<sub>2</sub> outputs are presented in the *ModelInterface*, these are in units of million metric tonnes of Carbon, or MTC. To convert MTC to MTCO<sub>2</sub>, multiply by the ratio 44/12, which is based on the relative molecular weights of CO<sub>2</sub> (44) and C (12). 

* Air pollutant emissions are reported in teragrams (Tg), or grams x 10^12. A Tg is equivalent to a million metric tonnes, or a MT. One MT is equivalent to 1,102,310 US short tons. 

* The $-years for monetary values are different than today's $s, resulting in the need to adjust for inflation when interpreting results.

  * Prices and costs typically are reported in 1990$s. 

  * Technology taxes and subsidies are represented in 1975$s per unit of output 

  * Based on the Consumer Price Index, 

    * \$1 in 1975\$s is equivalent to \$2.40 in 1990\$s

    * \$1 in 1975\$s is equivalent to \$6.14 in 2026\$s

    * \$1 in 1990\$s is equivalent to \$2.53 in 2026\$s.

* Travel demands are represented in billion passenger-km or billion tonne-km in GCAM 9.1, but million passenger-km or million ton-km in GCAM 8.2.

<h2 id="110-interpreting-gcam-results">1.10 Interpreting GCAM results</h2>

While GLIMPSE simplifies tasks such as developing policy scenarios, executing GCAM, and analyzing results, GLIMPSE users should keep in mind that neither it, nor the underlying GCAM model, are commercial products. Documentation is available, but support is limited.

You can find the current GLIMPSE-CE releases via the GLIMPSE-CE GitHub page: [https://github.com/DLoughlin/GLIMPSE-CE](https://github.com/DLoughlin/GLIMPSE-CE). Please consider reporting back your experiences, feature requests, comments, and suggestions, either via the GitHub repository's "Issues" or "Discussion" areas or directly to Dan Loughlin ([Dan@En2MG.com](Dan@En2MG.com)).

Furthermore, please note that GCAM is a complicated model. Applying it for research and policy analyses requires experience and skill that takes time to develop. For example, when assessing model results, it is recommended that users look beyond the high level results (e.g., whether emissions went up or down) and examine the detailed technology-level outputs, asking questions such as "Do these responses make sense?", "Is this result reasonable, or did I uncover a response that arose because the scenario pushed the model in a new direction?", "How did limitations in the model formulation affect this result?", and "How were the results impacted by calibration to historical data or by assumptions about future conditions?" 

Users should not expect GLIMPSE to provide "turnkey" answers. Analyses with tools such as GLIMPSE and GCAM are iterative processes, with each iteration providing additional information about the problem, potential solutions, and the representation of these in the model. It is common to make refinements to scenario assumptions and policy implementations as part of this iterative process.

When presenting model results, it is important to avoid statements such as: "If policy X is implemented, Y will happen." Results are contingent on many factors, including the assumptions about population growth and migration, economic growth, technology costs and efficiencies, climate change, and human behavior and choices. Predicting these factors into the future is inherently uncertainty. Additionally, while the logit algorithm that predicts market shares has been calibrated to past decisions, real-world human decision-making involves many considerations beyond the relative costs of competing options. 

Thus, to properly present and caveat findings, it is important for analysts to understand the operation of the model, key assumptions, and limitations. For any given analysis, users are encouraged to explore how a scenario impacts technology market shares, market prices, and fuel use. These will provide insights regarding the underlying pathways and mechanisms that led to observed results. Sensitivity analysis is encouraged, which will indicate how the model's results change in response to incremental perturbations in key input parameters. 

To begin the process of learning how GCAM operates, we highly recommend that users read the "How does GCAM work?" portion of this Users' Guide, which includes information about the operation of markets, the logit function, shareweights, and model calibration. 

When applying GCAM to particularly challenging policy scenarios (e.g., a Net Zero CO<sub>2</sub> target or to specific GHG emissions targets), users should think holistically about the scenario and how Reference Case assumptions may shift under such a target. For example, users may want to modify shareweights for electric vehicles to reflect conditions that are not simulated by GCAM, such as the investment and build-out of a charging infrastructure. The process of developing Deep Decarbonization scenarios, and others that diverge significantly from historic and Reference Case conditions, may involve many such considerations. 

<h2 id="111-known-bugs-limitations-and-other-considerations">1.11 Known bugs, limitations, and other considerations</h2>

GLIMPSE users should take note of the following. 

* Unexpected termination – GCAM will occasionally terminate unexpectedly, reporting "DNF" (for "Did not finish") to the *Scenario Builder*. There are several causes, including the computer's available resources being exhausted (e.g., RAM or disk space), conflicts in the names of markets or policies, or incompletely defined technologies or markets in the model. We have built tools into the *Scenario Builder* to avoid and help deduce the cause of these problems. Nonetheless, some level of "debugging" by users may be required. See Section 4.16 for more information.

* Numerical issues – With GCAM's global coverage and representation of thousands of markets of diverse sizes, the model's solver must deal with numbers that vary greatly in magnitude. Under some circumstances (e.g., a market share constraint on a very small market) this can result in numerical issues that can lead to market failures. This Users' Guide includes information about how unsolved market messages can be interpreted and how to avoid numerical issues by adjusting the solver parameterization. See [Section 5.3](Chapter%205꞉%20Advanced%20topics.md#53-interpreting-and-debugging-unsolved-market-information-in-the-main_logtxt-file) for more information. 

* Output database size limitations – GCAM uses the BaseX database software for storing model results. We have found the BaseX software to be unable to open databases that exceed 40 GB in size. For GCAM, this is the equivalent of 15 to 20 results. Once the database exceeds this limit, it can no longer be opened and the data inside are lost. We recommend that users pay particular attention to database sizes and to the information in [Section 4.5](UsersGuideDocs/Chapter%204꞉%20How%20do%20I...ʔ.md#45-how-do-i-manage-database-size) of this Users' Guide about how to monitor database size; export, import, and delete results from a database; and create new databases.

* *ModelInterface* freezes – There are instances when the *ModelInterface* freezes. When this occurs, users can terminate the *ModelInterface* task without terminating GLIMPSE by using the Windows Task Manager or the **Tools>Advanced>Stop ModelInterfaceJobs** menu option in the *Scenario Builder*. See [Section 6.4.2](UsersGuideDocs/Chapter%206꞉%20Reference.md#642-common-errors-using-the-modelinterface) for instructions on terminating the *ModelInterface* when it is frozen. 

* Boundaries on system representations – In a complex model such as GCAM, decisions must be made by the developers regarding where to draw the boundaries for the various systems represented in the model. These decisions are often impacted by factors such as data availability and computation challenges. Knowing these boundaries is important in understanding the responses of the model to policies and other perturbations. For example, while GCAM simulates biomass growth, an increase in biomass growth would not result in an increase in the transportation emissions necessary to transport that biomass to market.

* Myopic solution process – GCAM is a dynamic recursive simulation model. This means that the model steps through time, using the solution from the last modeled time period as the starting point for the current time period. Markets are solved in the current period by considering conditions in that period but not taking into consideration future conditions. As a result, GCAM's solution process cannot anticipate conditions in future time periods, such as the planned tightening of a carbon cap over time. As a result, the model may make decisions in the short term that are not optimal from a long-term perspective. 

* Calibration – GCAM uses a logit function to assign market share to competing technologies. The parameters used in the logit function for many technologies and markets are determined based upon conditions in the final calibration year, which is 2021 in GCAM-USA 9.1. For new and emerging technologies, these parameters are based upon assumptions about the speed at which barriers and biases will be addressed. These assumptions may not reflect behavior in the future, particularly for deep decarbonization scenarios that differ significantly from historical decisions and Reference Case assumptions. Please see [Section 5.5](UsersGuideDocs/Chapter%205꞉%20Advanced%20topics.md#55-considerations-when-modeling-a-deep-decarbonization-scenario) for a discussion of adjustments that are recommended for consideration when modeling deep decarbonization scenarios. 

<h2 id="112-where-to-learn-more-about-gcam-and-glimpse">1.12 Where to learn more about GCAM and GLIMPSE</h2>

If you would like to know more about GCAM, please see the model's on-line documentation. Important links include: 

General GCAM documentation, including for the latest public release: 

* [http://jgcri.github.io/gcam-doc/](http://jgcri.github.io/gcam-doc/)

GCAM documentation for version 9.1, which is included in this GLIMPSE release: 

* [https://github.com/JGCRI/gcam-core/releases/tag/gcam-v9.1](https://github.com/JGCRI/gcam-core/releases/tag/gcam-v9.1)

GCAM-USA documentation:

* [http://jgcri.github.io/gcam-doc/gcam-usa.html](http://jgcri.github.io/gcam-doc/gcam-usa.html)

GCAM Users' Guide:

* [http://jgcri.github.io/gcam-doc/user-guide.html](http://jgcri.github.io/gcam-doc/user-guide.html)

GCAM Video Tutorials:

* [https://gcims.pnnl.gov/community](https://gcims.pnnl.gov/community)

List of GCAM publications by PNNL authors: 

* [https://www.pnnl.gov/publications-reports?keywords=GCAM\&field\_document\_type=3](https://www.pnnl.gov/publications-reports?keywords=GCAM&field_document_type=3)

For more information about the GLIMPSE-CE software, please see the following links:
GLIMPSE-CE GitHub site (subject to change): https://github.com/DLoughlin/GLIMPSE-CE.

Below are publications in which GLIMPSE was used:

*	Auld, J., Zuniga Garcia, N., Waddell, P., de Souza, F., and D.H. Loughlin (2026). "Interactions between climate policy and technology influenced travel behavior: Mitigating induced demand from cooperative adaptive cruise control." *Transportation Research Record: Journal of the Transportation Research Board*, https://doi.org/10.1177/03611981261437050

*	Sadavarte, P., Shindell, D., and D.H. Loughlin. (2025). "Comparing the climate and air pollution footprints of lithium ion battery based electric vehicles and internal combustion engine vehicles in the United States incorporating systemic energy system responses." *PLOS Climate*, 4(10), e0000714. https://doi.org/10.1371/journal.pclm.0000714

*	Shankar, U., Murphy, B., Weber, M., Ou, Y., Smith, S., Loughlin, D.H., and C. Nolte. (2025). "Modeling the air quality impacts of future energy scenarios." *Environmental Science & Technology Air*. https://doi.org/10.1021/acsestair.5c00175

* Ou, Yang, Noah Kittner, Samaneh Babaee, Steven J. Smith, Christopher G. Nolte, and Daniel H. Loughlin. "Evaluating long-term emission impacts of large-scale electric vehicle deployment in the US using a human-Earth systems model." *Applied Energy* 300 (2021):117364. doi:10.1016/j.apenergy.2021.117364. 

* Ou, Yang, J. Jason West, Steven J. Smith, Christopher G. Nolte, and Daniel H. Loughlin. "Air pollution control strategies directly limiting future health damages in the US." *Nature Communications* 11 (2020): 957, doi:10.1038/s41467-020-14783-2.

* Babaee, Samaneh, Daniel H. Loughlin, and P. Ozge Kaplan. "Incorporating upstream emissions into electric sector nitrogen oxide reduction targets." *Cleaner Engineering and Technology* 1 (2020): doi:10.1016/j.clet.2020.100017. 

* Ou, Yang, Steven J. Smith, J. Jason West, Christopher G. Nolte, and Daniel H. Loughlin. "State-level drivers of future fine particulate mortality in the United States." *Environmental Research Letters* 14 (2019): 124071, doi:10.1088/1748-9326/ab59cb.

* Ou, Yang, Wenjing Shi, Steven J. Smith, Catherine M. Ledna, J. Jason West, Christopher G. Nolte, and Daniel H. Loughlin. "Estimating environmental co-benefits of U.S. GHG reduction pathways using the GCAM-USA Integrated Assessment Model." *Applied Energy* 216 (2018): 482-493, doi:10.1016.j.apenergy.2018.02.122.

* Shi, Wenjing, Yang Ou, Steven J. Smith, Catherine M. Ledna, Christopher G. Nolte, and Daniel H. Loughlin. "Projecting state-level air pollutant emissions using an integrated assessment model: GCAM-USA." *Applied Energy* 208 (2017): 511-521, doi:10.1016/j.apenergy.2017.09.122.

<h2 id="113-how-to-get-started-using-glimpse">1.13 How to get started using GLIMPSE</h2>

To install GLIMPSE, please follow the instructions described in the [GLIMPSE Installation Guide](~%20Getting%20Started.md#installation-instructions).

A good place to start in understanding GLIMPSE's underlying assumptions and capabilities is to read [Chapter 2](Chapter%202꞉%20Placeholder.md) of this Guide, which describes the GLIMPSE Reference Case. While key results are provided at the national level, similar results can be generated at the state level or for other countries.  

For those who would like to use GLIMPSE themselves, the GLIMPSE tutorials are the next step. These will take you through important steps of setting up, running, and analyzing scenarios. The tutorials are in the [Appendix](../../contents.md#tutorials) of the Users' Guide.

Once you have completed the tutorials, we recommend that you read [Chapter 3](Chapter%203꞉%20How%20does%20GCAM%20work%CA%94.md), "How does GCAM work?", in the Users' Guide. This will provide an overview of how GLIMPSE simulates markets in determining the market shares of technologies and fuels, as well as how markets are impacted by policies such as taxes and caps. 

Next, try to develop and evaluate your own policy scenarios, using the information in [Chapter 4](Chapter%204꞉%20How%20do%20I...%CA%94.md), "How do I?", and in [Chapter 6.4](Chapter%206꞉%20Reference.md#64-troubleshooting), "Troubleshooting", as needed. 

To develop a more detailed understanding of GCAM, please see the PNNL GCAM documentation: [http://jgcri.github.io/gcam-doc/user-guide.html](http://jgcri.github.io/gcam-doc/user-guide.html). 

Demonstrations and trainings are periodically available. Please inquire with Dan Loughlin at [Dan@En2MG.com](Dan@En2MG.com) and check the discussion section of https://github.com/DLoughlin/GLIMPSE-CE. 

<h2 id="114-where-to-get-assistance-or-provide-feedback">1.14 Where to get assistance or provide feedback</h2>

If you have difficulties installing or running GLIMPSE, please see the "[Troubleshooting](Chapter%206꞉%20Reference.md#64-troubleshooting)" section of this Users' Guide to see if your problem can be readily addressed. If the information there does not help solve your problem, please contact the GLIMPSE development team, either via the GitHub repository's "Issues" or "Discussion" areas or  by emailing Dan Loughlin at [Dan@En2MG.com](Dan@En2MG.com). Include detailed information about your problem, as well as what steps you have already taken to address it. Thank you\!

As this is only the second edition of GLIMPSE-CE and of its Users' Guide, there are undoubtedly many ways that both could be improved. Please send bug reports or feature suggestions through GitHub or by email for consideration in future updates. 

