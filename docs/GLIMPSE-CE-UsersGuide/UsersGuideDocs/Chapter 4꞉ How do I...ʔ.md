# Chapter 4꞉ How do I...ʔ

In this Chapter, we describe how to accomplish important activities in the *Scenario Builder*. 

<br>
<details open><summary><b>Sections</b></summary><br>

[4.1 How do I change options in the GLIMPSE option files?](#41-how-do-i-change-options-in-the-glimpse-options-files)

[4.2 How do I update the JAVA\_HOME environmental variable?](#42-how-do-i-update-the-java_home-environmental-variable)

[4.3 How do I create a new database?](#43-how-do-i-create-a-new-database)

[4.4 How do I import and export scenarios from the *ModelInterface*?](#44-how-do-i-import-and-export-scenarios-from-the-modelinterface)

[4.5 How do I manage database size?](#45-how-do-i-manage-database-size)

[4.6 How do I archive scenarios in GLIMPSE?](#46-how-do-i-archive-scenarios-in-glimpse)

[4.7 How do I access and interpret the main_log file?](#47-how-do-i-access-and-interpret-the-main_log-file)

[4.8 How do I edit a scenario's configuration file?](#48-how-do-i-edit-a-scenarios-configuration-file)

[4.9 How do I import scenarios into GLIMPSE?](#49-how-do-i-import-scenarios-into-glimpse)

[4.10 How do I import files into the *Component Library*?](#410-how-do-i-import-files-into-the-component-library)

[4.11 How do I recover deleted scenario components and scenarios?](#411-how-do-i-recover-deleted-scenario-components-and-scenarios)

[4.12 How do I save files associated with each run?](#412-how-do-i-save-files-associated-with-each-run)

[4.13 How do I clean up saved files?](#413-how-do-i-clean-up-saved-files)

[4.14 How do I use the CSVtoXML utility?](#414-how-do-i-use-the-csvtoxml-utility)

[4.15 How do I know if my computer's resources are running low?](#415-how-do-i-know-if-my-computers-resources-are-running-low)

[4.16 How do I determine why a GCAM run did not complete and GLIMPSE reports "DNF"?](#416-how-do-i-determine-why-a-gcam-run-did-not-complete-and-glimpse-reports-dnf)

[4.17 How do I access the GCAM data system and find the original sources of data?](#417-how-do-i-access-the-gcam-data-system-and-find-the-original-sources-of-data)

</details>

## 4.1 How do I change options in the GLIMPSE options files?

The "options_GCAM-USA-9.1.txt" file resides in the GLIMPSE-CE folder. This file lists many GLIMPSE options that are loaded when the program starts. In the installation instructions, we demonstrate how to modify "glimpseDir". Additional important options specified in the file include the name of the output database, "gCamOutputDatabase", and the number of model periods to execute, "stop-period". 

There are a few things to note in the options file: 

* Any line that starts with "\#" is interpreted as a comment.

* Where "\#glimpseDir\#" and "\#gCamGuiDir\#" appear to the right of an equal sign, the values of these parameters that are defined above are inserted. These are the only two parameters that are replaced in subsequent lines in this manner. For example, if you include "\#scenarioDir\#" to the right side of equals, it will not be replaced with the value of "scenarioDir".

Any changes that are made to the options file do not take effect until: (1) you restart GLIMPSE, or (2) you choose "File-\>Reload Options".

You also can access the options file from the *Scenario Builder* via "File-\>Edit Options", which will display the options file in a text editor. Any changes you make and save will not take effect until you choose "File-\>Reload Options" from the main menu bar or restart GLIMPSE. 

If you want to see the contents of the options file with the \#glimpseDir\# and \#gCamGuiDir\# parameters resolved, choose "File-\>Show Options", which will pop up a non-editable dialog window. 

Please note that changes you make to options such as the "stop-period" or the "gCamOutputDatabase" are not automatically reflected in scenarios that have already been created. These settings are reflected in scenarios that are created via the "Create Scenario" button, <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>, after the options file has been edited and re-loaded. 

## 4.2 How do I update the JAVA\_HOME environmental variable?

Periodically, your system administrators may update the version of Java on your computer. Depending on the setup, this may result in GCAM-USA no longer running. Typically, the command window will appear, then disappear, within a second. In addition, the *ModelInterface* may not run when you press the Results button. If you are experiencing these symptoms, the next step is to check your JAVA\_HOME setting, which lets GLIMPSE know where to find Java. You can do this using the following step: 

* Open your run\_GLIMPSE\_GCAM-USA-9.1.bat file by right-clicking on the file and choose to edit the file.

* Identify the JAVA\_HOME setting.

* Identify the location of the JRE folder on your computer. It will be similar to the JAVA\_HOME setting, but may have a different number appended.

* Update the JAVA\_HOME setting in the run\_GLIMPSE\_GCAM-USA-9.1.bat file to reflect the folder number.

* Save the run\_GLIMPSE\_GCAM-USA-9.1.bat file.

* Re-start GLIMPSE by double-clicking on the run\_GLIMPSE\_GCAM-USA-9.1.bat.

If this does not solve your problem, please see the Troubleshooting section of this Users' Guide. 

## 4.3 How do I create a new database?

GLIMPSE is shipped with a BaseX database named "database". 

You may want to create a new database if "database" is getting close to 40 GB in size. One way to do thise is to modify the database setting in the options file to provide a new database name (e.g., database\_v2). The current options file can be edited from the *Scenario Builder* File menu. After editing the name of the "gCamOutputDatabase" variable, re-load the options file or restart GLIMPSE. Then press the "Results" button. 

If the database does not exist, the following message will appear: 

<img src='..\UsersGuideGraphics\C4-1.png' title='Warning message associated with creating a new database'/>

 In general, you can answer "Yes". The new database will be created and placed in "GLIMPSE-CE-2.2\\GCAM-Model\\gcam-v9.1\\output". You will still need to update the database specified in the options file, then reload the options file, if you want this new database to be used in subsequent runs. 

Please note that each scenario's configuration file includes a specification of the database to use for its results. The database name is inserted at the time the scenario and its configuration file are created (e.g., when you press <img src='..\UsersGuideGraphics\create.png' style='height:16pt;'/>, then click "OK" in the "Creating Scenario" dialog). Providing a new database name in the options file does not change the database specified in any existing configuration files. You will need to either edit existing configuration files or re-create the scenarios if you would like them to use the new database setting. 

## 4.4 How do I import and export scenarios from the *ModelInterface*?

Users may have reasons to export scenario results from the *ModelInterface, such as*: 

* There are limits on the size of databases that the ModelInterface can open. In general, the limit is approximately 20 scenarios (or approximately 40 GB in total size). After the database reaches the limit, it can no longer be opened, and the data are inaccessible. Users thus may wish to maintain a smaller database that includes only results of particular interest.

* Users may wish to archive model results for specific scenarios that have been run.

* Users may wish to share scenario results with other GLIMPSE or GCAM users.

The *ModelInterface* includes features for importing and exporting scenarios that can address these needs. To export one or more scenarios from the *ModelInterface*, click the "Manage DB" button at the bottom of the Scenarios panel. The "Manage Database" window appears: 

<img src='..\UsersGuideGraphics\manageDatabaseDialog.png' title='The Manage Database dialog'/>


If you select a scenario from the list, you have the option to "Remove" it from the list, "Rename" it, or to "Export" the scenario. 

**It is particularly important to note that "Remove" currently removes the scenario name from the scenario list; however, it does \*not\* decrease the size of the database. Thus, this option does not solve the problem of a database getting too large. To rebuild the database with only the visible scenarios, use the "Rebuild DB" button. This will export the visible scenarios, delete the database, create a new one, and then import the visible scenarios back in.** 

Exporting a scenario writes the scenario's contents to an XML file. This XML file is generally too large to open in an editor. However, you can import the scenario into another database, thus facilitating sharing or keeping a smaller database of import model results. 

To import a scenario's XML file into a database via the *ModelInterface*, click the "Manage DB" button, then click on "Add". Note: Do not try to open a scenario's XML file via "File-\>Open-\>XML". This will not work, and your computer's hourglass will spin indefinitely. 

## 4.5 How do I manage database size?

When the GCAM database reaches approximately 40GB, the *ModelInterface* is no longer able to open the database or access its contents, and your data are effectively no longer available. 

### 4.5.1 Changing to a new database

To avoid this outcome, we suggest that you switch to a new database when :the database has less than 10% free space available. You can see the amount of remaining space on the Scenario Builder's status bar: 

<img src='..\UsersGuideGraphics\statusBar.png' title='The Scenario Builder status bar'/>

To switch to a new database, open GLIMPSE's options file via "File->Edit Current Options File". Find the database entry and change that entry to specify the name of your new database. Save the options file. To have this change take effect, you will need to restart GLIMPSE or choose "File->Reload Options File". 

Note that changing the database specified in the options file only is reflected in scenarios that are created after the change is made. 

### 4.5.2 Deleting scenarios from a database

A new option (added to GLIMPSE-CE 2.1) for managing database size is to delete scenarios from a database using the GLIMPSE-ModelInterface. To delete a scenario, follow all of the following steps:

* In the Scenario Builder, click on the name of a scenario that is in the database, then click the "Results-Selected" button <img src='..\UsersGuideGraphics\results-selected.png' style='height:16pt;'/>. This will start the ModelInterface, automatically loading the database with that scenario result.

* At the bottom of the "Scenarios" pane in the Model Interface, click on the "Manage DB" button. This will show the "Manage Database" dialog.

* Select the scenario you would like to delete and press "Remove". A confirmation message appears with additional instructions. Pressing "Yes" will remove the selected scenarios from the database.

*	The prior action removes the scenario from the list of scenarios, but does not decrease the database size. To decrease size, press the "Rebuild" button. Another confirmation dialog appears, and clicking "Yes" begins the rebuild process.

* Follow the gray status bar to observe the steps being taken. These include: exporting each scenario to be kept, creating a new database, and then importing each of those scenarios. The entire process may take several minutes. If the process is successful, the old version of the database is deleted. If an error occurs, the user is given the option to revert to the prior version.

### 4.5.3 Managing scenarios of interest

The ModelInterface's "Manage Database" dialog also includes several other options for managing scenarios, including Exporting and Adding scenarios. Exporting a scenario can be helpful if you would like to share the scenario with another GCAM user, if you want to use the ModelInterface features to compare scenarios that are in different databases, or if you want to group a set of scenario results together. 

To export a scenario, click on the scenario in the "Manage Database" dialog, then press "Export". The scenario is exported into a single XML file. These can be as large as 3 to 4 GB, but zip down to approximately 100 to 150 MB. In the "Edit->Preferences" dialog, there is an option to automatically zip exported scenario files. Click "Add" to add an unzipped scenario XML to your database. 


## 4.6 How do I archive scenarios in GLIMPSE?

When conducting studies with a model like GLIMPSE, repeatability is important. An archive feature has been added to GLIMPSE to support repeatability. If you click on a scenario name in the *Scenario Library* list, then choose "Tools-\>Archive Scenario" from the main menu bar, the following steps are carried out: 

* A dialog appears to confirm whether you would like to create an archive.

* If "Yes", the scenario's configuration file is parsed, and all of the scenario components listed in the file are identified (this includes from lines that are "commented out" via "\<\!—" and "--\>").

* These scenario components are copied to an "archive" subfolder within the scenario's folder. 

* If there are multiple scenario components with the same filename, a warning message appears and only the last file with that filename is copied. 

* A copy of the scenario's configuration file is created but modified to point to the scenario components within the "archive" folder instead of their original location.

* The archive folder is zipped (although the unzipped folder is also currently maintained). 

* This archive folder can also be found in the scenario's folder.

When the user selects a scenario name in the *Scenario Library* table and presses play, GLIMPSE first checks to see if an archive version of the configuration exists. If so, it asks whether the user would like to run the scenario from the archive. 

## 4.7 How do I access and interpret the main\_log file?

Most of the diagnostic information that is printed to the command terminal during GCAM execution is also saved to the main\_log.txt file that is in the "GCAM-Model\\gcam-v9.1\\exe\\logs" folder. This information can be extremely useful in diagnosing issues with the run, including identifying when during execution that problems occurred and in identifying which time periods had unsolved markets. For a run that is ongoing, you can access the "main\_log.txt" file easily in one of two ways. First, you can select the menu item "View-\>Current Main Log". The file will be displayed using the text editor that is specified in the options file. Alternatively, press the "Log" button, <img src='..\UsersGuideGraphics\log.png' style='height:16pt;'/>. 

By default, GLIMPSE saves the "main\_log.txt" file that is associated with each run after that run terminates. You can find a scenario's "main\_log.txt" file in the folder associated with the scenario via GLIMPSE's *Scenario Library*. To access the folder, click on a scenario, then click the "Browse" button, <img src='..\UsersGuideGraphics\open_folder1.png' style='height:16pt;'/>. Alternatively, you can click on the scenario's name in the *Scenario Library*, then click the "Log" button, <img src='..\UsersGuideGraphics\log-selected.png' style='height:16pt;'/>. 

## 4.8 How do I edit a scenario's configuration file?

You can edit a scenario's configuration file by double-clicking on the scenario name in the *Scenario Library* pane. This will display the configuration file in a text editor. Settings such as the output database or stop period can be edited and will be reflected when the scenario is executed. 

## 4.9 How do I import Scenarios into GLIMPSE?

Configuration files developed outside of GLIMPSE can be added as scenarios within the *Scenario Library*. To use this function, use "File-\>Import Scenario". Select the configuration file using the File Browser that appears. GLIMPSE will then create a new Scenario entry in your *Scenario Library* and will make a copy of the configuration file in that location. The scenario can be executed and archived as if it had been created in GLIMPSE. However, you will not be able to edit its components. 

## 4.10 How do I import files into the *Component Library*?

You can include XML add-on files in GLIMPSE in one of several ways. 

* Using the *New Scenario Component Creator*, you can use the XML List feature to "point" to one or more external XMLs. The resulting file list will be saved to the *Component Library*. Adding this scenario component to a scenario will result in the files in the file list being inserted into the scenario's configuration file when it is created.

* You can place XML files directly into the *Component Library* folder. The XMLs will appear in your *Component Library* and can be added to scenarios as you would add any other scenario component. 

## 4.11 How do I recover deleted Scenario Components and Scenarios?

You can view deleted scenario components and Scenarios by selecting "View-\>Browse Folder-\>GLIMPSE Trash Folder" via the main menu of the *Scenario Builder*. Deleted scenario component files are located within this trash folder, while deleted scenarios are subfolders within the trash folder. 

If you have not emptied the GLIMPSE trash folder, you can restore deleted scenario components by placing them back in the *Component Library* folder. Access that folder by clicking on the Open Folder button, <img src='..\UsersGuideGraphics\open_folder.png' style='height:16pt;'/>, in the *Component Library* section of the *Scenario Builder*.

Similarly, you can restore deleted scenarios by moving the sub-folder with the scenarios name from the trash folder to the "GLIMPSE Scenarios Folder". You can access the "GLIMPSE Scenarios Folder" via "View-\>Browse Folders-\>GLIMPSE Scenario Folder" in the main menu of the *Scenario Builder*. 

## 4.12 How do I save files associated with each run?

In the GCAM-USA output file, options\_GCAM-USA-9.1.txt, is an option "gCamOutputToSave", which is followed by a list of files to save for each run. Upon the completion of each run, these files are saved to the scenario folder. 

Modify the gCamOutputToSave parameter to change the files to be saved. After the values have been modified, select "File-\>Reload Options File" to update the settings in GLIMPSE. 

## 4.13 How do I clean up saved files?

By default, after every run, GLIMPSE will save a number of log files, including main\_log.txt, solver\_log.csv, calibration\_log.txt, as well as debug.xml (the files to be saved can be changed when creating a scenario). These logs can be large. For example, the solver\_log.csv file may be several hundred MBs. The main\_log.txt file should be saved. However, the other files are most useful for debugging, and it is not necessary to save them for future use. Selecting "Tools-\>Advanced-\>Cleanup Saved Files" will delete the unneeded log files. 

## 4.14 How do I use the CSVtoXML utility?

There are several approaches one can use to develop input files for GCAM. One approach involves integrating the new data and processing into the GCAM data system. This approach requires significant understanding of the data system structure and operation. Please see PNNL's github site for information about the datasystem, including an [online manual](https://github.com/JGCRI/gcamdata). See the next section for links to additional information on the data system. 

Most GLIMPSE users may not need to use the data system. Instead, these users can create "add-on" files that are formatted in XML. These add-on files could be created by hand, but the process can be tedious for complicated files. Alternatively, the *ModelInterface* (and *ModelInterface*) provide a "CSVtoXML" utility that can convert CSV-formatted files into XML files using rules provided in a header file. For more information on using the "CSVtoXML" utility, please see the instructions that are available in [Using the Model Interface to create XML files from CSV files.pdf](Misc/Using%20the%20Model%20Interface%20to%20create%20XML%20files%20from%20CSV%20files.pdf). 

## 4.15 How do I know if my computer's resources are running low?

When GLIMPSE starts up, several computing system properties are evaluated. Results are displayed in the *GLIMPSE Console*, including warnings if specific thresholds are not met. This analysis can be executed at any time by selecting "Tools-\>Check Installation" from the *Scenario Builder*'s main menu. Thresholds include: 

***Table 4.1 Thresholds used for evaluating computer resources.** Resources are compared to these thresholds at GLIMPSE startup and when "Tools-\>Check Installation" is selected.*

| Parameter | Threshold |
| :---- | :---- |
| Total physical memory (RAM) for GCAM | 12 GB |
| Total physical memory (RAM) for GCAM-USA | 16 GB |
| Free hard disk space  | 100 GB |
| Swap space size | 25 GB |

In addition, GLIMPSE checks computer resource status approximately every 20 seconds and updates the information in the status bar at the bottom of the *Scenario Builder*. Status shown includes CPU usage, total RAM, free RAM, total swap space, free swap space, the name of the current database, and its size and fraction of the maximum recommended size that has been used. 

These values are also compared against runtime thresholds: 

***Table 4.2 Runtime thresholds used for evaluating computer resources.** Resources are compared to these thresholds approximately every 20 seconds.*

| Parameter | Threshold |
| :---- | :---- |
| Free physical memory (RAM)  | 5% |
| Free swap space | 5% |
| Free hard disk space  | 40 GB |
| Free database size as a percentage of the max size listed in the options file | 80% |

When specific thresholds have been exceeded, the status message is concatenated with "\!\!\!" and asterisks are placed next to the values in exceedance. Additionally, this information is saved to a log file that can be accessed by "View-\>Resource Logs-\>Current Session". If GLIMPSE crashes and needs to be restarted, you will find information from the prior session at "View-\>Resource Logs-\>Prior Session". For each log entry there is a time stamp and the name of the currently executing scenario. This information can be useful in debugging execution problems that are caused by computer resource limitations. 

## 4.16 How do I determine why a GCAM run did not complete and GLIMPSE reports "DNF"?

There are several reasons why a GCAM run would not complete successfully. These include the following: 

* JAVA\_HOME is set incorrectly. GCAM relies on the Java virtual machine (JVM) for execution. The location of the JVM is defined by the JAVA\_HOME environmental variable in the run\_GLIMPSE\_GCAM-USA-9.1.bat. If JAVA\_HOME is set incorrectly, a gcam.exe window will appear then disappear nearly instantly. If you are using a computer that is managed by an administrator, one potential cause is that a new version of Java may have been installed on your computer, and the previous setting for JAVA\_HOME is no longer correct. Please follow the instructions in section 4.2, ["How do I update the JAVA\_HOME environmental variable?"](#42-how-do-i-update-the-java_home-environmental-variable) 

* There are errors in the formatting of the configuration file. Configuration files for GCAM are formatted in XML. If there are errors in the formatting of this file, such as the use of incorrect syntax, GCAM's XML parser will not be able to read the configuration file. Similar to when JAVA\_HOME is set incorrectly, the gcam.exe window will appear then immediately disappear. For the scenario with the problem, open the scenario's configuration file and check the formatting. Some text editors can color-code the content of XML files and can check the structure and format of the file, helping you identify if formatting is the problem. 

* There are errors in a scenario component file. When GCAM starts, the parser will indicate in the gcam.exe window when the process of reading in each scenario component begins. If the parser tries to load an incorrectly formatted scenario component file, or if the file does not exist, GCAM will terminate and the gcam.exe window will disappear. When this occurs, you can often examine the scenario's main\_log.txt file to find the last file that the parser attempted to load. This file typically is the one that caused the run failure. Make sure the referenced scenario component exists in the location at which it is referenced. If it does, check its format for errors.  

* Computer resources have been exceeded. Running out of available RAM, swap space, or disk space will result in your GCAM run being terminated abruptly, with no message or sign in the main\_log.txt file indicating the reason for termination. If this appears to be the case, please use the "View-\>Resource Logs-\>Current Session" option in the *Scenario Builder* main menu to see if there are any resource warnings that were reported around the time the problem occurred. If the information in the log indicates that disk space or swap space were limited, consider taking actions to free up hard disk space. If the warning indicates that RAM was the issue, then you may need to revisit the policies represented in your scenario. For example, market share constraints such as Renewable Portfolio Standards can be particularly memory-intensive. You may want to consider alternative approaches for simulating these types of policies, such as via the "Tech Bounds" or "Tech Avail" scenario components, or by adjusting shareweights. 

* There are naming conflicts for markets or policies. GCAM users have considerable flexibility in naming markets and policies. However, if the same name is used for more than one market or for more than one policy, this can create conflicts that may cause the model to terminate abruptly during the model run. GLIMPSE attempts to address this by assigning unique names to markets and policies within scenario components that are created within the system. However, if you have imported or referenced scenario components created outside of GLIMPSE, there is the possibility that naming conflicts exist. Please check any add-on files constructed outside of GLIMPSE to determine if there are conflicts. 

* The scenario's gcam.exe file was closed during execution. Closing the gcam.exe window that is displaying text from the GCAM run will terminate the run. 

If the GCAM run completes but GLIMPSE is reporting problem markets, please see [5.3 Interpreting and debugging unsolved markets](Chapter%205꞉%20Advanced%20topics.md#53-interpreting-and-debugging-unsolved-market-information-in-the-main_logtxt-file). 


## 4.17 How do I access the GCAM data system and find the original sources of data?

The GCAM data system includes the underlying data used to generate the model's XML-formatted input files. You can access the data system at GCAM-Model/gcam-v9.1/input/gcamdata. This folder includes an xml folder that contains the model's input files, as well as many other folders that contain data and code that were used to create the inputs. 

Much of the input data can be found in the following subfolder: "GCAM-Model/gcam-v9.1/input/gcamdata/inst/extdata". Within this folder are subfolders that include data for the various human-Earth systems represented in GCAM. These include: 

* aglu (agriculture and land use files)

* emissions 

* energy 

* socioeconomics

* water

In addition, there is a subfolder named "gcam-usa" for GCAM-USA-specific data. 
