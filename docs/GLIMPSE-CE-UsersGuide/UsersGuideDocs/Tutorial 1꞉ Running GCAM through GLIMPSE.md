# Tutorial 1꞉ Running GCAM through GLIMPSE

*A note: these tutorials have been created using GLIMPSE-CE v2.03 featuring GCAM v8.2, but, aside from version numbers, they should still be mostly accurate for the latest release.*

## T1.1 Overview  
The purpose of this tutorial is to walk new GLIMPSE users through the steps of starting GLIMPSE, executing GCAM-USA for the "GLIMPSE-8.2-Ref" scenario, and monitoring progress. In Tutorial 2, users will evaluate the results of the simulation.

The reference material in [Chapter 6](Chapter%206꞉%20Reference.md) of this Users' Guide may be useful as you carrying out the steps of the Tutorial. Chapter 6 includes descriptions of GLIMPSE's buttons and menu options, brief descriptions of the types of Scenario Components that can be created and the options for each, descriptions of the scenario components that are included in the Component Library, descriptions of commonly-used queries, information for troubleshooting, and a [Glossary](Chapter%206꞉%20Reference.md#65-glossary). 

## T1.2 Opening the GLIMPSE software
Open Windows Explorer. In the "GLIMPSE-CE-X.X" folder, double click "run_GLIMPSE_GCAM-USA-8.2.bat". 

<img src='..\UsersGuideGraphics\T1-1.png' title='Contents of the main GLIMPSE folder'/>

This will start the *Scenario Builder*, using the options specified in the file "options_GCAM-USA-8.2.txt". A security warning may pop up; if so, click "Run" to start the program. It may take a few minutes for the *GLIMPSE Scenario Builder* graphical user interface to appear.

<img src='..\UsersGuideGraphics\T1-2.png' title='The Scenario Builder'/>

The *Scenario Builder* consists of three panes. In the top-left pane is the scenario "Component Library", which contains a list of scenario components that have been previously created. A scenario can be created by adding one or more components from the library to the "Create Scenario" pane on the top right. Scenarios that have been created are listed in the *Scenario Library* table at the bottom.

To view diagnostic information about your GLIMPSE setup and computer resources, use the "console" button, <img src='..\UsersGuideGraphics\console.png' style='height:16pt;'/>, to open the GLIMPSE console. 

<img src='..\UsersGuideGraphics\T1-3.png' title='Diagnostic information for the session displayed to the GLIMPSE Console'/>

The *Scenario Library* table lists the date and time that the scenario was created, the date and time that its execution was completed, the status of the run, the modeled time periods that experienced solution errors (if any), and the total runtime for any runs that have completed. Options for status include blank (indicating the run has not been started), "In queue" to be run, "Running", "Success", "DNF" (an abbreviation for "Did Not Finish"), and "Unsolved mkts". 


The GLIMPSE-CE-2.03 distribution includes two scenarios. The first is "GLIMPSE-8.2-Ref", a scenario calibrated to 2021 with no additional policies. More information is available in the [GCAM documentation](https://jgcri.github.io/gcam-doc/gcam-usa.html). The second scenario included with this distribution is "GLIMPSE-8.2-NZ", which implements a net zero CO<sub>2</sub> target as derived from the [EMF 37 intermodel comparison exercise](https://emf.stanford.edu/emf-37-deep-decarbonization-high-electrification-scenarios-north-america). 

You can see which components were included in each scenario by hovering the mouse over its entry in the table (it may be necessary to click on a scenario first). A tooltip appears, listing the scenario's database location and any additional components. "GLIMPSE-8.2-Ref" displays only the database location, while "GLIMPSE-8.2-NZ" includes one scenario component: "Policy-CO2-Cap-netZero-after-land-sink-2025start.csv". This component imposes a US economy-wide CO<sub>2</sub> target that linearly transitions from 1310 MTC (million metric tonnes of carbon) in 2021 to 211 MTC in 2050. This final value does not reach zero since the study assumed that there are an additional 211 MTC that can be reduced through agricultural, forestry, and land use measures.


A scenario's configuration file indicates options such as which input files are included, the name of the database where outputs will be sent, how many model periods to execute, and whether to generate a detailed "debug" file. To view the configuration file associated with a scenario, double-click on the scenario's name in the *Scenario Library*. For example, double-clicking on "GLIMPSE-8.2-Ref" displays the following file, using the text editor specified in your options file. Alternatively, you can select a scenario, then click on the "edit" button, <img src='..\UsersGuideGraphics\edit1.png' style='height:16pt;'/>, which will open the configuration file(s) associated with the selected scenario(s).


<img src='..\UsersGuideGraphics\T1-4.png' title='The configuration file for the GLIMPSE-8.2-Ref scenario'/>

You may want to use an XML editor when viewing or editing XML files. For example, Notepad++ color codes the XML code, including marking comments as green, tags as blue, attributes in red, and attribute values in purple; Jupyter (above) uses teal for comments, green for tags, blue for attributes, and red for attribute values. The programs to use for opening ".txt" and ".xml" files can be specified in GLIMPSE's options file. 

If your configuration file did not appear, your XML editor's path may be specified incorrectly in your options file. You can correct this setting in the options file, then choose "File->Reload Options" in the Scenario Builder menu to adopt the change.


The GCAM Users Guide provides information about the contents and sections of the configuration file: [GCAM Documentation](https://jgcri.github.io/gcam-doc/). For scenarios that are constructed in GLIMPSE, meta-data is added the top of the configuration file, surrounded by XML comment indicators "\<\!--" and "--\>". The meta-data indicates the scenario name, output database, end year, and the scenario components from the Component Library that were included in the run.

## T1.3 Executing the GLIMPSE Reference Scenario

To execute the GLIMPSE Reference Scenario, "GLIMPSE-8.2-Ref", select it in the Scenario Library, then press the "play" button, <img src='..\UsersGuideGraphics\play.png' style='height:16pt;'/>. The scenarios status will immediately change to "In queue". If the queue is empty, the status will change to "Running" within a few seconds. 

To view GCAM outputs, use the "console" button, <img src='..\UsersGuideGraphics\console.png' style='height:16pt;'/>, to open the GLIMPSE console, then use the "GCAM" button to navigate to the relevant window. This status window displays diagnostic information for the run, including which input files have been imported, as well as warnings and errors. By default, there are many warnings reported to this window that can be ignored. Critical problems typically are reported as "Errors", "Critical Errors". Please see the [Troubleshooting](Chapter%206꞉%20Reference.md#64-troubleshooting) section if problems occur as the model executes.

<img src='..\UsersGuideGraphics\T1-5.png' title='Diagnostic information displayed while a scenario is executing'/>

Additional diagnostic information is also printed to the console window. For each model period, these include the number of solver iterations within that period, the total solver iterations, and the number of solved markets at that time. 

<img src='..\UsersGuideGraphics\T1-6.png' title='Additional diagnostic information'/>

This information can be useful in determining that the model is actively running and in gauging the progress that it is making. The solved market number tends to increase rapidly, then progress tapers off. Occasionally the number of solved markets will decrease as the solver adjusts its solution approach. The solver configuration file includes a parameter that specifies the maximum number of iterations allowed in each model time period. In the default GLIMPSE setup, this maximum is 8,000 for most time periods. Thus, the value following "iterations" can provide some indication of the maximum number of remaining iterations in that period. When the iteration limit is reached but unsolved markets remain, information about those markets is written to the status window. Please see the Users' Guide for help with [interpreting information on unsolved markets](Chapter%205꞉%20Advanced%20topics.md#53-interpreting-and-debugging-unsolved-market-information-in-the-main_logtxt-file). The GCAM documentation includes a detailed description of [the solver and its parametrization](https://jgcri.github.io/gcam-doc/solver.html) as well as [debugging](https://jgcri.github.io/gcam-doc/dev-guide/debug.html).

At the run continues, the information in the Scenario Table is periodically updated. For example, "Running (6)" indicates that GCAM is currently running and that the model is in the sixth period. The status bar at the bottom of the Scenario Builder window shows current computer resources and utilization. For many of these metrics there are thresholds beyond which problems may occur. When specific thresholds have been exceeded, the status message is concatenated with "!!!". Additionally, this information is saved to a log file that can be accessed by "View->Resource Logs->Current Session". This information can be useful in debugging execution problems that are caused by computer resource limitations. 

Most of the information written to the status window is also saved to the "main_log.txt" file, which is in the "GCAM-Model/gcam-8.2/exe/logs" folder and can also be accessed with the "main_log" button, <img src='..\UsersGuideGraphics\log.png' style='height:16pt;'/>.

When the run completes, several sequential steps occur:
- The results are written to the output database. <span style='color:red'>Note that if the *ModelInterface* is currently open and viewing the output database, then this step cannot proceed.</span> A message appears in the gcam.exe window asking that the *ModelInterface* be closed. Once it is closed, the process of writing results to the database begins. 
- Several files are moved to the scenario's folder, e.g. "GLIMPSE-Data/GCAM-USA-8.2/ScenarioFolders/GLIMPSE-8.2-Ref", including the main_log.txt file and other files that are specified via the "gCamOutputToSave" option in the options file. The saved files may include the following: 
	- calibration_log.txt – diagnostic information from GCAM's calibration process in which the model determines shareweights based on real-world, calibration-year data 
	- debug.xml – detailed information provided for a single region or state
	- solver_log.csv – information on market prices and solution status at each iteration of GCAM's solver
-	The main_log.txt file is parsed by GLIMPSE to ascertain whether the run completed successfully and whether there were any solution periods with unsolved markets.
-	The scenario's status is updated in the Scenario Library to "Success", "DNF" (Did Not Finish), or "Unsolved mkts" (Unsolved Markets). 
-	If there were periods with unsolved markets, those are listed in the "ProbMkts" (Problem Markets) column. 
-	The scenario's runtime is listed in the table.

<img src='..\UsersGuideGraphics\T1-7.png' title='Scenario Builder indicating successful completion of GLIMPSE-8.2-Ref'/>

Runtime for GLIMPSE-8.2-Ref can vary significantly from one computer to another, depending on processing speed, available RAM, disk speed, and the types of policies included in the run. You should expect this run to require 30 minutes to 5 hours. For the laptop used in constructing this tutorial (vintage 2023 with 32 GB of RAM), execution of GLIMPSE-8.2-Ref required 33 minutes. 

## T1.4 Examining information saved with each run
To access the folder associated with a scenario, click on the scenario's name in the Scenario Library, then press the open folder button above the table, <img src='..\UsersGuideGraphics\open_folder1.png' style='height:16pt;'/>. In addition to the saved files listed in the previous section, the scenario folder also includes the scenario's configuration file and input files that were developed in the creation of the scenario. 

For GLIMPSE-8.2-Ref, the contents of the scenario folder are shown below.

<img src='..\UsersGuideGraphics\T1-8.png' title="Files saved to the scenario's folder"/>

As a shortcut to access a scenario's main_log.txt file, you can click on the scenario's name in the Scenario Library, then click on the "selected main_log" button, <img src='..\UsersGuideGraphics\log-selected.png' style='height:16pt;'/>. The arrow on the button indicates that the main_log.txt file(s) for the selected scenario(s) will be opened in a text editor.

Users generally will not need to visit a scenario's folder. However, the "debug.xml" and "solver_log.csv" files may be useful to advanced users in diagnosing problems with GCAM execution. Users can modify which outputs are saved with each run by changing the "gCamOutputToSave" settings in the GLIMPSE options file. Alternatively, when a scenario is created via the create scenario button, <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>, the user has the opportunity to override these settings.

Scenario results are saved in an output database, which is accessed via the *ModelInterface*.

[Part 2](Tutorial%202꞉%20Examining%20model%20results.md) of the tutorial discusses how to examine model results.


