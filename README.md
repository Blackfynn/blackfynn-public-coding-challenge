# blackfynn-public-coding-challenge

## Introduction

In the United States, the privacy of patient data is governed by the Health
Insurance Portability and Accountability Act, or as it's commonly known, HIPAA.
One regulation falling under HIPAA is the Privacy Rule, which, among other
things, governs what health data can be shared and under what circumstances.
Additionally, to limit risk, when working with data internally, we follow the
minimum necessary standard for accessing data containing Protected Health
Information (PHI). To overcome these risks, we remove PHI, currently a set of 18
types of data including biometrics such as fingerprints, birth dates, telephone
numbers, social security numbers, etc. through a process known as 'Safe Harbor'
de-identification.

For this challenge, we're going to write a real-time de-identification
engine. This will be a simple API which takes in patient/participant records as
JSON, de-itentifies these records, and provides a mechanism for retrieving
individual or bulk records.

In this exercise, we will not implement de-identification methods for all types
of PHI, but focus on a few commonly used forms.

## Problem Statement

Write an API that exposes an endpoint for the de-identification of
patient/participant records. Specifically, we're looking for the following:

* Birthdates should be converted to the patient's age.  If someone is over the
  age of 89, they should be lumped into a 90+ category.

* ZIP Codes should be stripped to the first three digits _except_ where fewer
  than 20,000 people reside in the combination of all ZIP codes with those three
  digits. In this case, the ZIP Code should be set to 00000.  A file with ZIP
  codes and their populations is included in CSV format.  Note that these are
  Zip Code Tabulation Areas (ZCTAs) which exclude certain ZIP codes which are
  not useful for population data (i.e. some office buildings have their own ZIP
  codes due to mail volume, but are not considered for census tabulation). ZIP
  codes and associated cencus information are found in:

```
/src/main/resources/population_by_zcta_2010.csv
```

* Enrollement dates should be set to the year only.

* The notes section should replace anything that looks like an email address, US
  social security number, or a US telephone number with sensible replacements.
  Any dates in the notes section should be replaced with the year.


You can assume data is reasonably well formed (i.e. no one will try to pass a
date as a zip code) but the server should ideally not crash if it encounters an
edge case.

## Sample Inputs and Outputs

A sample input of:

```
{
    "name": "jane smith"
    "birthDate": "2000-01-01",
    "zipCode": "10013",
    "enrollementDate": "2019-03-12",
    "notes": "Participant with ssn 123-45-6789 previously presented under different ssn"
}
```

Should yield output of:

```
{
    "participantId": "1569698"
    "age": "20",
    "zipCode": "10000",
    "enrollementDate": "2019",
    "notes": "Participant with ssn XXX-XX-XXXX previously presented under different ssn"
}
```
## Implementation notes

To ensure a complete understanding of the scope of this exercies and to provide
ample amount of information, please see the below set of requirements.

 * Endpoint that ingests participant data into a in-memeory store (SQL-lite used
 in pre-defined framework) and returns newly minted participant id.
     * `POST /participants`
 * Method to de-identify data, removing PHI and appending a participant internal
 ID (7 digit integer))
 * Endpoint that hands retrieval of de-identified participant information based
 on internal ID
     * `GET /participants/:id`
 * Endpoint that retrieves all de-identified records in array
     * `GET /participants`

## Logistics

Because we are primarily a Scala developement group and understanding the
potential overhead to set up the service, database, etc., we've provided a base
REST service framework (derived from
https://developer.lightbend.com/guides/akka-http-quickstart-scala/).  Please
provide your solution as a ZIP file, or as a repository from any of the major
source control sites, but please keep repos containing your solution private.
The final submission should be aligned with what you might submit as a PR within
your team for reviews. Definitely reach out if you have any questions or need
any clarifications.


## Compiling and Testing

You will need a Java installation on your computer. Run `java -version` to check
if Java is installed. See
https://java.com/en/download/help/download_options.html for download options.

To start the [Scala Build Tool (SBT)](https://www.scala-sbt.org/) run

```
./sbt
```

Once in the SBT console, compile the project with the `compile` command. Run
tests with the `test` command.

You can start the REST API with the `reStart` command. Stop the server with the
`reStop` command.

Stop SBT with `Ctrl-C` .
