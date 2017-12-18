# G Suite Automation Tool
A set of tools designed to facilitate the on-boarding of G Suite email  
users with the G Suite API  

Note: this was built with OS X in mind, but could be modified without too much  
trouble for other platforms since it is Java based

### Summary of common commands:
* Account creation `bash gsuite -p <path_to_csv> --create`
* Email group additions `bash gsuite -p <path_to_csv> --add`

Use the --help flag for help and information on commands  

## Installation & Build
### Necessary materials
* Super Admin level GSuite account
* `client_secret.json`
* gradle 3.5 or higher
* Java 1.8 (JRE 8) or higher

### Relevant G Suite articles
* [To see if G Suite API's are enabled](https://support.google.com/a/answer/60757?hl=en)
* [To research the Admin SDK API](https://developers.google.com/admin-sdk/)
* [To enable the Admin SDK API](https://console.developers.google.com/apis/library/admin.googleapis.com/)
* [To get the `client_secret.json` file](https://cloud.google.com/genomics/downloading-credentials-for-api-access)  
Note: client_secret not client_secrets

### Install
1. Clone this repo to desired location
2. Install gradle via homebrew
3. Download and install [JRE 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)
4. Either follow the build instructions at bottom or use my build script `bash install.sh`

## Usage
### Formatting file for Use
Create a UTF-8 encoded CSV file with format described [here](HELPME.md)

```
$ bash gsuite --help
Usage: bash gsuite [OPTIONS] [-p <PATH_TO_DATA_FILE>]

    [OPTIONS]
     -a --add --groups                Add members to a group. [REQUIRES PATH]
     -c --create                      Create new users. [REQUIRES PATH]
     -d --debug                       Print extra details for debugging.
     -n --dry                         Do a dryRun. Doesn't make any changes.
     -e --example                     Print out a line of the expected headers for imports.
     -? -h --help                     Print this message then exit.
     -lg --list-groups                List all groups in domain.
     -lu --list-users                 List all users in domain.
     -p --path                        Defines next arg as (full) path to UTF-8 csv formated data file.
     -r --reset                       Reset permission levels after changing scopes.
     -t --test                        Test method for new implementations.
     -u --update                      Update organization fields of users. [REQUIRES PATH]
     -v --verbose                     Turn on full output.
     For more help, see documentation in HELPME.md
```

## Contributions
Contributions are welcome and wanted.  
Keep in mind that you will need to add your own API token to use this software  
and should likely tailor it to fit your needs.  

To make changes, go to the build folder `build-gsuite/`  
If you see a bug or have questions, open an issue and I'll get back to you on it.

#### Explanation of Process
Input: CSV file with user data  
Output: Success/Failure report to run given command on each user  

##### Resource Locations
* Java source files in [build-gsuite/src/main/java](build-gsuite/src/main/java)  
* Files for testing are in [build-gsuite/src/test](build-gsuite/src/test)
* API Token is stored at [build-gsuite/src/main/resources/client_secret.json](build-gsuite/src/main/resources/client_secret.json)
* Credentials will be saved to the home directory during run-time

##### Feature Changes
* To manage dependencies for installing new/updated libraries,  
edit the `build.gradle` file.
* To add features to the main tool, edit the Java source files

##### Build Commands
* For a list of all gradle tasks: `gradle tasks`  
* To build/use the project: `gradle run`  
  With parameters: `gradle run -P myArgs="['--some-args','-i','-n',-this-format']"`  
* To run ~unit~ integration tests: `gradle test`  
  * Make sure you have a file to test on in `src/test/resources/testData.csv`  
* To make a clean slate before build: `gradle clean`  
* To create a new release:  
  1. Delete the old release (or keep it I guess?)  `rm -rf ../gsuite-#.#.#`   
  2. Update the version number in `build.gradle`  
  3. Assemble distribution: `gradle buildNeeded`  
     (you might only need `gradle distTar` to accomplish this step)  
  4. Move the build up to the repo and unpack it  
    `mv build/distributions/gsuite*.tar ..`  
    `tar -vxf ../gsuite*.tar`  
