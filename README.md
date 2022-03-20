## Java Chat Program README

by Daniel Ranchpar and Arteen Galstyan

### RUNNING THE PROGRAM

It is recommended to install the latest JDK before running this program. Older Java versions should be compatible but are not guaranteed to work correctly with this program.

You MUST add the json-simple-1.1.1.jar file to the build path in order to run the program. This is possible in eclipse and vscode. Running the program on Command Prompt without this buildpath will result in the connect function to not work.

To launch the program, run the main Chat.java file and input the parameters (port number must be 4 digits). If you want to launch 2 chat programs on 1 computer with the same IP, then the initial parameter ports must both be different.

### CONTRIBUTIONS

Arteen Galstyan

- Conducted java.net socket research
- Implemented all the Threads
- Implemented the bulk of the server and socket code
- Implemented JSON helper libraries
- Implemented the List, Terminate, Send, and Exit functions
- Handled half of all major errors

Daniel Ranchpar

- Implemented researched java.net socket code
- Implemented the logic, control, and user input of the program
- Assisted in thread logic
- Implemented the Help, MyIP, MyPort, and Connect functions
- Handled half of all major errors
- Demo Video Editor
