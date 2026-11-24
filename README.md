# PayGuard

PayGuard is a Java console application that simulates digital payments between university-project accounts.
Producer threads submit transactions to a bounded queue, and consumer threads process them safely.
Accounts and transaction records are stored in SQLite with plain JDBC.
Stream-based fraud rules and CSV account import and transaction export complete the simulation.

## Features

- Create accounts and display account and transaction records.
- Run a payment simulation with two producers and two consumers.
- Keep account balance changes safe with ordered account locks and a single JDBC transaction per payment.
- Flag unusually large payments, rapid transactions, and repeated failed attempts.
- Import account data from CSV and export transaction reports to CSV.
- Print total balances before and after a simulation and join worker threads before returning to the menu.

## Package structure

```text
com.payguard
├── Main.java                 Console menu and simulation setup
├── ConcurrencyCheck.java     Two-consumer balance check
├── FraudCheck.java           Runnable fraud-rule example
├── model                     Account, Transaction, TransactionStatus
├── exception                 Payment and account exceptions
├── queue                     Bounded transaction queue
├── thread                    Producer and Consumer Runnable classes
├── dao                       SQLite connection and CRUD classes
├── fraud                     Stream-based fraud rules
└── io                        CSV import and export
```

## Build and run

Maven was not available in the verification environment (`mvn -v` was not recognized), so the project was compiled with `javac --release 17` and the SQLite driver downloaded into the ignored `lib/` folder. From PowerShell in the project directory, the verified compile command was:

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.54.1.0/sqlite-jdbc-3.54.1.0.jar' -OutFile 'lib/sqlite-jdbc-3.54.1.0.jar'
$sources = Get-ChildItem src/main/java -Recurse -Filter '*.java'
New-Item -ItemType Directory -Force target/classes | Out-Null
& 'C:\Program Files\Java\jdk-26.0.1\bin\javac.exe' --release 17 -cp 'lib/sqlite-jdbc-3.54.1.0.jar' -d target/classes @($sources.FullName)
```

The verified run command was executed with the current directory set to a fresh run folder, so SQLite created a fresh `payguard.db` there. Replace the project path if it is stored elsewhere:

```powershell
$project = 'C:\Users\ASUS\Desktop\payguard'
$classPath = "$project\target\classes;$project\lib\sqlite-jdbc-3.54.1.0.jar"
$runFolder = Join-Path $env:TEMP ('payguard-run-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runFolder | Out-Null
Push-Location $runFolder
& 'C:\Program Files\Java\jdk-26.0.1\bin\java.exe' --enable-native-access=ALL-UNNAMED -cp $classPath com.payguard.Main
Pop-Location
```

The SQLite JDBC driver is also declared in `pom.xml`. The application creates `payguard.db` in the current working directory on startup. To repeat the concurrency check five times, run from a fresh working folder (each run adds two accounts):

```powershell
1..5 | ForEach-Object { & 'C:\Program Files\Java\jdk-26.0.1\bin\java.exe' --enable-native-access=ALL-UNNAMED -cp $classPath com.payguard.ConcurrencyCheck }
```

Run the fraud example with:

```powershell
& 'C:\Program Files\Java\jdk-26.0.1\bin\java.exe' -cp $classPath com.payguard.FraudCheck
```

## CSV formats

Account imports use two columns: `holder_name,balance`. A header row is optional. Names containing commas must be quoted.

```csv
holder_name,balance
"Rao, Meera",250.50
```

The importer skips blank lines, reports invalid rows with their line numbers, continues after bad rows, and prints a summary.
Transaction exports contain `id,sender_account_id,receiver_account_id,amount,timestamp,status`.

## Fraud rules

The thresholds are constants in `FraudDetector`:

- `HIGH_AMOUNT_THRESHOLD = 10000.00`: flags a transaction strictly above this amount.
- `MAX_TXNS_IN_WINDOW = 3`: flags a transaction when its sender has more than three transactions in the trailing window.
- `WINDOW_SECONDS = 60`: sets the rapid-transaction window to 60 seconds.
- `MAX_FAILED_ATTEMPTS = 3`: flags failed transactions after a sender reaches three failed attempts.

## Current status

The source compiled successfully with `javac --release 17` using JDK 26, and the concurrency check produced one SUCCESS, one FAILED, and balances of 200.00 and 800.00 in all five runs. A scripted Main session completed account creation, simulation, account and transaction views, flagged view, CSV export, both CSV imports, and clean exit. Its reported simulation total stayed at 3000; a JDBC query confirmed all account and transaction rows, no negative balances, and `PRAGMA integrity_check` returned `ok`. Maven itself and an actual Java 17 runtime were not verified; runtime checks used JDK 26 with Java 17-targeted classes.

## Pending work / next milestone

- Generic `Repository<T>`
- Refund and reversal support
- Configurable fraud thresholds
- Summary reports
- JUnit tests

