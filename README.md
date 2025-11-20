# Zomboid Decompiler
Simplified decompilation tool for Project Zomboid powered by [Vineflower](https://github.com/Vineflower/vineflower).
## Usage
### Windows
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .zip from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Extract the zip.
4) Navigate to `bin/` and run `ZomboidDecompiler.bat`.
5) Wait a few minutes for decompilation to complete. The black box will close when the program has finished.  
   - If you receive an error about not being able to find the game directory, open your command line to the `bin` folder and execute ``ZomboidDecompiler.bat "PATH"``, replacing `PATH` with the path to your game installation's `ProjectZomboid` folder.
     - Example: ``ZomboidDecompiler.bat "D:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"``

The decompiled source code will be written to `output/`, along with the dependencies and game jar.

### Other
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .zip from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Extract the zip.
4) Open your command line to the `bin` folder and execute ``ZomboidDecompiler "PATH"``, replacing `PATH` with the path to your game installation's `ProjectZomboid` folder.
   - Example: ``ZomboidDecompiler "D:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"``
5) Wait a few minutes for decompilation to complete.
The decompiled source code will be written to `output/`, along with the dependencies and game jar.

## Features
- Single click game decompilation.
- Automatic gathering of game dependencies as decompilation context and for future recompilation.
- Renaming of function parameters using Rosetta data.
- Renaming of other variables according to type to enhance readability.
- Line number remapping for remote debugging.

## Command Line Interface
Launch with ``-h`` or ``--help`` for information about command line parameters.

## Remote debugging
A basic guide on using ZomboidDecompiler for remote debugging is hosted [here](https://github.com/demiurgeQuantified/PZModdingGuides/blob/main/guides/RemoteDebugging.md).

## Building
ZomboidDecompiler can be built with `gradlew build`.  
You can include Rosetta files in `src/main/resources/rosetta/` to be used as defaults when no rosetta directory is passed.
The standard binaries in Releases are built with the
[latest Rosetta data](https://github.com/PZ-Umbrella/pz-rosetta-source) included.
