# Chapter 5꞉ Advanced topics

In this chapter, we provide information about advanced concepts and operations, such as understanding how GLIMPSE interacts with GCAM-USA, debugging model solution errors, and interpreting biomass flows. 

<br>
<details open><summary><b>Sections</b></summary><br>

[5.1 What is happening behind the scenes in GLIMPSE…?](#51-what-is-happening-behind-the-scenes-in-glimpse)

[5.2 GLIMPSE folder structure](#52-glimpse-folder-structure)

[5.3 Interpreting and debugging unsolved market information in the main_log.txt file](#53-interpreting-and-debugging-unsolved-market-information-in-the-main_logtxt-file)

[5.4 Biomass flows and CO<sub>2</sub> accounting in GCAM-USA](#54-biomass-flows-and-co2-accounting-in-gcam-usa)

[5.5 Considerations when modeling a deep decarbonization scenario](#55-considerations-when-modeling-a-deep-decarbonization-scenario)

</details>

## 5.1 What is happening behind the scenes in GLIMPSE…?
In this section, we describe actions taken by GLIMPSE when you perform various functions using GLIMPSE.

### 5.1.1 … when you create a new Scenario Component? 
When you press "Save" in the *New Scenario Component Creator* window, what happens next depends on what type of scenario component you are creating. If you are creating an XML list (e.g., a text file that points to one or more XML files), GLIMPSE prompts you for the file name, then saves the resulting file to the *Component Library* or another location of your choice. For GLIMPSE to "see" the new Scenario Component, you will need to place it in the *Component Library* or a subfolder of that library.

If you are creating one of the other types of Scenario Components, the first step that occurs is a quality assurance step. GLIMPSE checks to see if all of the necessary options have been selected. If not, a warning message is provided. Otherwise, you are prompted for a filename. At this point, GLIMPSE scans all of the policy names and market names for the CSV-formatted Scenario Components. It chooses unique names when writing the current file to disk to avoid naming conflicts with existing policy representations. This action will produce a CSV file, formatted to match one or more headers in the GLIMPSE header file: "glimpseXMLHeaders_v9.1.txt". Note that later, when the scenario is created, its CSV files will be converted to XML files automatically using these headers. 

### 5.1.2 … when you construct a new scenario? 
When you press the Create Scenario button, <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>, GLIMPSE first checks to see if this scenario already exists. It does so by checking if there is a subfolder in the *Scenario Library* folder with the same name. If there is, the user is prompted to determine if they wish to continue. Next, GLIMPSE checks the size of the database in which the scenario's outputs are to be saved. If the database exceeds a specific size (default is 40 GB, although alternatives can be specified in the options file), a warning is presented, and, again, the user has the option of continuing or canceling. Next, a dialog appears. It lists the scenario name, the database to which the results will be stored, and the size of that database. In addition, the user is given the options of whether to generate a debug file (recommended) and which region to use for the debug region. The option is available to change the final year of the run, and a comment area is provided for the user to leave any text that describes the scenario.

When you click "OK", the scenario's folder is created. Next, the Scenario Template file is accessed (typically, configuration_GCAM-USA-9.1_template.xml). Metadata is written to the top of the file, then the database, stop-year, and debug information are updated to reflect what the user has selected. 

Next, GCAM goes through the Scenario Components that have been selected for inclusion. When it reads an XML list file, it places references to each of the indicated XML files at the bottom of the Scenario Components block within the configuration files. When it reads an XML file, it places a reference to that file into the same location. When it reads a CSV file, it identifies the matching headers in the glimpseXMLHeaders_v9.1.txt file, then uses the CSV2XML.jar utility to convert the CSV file into an XML. The resulting XML is automatically placed in the new scenario folder, and a reference to it is placed in the configuration file. Finally, the edited configuration file is renamed to reflect the scenario's name and placed in the scenario's folder. 

### 5.1.3 … when you run a scenario? 

Selecting one or more scenarios in the Scenario Library, then clicking "Play", <img src='..\UsersGuideGraphics\play.png' style='height:16pt;'/>, adds those scenarios to the run queue. GCAM runs are made sequentially from the queue, on a first-in first-out basis. When running each scenario, GLIMPSE calls the GCAM executable identified in the options file, using the "-C" argument to use that scenarios configuration file. When a run has finished, whether successful or not, the "main_log.txt" file, several other log files, and the debug file are copied to the scenario's folder. 

Every 20 seconds, GLIMPSE updates the information about each scenario in the Scenario Library. It does this by traversing the GCAM-Data/GCAM-USA/ScenarioFolders folder. For each scenario's folder within, it searches for the main_log.txt file. If that file exists, then it is searched for the text "Model completed successfully", "Model periods not solved", and for the runtime. If no main_log.txt file exists, GLIMPSE checks to see which run is currently executing by checking the current main_log.txt file in the exe folder. Based on the results, GLIMPSE assigns status as "Running", "Success", "PblmMrkts", "DNF", or blank. PrbmMrkts indicates that there were market errors in one or more time periods. DNF stands for Did Not Finish. 

## 5.2 GLIMPSE folder structure

In this section, we describe some of the most important folders in GLIMPSE. 

* **GLIMPSE** \- The root folder for GLIMPSE. This may be different, depending on your installation. 

The following folders contain the GCAM model and data. 

* **GLIMPSE/GCAM-Model/gcam-v9.1 -** The root folder for a particular version of GCAM 9.1. 

* **GLIMPSE/GCAM-Model/gcam-v9.1/exe -** Includes the GCAM executable (gcam.exe) and example configuration files (e.g., configuration\_ref.xml and configuration\_usa.xml). 

* **GLIMPSE/GCAM-Model/gcam-v9.1/exe/logs -** Includes log files that are created during GCAM calibration and execution, such as main\_log.txt, calibration\_log.txt, and solver\_log.csv. 

* **GLIMPSE/GCAM-Model/gcam-v9.1/input -** Includes folders with input data used by GCAM or the GCAM data system.

* **GLIMPSE/GCAM-Model/gcam-v9.1/input/policy -** Includes example policy files for GCAM (but not for GCAM-USA), including files that impose carbon taxes and that implement various global warming targets.

* **GLIMPSE/GCAM-Model/gcam-v9.1/input/gcamdata -** Includes the gcam data system, an R-based system that creates XML-formatted input files for GCAM from CSV-formatted source data.

* **GLIMPSE/GCAM-Model/gcam-v9.1/input/gcamdata/xml -** Includes the xml-formatted input files that are produced by the GCAM data system and read in by GCAM as scenario components.

* **GLIMPSE/GCAM-Model/gcam-v9.1/input/gcamdata/inst/extdata -** Includes the CSV-formatted source data. The CSV files have metadata identifying the original source of data tables.

* **GLIMPSE/GCAM-Model/gcam-v9.1/output -**  Includes GCAM's output databases. Each database is a subfolder that includes seven files with the ".basex" extension. These are binary files in which GCAM's XML-formatted outputs are stored.

GLIMPSE folders: 

* **GLIMPSE/Docs** \- Includes GLIMPSE documentation, such as this Users' Guide.

* **GLIMPSE/GLIMPSE-Data/GCAM-USA/ScenarioComponents** \- Includes Scenario Components available to GLIMPSE. These can be XML add-on files, text files that point to one or more external XML files, or CSV files that are formatted specifically for use by GLIMPSE. 

* **GLIMPSE/GLIMPSE-Data/GCAM-USA/ScenarioFolders** \- Includes folders for each scenario in the GLIMPSE *Scenario Library*. These folders are where the scenario's log files are stored after each run, as well as where any scenario-specific XML files are stored. If the scenario is archived, the copies of all scenario components will be placed in a subfolder named "archive". 

* **GLIMPSE/GLIMPSE-Data/GCAM-USA/trash** \- Includes deleted Scenario Component files and Scenario folders. To restore from trash, move deleted components and scenarios back into "ScenarioComponent" and "ScenarioFolders", respectively. 

* **GLIMPSE/GLIMPSE-GUI** \- Includes all of the files associated with the GLIMPSE graphical user interface (GUI)

* **GLIMPSE/GLIMPSE-GUI/exe \-** Includes ScenarioBuilder.jar, the GLIMPSE GUI executable file.

* **GLIMPSE/GLIMPSE-GUI/templates** \- Includes files that are used as templates by GLIMPSE, such as the template configuration file used when constructing new scenarios.

* **GLIMPSE/GLIMPSE-GUI/resources** \- Includes ancillary files used by GLIMPSE, such as button images and the technology listing that is used the *New Scenario Component Creator* window. 

* **GLIMPSE/GLIMPSE-ModelInterface** \- Includes a modified version of PNNL's *ModelInterface*, which adds features for filtering, graphing, and exporting data.

* **GLIMPSE/GLIMPSE-ModelInterface/exe** \- Includes ORDModelInterface.jar, the executable used to run the program, as well as the modelInterface.properties file that has start-up settings and the Main\_queries\_GLIMPSE.xml file that includes the queries used by GLIMPSE. 

## 5.3 Interpreting and debugging unsolved market information in the main\_log.txt file

The GCAM solver works by attempting to identify the price in each market at which supply equals demand. When GCAM experiences an unsolved market, information about that market is reported to the "main\_log.txt" file. Here is an example of such a report: 

<img src='..\UsersGuideGraphics\C5-1.png' title='Example of the table used to report market solution errors'/>

Parameters of relevance are defined in the table below.

*Table 5.1 Parameters relevant to solver operations*

| Parameter | Description |
| :---- | :---- |
| X | The current guess for the price that will solve the market. |
| XL | XL and XR represent the previous two guesses, with XL being the lower of the two values.  |
| XR | XR is the upper value of the prior two guesses. |
| ED | At price X, ED represents the excess demand, or Demand minus Supply. |
| EDL | At price XL, EDL represents the excess demand, or Demand minus Supply. |
| EDR | At price XLR, EDR represents the excess demand, or Demand minus Supply. |
| RED | Relative excess demand (RED) is the absolute value of ED divided by demand. |
| Supply | The amount of Supply at price X.  |
| Demand | The amount of Demand at price X. |
| max-model-calcs | The maximum number of solver calculations that will be conducted in a model period, specified in the solver configuration file. |
| solution-tolerance | A threshold against which the RED is compared to determine whether the market has been solved, specified in the solver configuration file. |
| solution-floor | A threshold against which the absolute value of ED is compared to determine whether the market has been solved, specified in the solver configuration file.  |

The process GCAM's solver uses to determine the "market clearing" price is similar to the familiar "pick a number between 1 and 100" game. An initial set of guesses for X are made, with XL being the lower of the two values and XR being the higher. Excess demands are calculated for both XL and XR, providing EDL and EDR. These values are used to determine the next guess for X. This value of X then displaces either XL or XR, new values for EDL and EDR are calculated, etc.

The search for the best value for X ends when one of three stopping conditions is met:

* The calculation limit, max-model-calcs, has been exceeded 

* The value of RED at X falls below the specified solution-tolerance

* The value of |ED| at X falls below the specified solution-floor

The second and third criteria are similar, but the normalization that occurs when evaluating RED is useful evaluating convergence in small markets. The solution-floor is typically smaller than the solution-tolerance and can also be interpreted as the difference between supply and demand being so small that it can be interpreted as being zero. 

If the calculation limit is reached, but not all markets have been solved, the markets that have not been solved are reported to the "main_log.txt" file. 

For the example report shown above, there are 11 market failures reported in time period 7 of the model, 2030. The columns are not aligned well, but the fields in each row are separated by commas, and the proper column for each value can be readily deduced. See Table 1 in this section for a brief description of the parameters in the table. 

Here, the supply and demand for each market are quite similar and are often different at the 4th or 5th decimal point. This is evident in the RED column, where many of the values are small. The solution-tolerance for this run was 0.0001, so many of these markets were close to having been solved. One approach to address very small market errors is to relax the tolerance in the solver's configuration. Alternatively, if these markets would have met the relaxed tolerance, it may be possible to assume they have been solved. If this latter approach is taken, it is suggested that the modeler carefully inspect model results for that time period for discontinuities or other unexpected behavior. 

In contrast, in the error table, the RED value for the "USArefining" market indicates that supply and demand were different by 11%. That is a relatively large difference in an important market. Additional examination is necessary, including evaluating whether any constraints in the model make the solution infeasible. 

There are other causes of unsolved markets. In some instances, a particular policy or technology may have introduced complexities that are resulting in the need for more computations to determine the market-clearing prices. Increasing the max-model-calcs in the solver configuration file allows the search to perform additional iterations. Modifying solution-tolerance and solution-floor may also provide some benefit, although changing these values impacts the solution process and may not lead to the desired results. 

Some markets may also be unsolvable or difficult to solve because of numerical issues involving very low demands or extremely high prices, producing conditions that are incompatible with the scaling approach used by the solver. State-level constraints on transportation technologies are particularly problematic because of the unit conversions used specifically for that sector within the model. In the solver configuration included in GLIMPSE, we have lowered the solution-tolerance and solution-floor from their default values, which has addressed many of these instances. 

For some of these markets that suffer from numerical issues, the multi-threading capability of GCAM may exacerbate these errors by introducing additional small numerical errors. For scenarios experiencing errors in small markets, turning off multi-threading may solve the problem at the expense of a longer runtime. This option is available in the dialog window that appears when you click on the "Create Scenario" button, <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>.

At this time, our goal is to provide some insights that may help a GLIMPSE user understand and recover from some market solution problems. As we seek to improve the solver settings and configuration to improve its robustness, this section will evolve. 

## 5.4 Biomass flows and CO<sub>2</sub> accounting in GCAM-USA 

As alluded to in the Tutorial, biomass has the potential to play a role in GHG mitigation strategies, including taxes and caps, renewable portfolio standards, and clean energy standards. In this subsection, we investigate CO<sub>2</sub> accounting associated with biomass as well as its use in the energy system. The information provided here was developed by analyzing the results of the "inputs by tech" and "output by tech" queries, which are highly recommended for assessing material flows in GCAM. 

There are several pathways by which biomass can enter a solution, as shown in the following figure.

<img src='..\UsersGuideGraphics\C5-2.png' title='Biomass and biofuel activities in GCAM-USA'/>

Biomass (e.g., fiber, grass, tree, root/tuber, sugar, and corn) for use in bioenergy and biofuels is "grown" in GCAM and GCAM-USA at the land-use resolution (e.g., water basins). Corn and oil crop can be used to create corn ethanol and biodiesel. Alternatively, these crops and other forms of biomass can enter states as "regional biomass", which, in turn, can feed applications in the electric sector and advanced biofuel production. In addition, regional biomass can be converted into "delivered biomass", which is used in the residential, commercial, and industrial sectors. 

From a CO<sub>2</sub> accounting standpoint in GCAM, combustion of biomass results in the release of CO<sub>2</sub> emissions (just as it does in the real world). To counter these releases, GCAM represents the CO<sub>2</sub> uptake from the atmosphere that is associated with biomass grown for bioenergy or biofuels. This uptake is accounted for at the state level (see "Up" on the biomass flow diagram), allowing bioenergy use and biofuel production to play a role in meeting an economy-wide CO<sub>2</sub> cap or for reducing costs under an economy-wide CO<sub>2</sub> tax. 

There are several limitations to this method of accounting. One limitation is that a state would not receive "credit" for reducing transportation emissions by increasing the biofuel content at the pump. Instead, the credit for that ethanol would occur in the state where the ethanol was produced. Furthermore, a cap on CO<sub>2</sub> emissions from the electric sector would not increase biomass use in that sector since the credit would be occurring in the state's "regional biomass" sector instead. 


## 5.5 Considerations when modeling a deep decarbonization scenario

A deep decarbonization scenario is one that achieves significant reductions in CO<sub>2</sub> emissions, often targeting an 80% or even 100% reduction by 2050, relative to a historical year such as 1990 or 2005. "Net-zero" strategies typically achieve zero CO<sub>2</sub> emissions through a combination of reducing emissions from the energy system, applying carbon capture to fossil- and biomass-fueled combustion activities, removing CO<sub>2</sub> from the atmosphere via direct air capture, and through changes in land use and farming practices.  

GLIMPSE can be used to explore pathways for achieving deep decarbonization scenarios. One approach is to introduce a declining CO<sub>2</sub> or GHG cap, allowing GCAM to select a technology pathway through time for achieving the target. Alternatively, GLIMPSE can be used to simulate sectoral mitigation strategies, including introducing market share targets for vehicle electrification and renewable portfolio standards for electricity production. 

For either approach, GLIMPSE users should be aware of how GCAM's myopic solution process, logit-based market share allocation, and shareweight assumptions affect the results (described in "Chapter 2. How does GCAM work?"). Depending on the scenarios being modeled, some adjustments may be desired or necessary, as discussed in this section. 

### 5.5.1 Potential adjustments for deep decarbonization scenarios

When simulating deep decarbonization strategies, users may want to consider making the following adjustments: 

* Including direct air capture (DAC) technologies. We have included DAC.txt Scenario Component that can be used to integrate DAC into decarbonization scenarios. 

* Modifying the shareweight trajectories of decarbonization technologies. In the Reference Case assumptions, many of the electric and hybrid transportation technologies have shareweights that begin at 0 and transition to 1 by 2050, 2070, or even 2100. A value of 1 indicates that all biases have been addressed such that the technology competes for market share based upon its cost (and the impact of any policies on technology costs). Under a deep decarbonization scenario, it is reasonable to assume that many of the barriers to hybrid, electric, and fuel cell technologies will be addressed sooner than in the Reference Case. For example, investments likely will be made to build out charging and hydrogen fuel infrastructure. Thus, it may be reasonable to modify the shareweight trajectories for these technologies to reach 1 sooner. We have created a DeepDecarbAssumptions.txt scenario component that includes revised shareweight trajectories for advanced transportation technologies; however, GLIMPSE users may opt to introduce their own assumptions instead. 

* Reducing the elasticities on end-use service demands. In GCAM, end-use service demands have price elasticities such that increases in cost lead to a reduction in demand. These elasticities have been developed with input based on past human behavior. However, a deep decarbonization scenario would diverge considerably from past conditions, and thus consumer choices and behaviors could be different as well. Using the default elasticities, common response in GCAM and GCAM-USA to deep decarbonization is significant reductions in service demands for historically difficult-to-decarbonize sectors, such as international aviation and cement production. When modeling decarbonization scenarios, we recommend that users examine the response of end-use demands and consider modifying the elasticities such that this is less demand response.

* Modifying the electric sector's coal plant retirement trajectory. GCAM and GCAM-USA include a "profit-shutdown-decider" function that drives retirement as operational costs increase. We have found that the parameterization of this function results in the model being unable to retire existing capacity quickly enough to meet some short-term decarbonization targets. As a result, users may choose to drive retirements via adjusting power plant lifetimes or by using the "Tech Bound" option in GLIMPSE. 

* Eliminating conventional technologies when using high market share constraints. Some deep decarbonization scenarios may involve representations of sector specific policies, such as electric vehicle sales reaching 100% or an RPS reaching 100% in a particular modeled year. When modeling such scenarios, users are advised to keep in mind how the logit function makes market share decisions in GCAM. The logit will assign market share based on relative costs, adjusted by technology-specific shareweights. In most instances, GCAM will assign a non-zero market share to all technologies that are competing in the market. As a result, market share constraints above 90% can be very challenging, often resulting in unsolved markets. This problem can be addressed by adjusting the shareweights of conventional technologies. For example, to meet a 100% EV target, in the years that target must be met, the shareweights for other technologies in that market can be set to zero, effectively removing them from the market. In GLIMPSE, the "Tech Avail" feature can be used for this purpose, and shareweight can also be modified via the "Tech Param" option. See Tutorial 5 for an example. 

### 5.5.2 Additional considerations

There are several additional aspects of deep decarbonization that should be considered. When interpreting the resulting mitigation strategy, it is important that users keep in mind the myopic nature of GCAM's solution process. GCAM solves each time step by optimizing technology and fuel choices based upon conditions within that time period only \- but with no knowledge of conditions in the future. Thus, GCAM will not make short-term decisions with long-term policy targets in mind, and this will impact the decarbonization pathway that is produced. 

Uncertainty is another important consideration. Assumptions about the cost and performance of decarbonization technologies typically are derived from a combination of peer-reviewed literature and government reports. Nonetheless, future technology cost and performance are very uncertain, particularly for technologies that have not yet been deployed commercially. Sensitivity analysis to explore alternative pathways can provide valuable insights into the roles that technologies may play and into the amount of flexibility available in meeting decarbonization targets.
