# Zomboid Decompiler
Simplified decompilation tool for Project Zomboid powered by [Vineflower](https://github.com/Vineflower/vineflower).
## Usage
### Windows, Steam
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .jar from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Create a new folder and put the jar inside it.
4) Double click the jar to run it.
5) Wait a few minutes for decompilation to complete.
You should see `logs/` and `output/` folders created next to the jar to indicate the program is running.
If nothing happens after a few minutes, try the below instructions for Other instead.

### Other
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .jar from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Create a new folder and put the jar inside it.
4) Open your command line to that folder and execute ``java -jar ZomboidDecompiler.jar "PATH"``, replacing `PATH` with the path to your game installation's `ProjectZomboid` folder.
   - Example: ``java -jar ZomboidDecompiler.jar "D:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"``
5) Wait a few minutes for decompilation to complete.

## Features
- Single click game decompilation.
- Automatic gathering of game dependencies as decompilation context and for future recompilation.
- Renaming of function parameters using Rosetta data.
- Renaming of other variables according to type to enhance readability.

## Command Line Interface
Launch with ``-h`` or ``--help`` for information about command line parameters.

## Building
ZomboidDecompiler can be built with `gradlew build`.  
You can include Rosetta files in `src/main/resources/rosetta/` to be used as defaults when no rosetta directory is passed.
The standard binaries in Releases are built with the latest Rosetta included, but when building you must provide these yourself.
