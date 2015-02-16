# Krill

A Corpusdata Retrieval Index using Lucene for Look-Ups


## Synopsis

... TODO:
> Adding data (JSON via server)
> Querying data (KoralQuery)
> Show results (JSON)


## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora,
developed at the Institute for German Language (IDS) in Mannheim,
Germany.

**The software is in its early stages and not stable yet**


## Features

Krill is the reference implementation for
[KoralQuery](https://github.com/KorAP/Koral), covering
most of the protocols features, including ...


- **Fulltext search**<br>
  "Find all occurrences of the phrase 'sea monster'!"<br>
  "Find all case-insensitive words matching the regular expression /krak.*/"

- **Token-based annotation search**<br>
  "Find all plural nouns in accusative!"

- **Span-based annotation search**<br>
  "Find all nominal phrases!"

- **Distance search**<br>
  ...

- **Positional search**<br>
  ...

- **Nested queries**<br>
  ...

- **and many more ...**<br>
  Multiple annotation resources;
  Virtual Collections;
  Partial highlightings;
  Support for overlapping spans;
  Relational queries;
  Hierarchical queries ...


## Prerequisites

At least Java 7,
[Git](http://git-scm.com/),
[Maven](https://maven.apache.org/).
Further dependencies are resolved using Maven.

## Setup

  $ git clone https://github.com/KorAP/Krill
  $ cd Krill

To run the test suite, type in ...

  $ mvn test

To start the server, type in ...

  $ mvn compile exec:java

To compile and run the indexer, type ...

  $ mvn compile assembly:single

  $ java -jar target/KorAP-krill-X.XX.jar
    src/main/resources/korap.conf
    src/test/resources/examples/


## Caveats

Krill operates on tokens and is limited to a single tokenization stream.
Token annotations therefore have to rely on that tokenization,
Span annotations have to wrap at least one token.
Punctuations are currently not supported.
The order of results is currently bound to the order of documents in the
index, but this is likely to change.


## Development and License

**Authors**: [Nils Diewald](http://nils-diewald.de/), Eliza Margaretha

Copyright 2013-2015, IDS Mannheim, Germany

Krill is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Institute for German Language (IDS).

For recent changes and compatibility issues, please consult the
[Changes](https://raw.githubusercontent.com/KorAP/Krill/master/Changes)
file.

**Contributions to Krill are very welcome!**
Before contribution, please reformat your code according to the korap
style guideline, provided by means of an
[Eclipse style sheet](https://raw.githubusercontent.com/KorAP/Krill/master/korap-style.xml).
You can either reformat using [Eclipse](http://eclipse.org/) or using
[Maven](https://maven.apache.org/) with the command

  $ mvn java-formatter:format

Krill is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).

To cite this work, please ...


## References and bundled Software

Named entities annotated in the test data by CoreNLP were using
models based on:

  Manaal Faruqui and Sebastian Padó (2010):
  Training and Evaluating a German Named Entity
  Recognizer with Semantic Generalization,
  Proceedings of KONVENS 2010,
  Saarbrücken, Germany
