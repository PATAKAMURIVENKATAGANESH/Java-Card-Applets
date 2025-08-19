# Java Card Wallet Applet

This repository contains a sample Wallet application for Java Card.

## Overview

The project consists of two main parts:
1.  **Applet:** The Java Card applet that runs on the card.
2.  **Client:** A host application to communicate with the applet.

## Applet Functionality

The Wallet applet simulates a simple electronic wallet on a Java Card. It supports basic financial operations and is protected by a PIN.

### APDU Commands

The applet communicates using APDU commands with a custom CLA byte of `0x80`. The supported instructions (INS bytes) are:

*   **`VERIFY` (0x20):** Verifies the user's PIN. The PIN must be successfully verified before any modification operations (credit/debit) can be performed.
*   **`CREDIT` (0x30):** Adds a specified amount to the wallet's balance. This operation requires prior PIN verification.
*   **`DEBIT` (0x40):** Subtracts a specified amount from the wallet's balance. This operation also requires prior PIN verification.
*   **`GET_BALANCE` (0x50):** Retrieves the current balance from the wallet. This is a public operation and does not require PIN verification.

### Security

*   **PIN Protection:** Access to critical functions like `CREDIT` and `DEBIT` is restricted. A user must first present the correct PIN using the `VERIFY` command.
*   **Try Limit:** The PIN is blocked after 3 incorrect verification attempts for security.

### Data Management

*   **Balance:** The applet maintains a balance that cannot exceed a maximum limit (`32767`).
*   **Transactions:** Both credit and debit transactions are capped at a maximum amount per transaction (`127`).

