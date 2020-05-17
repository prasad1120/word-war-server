
# Word War: Server

[![GitHub release](https://img.shields.io/github/v/release/prasad1120/word-war-server.svg)](https://GitHub.com/prasad1120/word-war-server/releases/)

A multiplayer game using client-server architecture and sockets in which two players compete to 
find more number of English language words in a predefined time from a separate but similar 
4x4 board of letters based on their frequency in English.

 > Check out [word-war-client](https://github.com/prasad1120/word-war-client) for more details.
 
## Features
- [x] Board populated on the basis of letter frequency in English language
- [x] Score of opponent displayed in realtime
- [x] All possible words are displayed after the game
- [x] Rematch with same as well as fresh opponent

## Built with
- **Gradle** - used as build system

## Build and Run Process

- To run the Word War Server, in your top-level project directory, run the following command on terminal:
~~~
$ ./gradlew run
~~~
 > When running Word War Server, set port to required value in `Server.java`.

- To build an executable jar, run:
~~~
$ ./gradlew jar
~~~
    
- To build and run Word War Server jar, run:
~~~
$ ./gradlew runExecutableJar
~~~

 > When building an executable jar, set port to required value in `build.gradle`. 
The jar will get exported as `build/libs/word-war-server-<version>.jar`.

**Default values**:
```
port=8000
```
