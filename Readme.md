# Krill

A Corpusdata Retrieval Index using Lucene for Look-Ups


## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora.

**The software is in its early stages and not stable yet - use on your own risk!**


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

To fetch the latest version of Krill ...

  $ git clone https://github.com/KorAP/Krill
  $ cd Krill

To run the test suite ...

  $ mvn clean test

To start the server ...

  $ mvn compile exec:java


## Caveats

Krill operates on tokens and is limited to a single tokenization stream.
Token annotations therefore have to rely on that tokenization,
span annotations have to wrap at least one token.
Punctuations are currently not supported.
The order of results is currently bound to the order of documents in the
index, but this is likely to change.


## Development and License

**Authors**: [Nils Diewald](http://nils-diewald.de/), Eliza Margaretha

Copyright (c) 2013-2015, IDS Mannheim, Germany

Krill is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Institute for German Language
([IDS](http://www1.ids-mannheim.de/)),
funded by the
[Leibniz-Gemeinschaft](http://www.leibniz-gemeinschaft.de/en/about-us/leibniz-competition/projekte-2011/2011-funding-line-2/)
and supported by the [KobRA](http://www.kobra.tu-dortmund.de) project,
funded by the Federal Ministry of Education and Research
([BMBF](http://www.bmbf.de/en/)).

For recent changes and compatibility issues, please consult the
[Changes](https://raw.githubusercontent.com/KorAP/Krill/master/Changes)
file.

Krill is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).
The Eclipse format style is based on the default style in Eclipse,
licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
Parts of the test corpus by courtesy of the
[DeReKo](http://ids-mannheim.de/kl/projekte/korpora/) project.

To cite this work, please refer to:<br>
Bański, Piotr, Joachim Bingel, Nils Diewald, Elena Frick, Michael Hanl, Marc Kupietz, Piotr Pęzik, Carsten Schnober, and Andreas Witt (2013):
*KorAP: the new corpus analysis platform at IDS Mannheim*. In: Z. Vetulani and H. Uszkoreit (eds):
*Human Language Technologies as a Challenge for Computer Science and Linguistics.*
Proceedings of the 6th Language and Technology Conference. Poznań: Fundacja Uniwersytetu im. A. Mickiewicza. 


## Contributions

Contributions to Krill are very welcome!
Before contributing, please reformat your code changes according to the KorAP
style guideline, provided by means of an
[Eclipse style sheet](https://raw.githubusercontent.com/KorAP/Krill/master/korap-style.xml).
You can either reformat using [Eclipse](http://eclipse.org/) or (recommended) using
[Maven](https://maven.apache.org/) with the command

  $ mvn java-formatter:format


## References and bundled Software

Named entities annotated in the test data by CoreNLP were using
models based on:

Manaal Faruqui and Sebastian Padó (2010):
*Training and Evaluating a German Named Entity Recognizer with Semantic Generalization*,
Proceedings of KONVENS 2010, Saarbrücken, Germany
