# Java-Card-Applets

This repository contains a sample Wallet application for Java Card.

## Overview

The project consists of two main parts:
1.  **Applet:** The Java Card applet that runs on the card.
2.  **Client:** A host application to communicate with the applet.

The Wallet applet allows for basic operations like checking the balance, crediting, and debiting an amount.

## Project Structure

```
.
├── applet/             # The Java Card applet
│   ├── pom.xml
│   └── src/
└── client/             # The host client application
    ├── pom.xml
    └── src/
├── build.sh            # Script to build the applet and client
├── run.sh              # Script to run the client application
└── README.md
```

## Prerequisites

*   Java Development Kit (JDK)
*   Apache Maven
*   Java Card Development Kit Simulator

## How to Build

To build both the applet and the client, run the `build.sh` script:

```bash
./build.sh
```

This will compile the source code and create the necessary `.cap` file for the applet and a `.jar` file for the client.

## How to Run

To run the client application and interact with the applet in the simulator, use the `run.sh` script:

```bash
./run.sh
```

This script will start the simulator, load the applet, and run the client application.

